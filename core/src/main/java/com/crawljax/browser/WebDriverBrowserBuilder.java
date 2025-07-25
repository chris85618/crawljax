package com.crawljax.browser;

import javax.inject.Inject;
import javax.inject.Provider;

import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.ProxyConfiguration;
import com.crawljax.core.configuration.ProxyConfiguration.ProxyType;
import com.crawljax.core.configuration.UnexpectedAlertHandler;
import com.crawljax.core.plugin.Plugins;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.ProxyConfig;
import com.machinepublishers.jbrowserdriver.Settings;

import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Default implementation of the EmbeddedBrowserBuilder based on Selenium WebDriver API.
 */
public class WebDriverBrowserBuilder implements Provider<EmbeddedBrowser> {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebDriverBrowserBuilder.class);
	private static final String HEADLESS_ARG = "--headless";
	private final CrawljaxConfiguration configuration;
	private final Plugins plugins;

	@Inject
	public WebDriverBrowserBuilder(CrawljaxConfiguration configuration, Plugins plugins) {
		this.configuration = configuration;
		this.plugins = plugins;
	}

	/**
	 * Build a new WebDriver based EmbeddedBrowser.
	 * 
	 * @return the new build WebDriver based embeddedBrowser
	 */
	@Override
	public EmbeddedBrowser get() {
		LOGGER.debug("Setting up a Browser");
		// Retrieve the config values used
		ImmutableSortedSet<String> filterAttributes =
		        configuration.getCrawlRules().getPreCrawlConfig().getFilterAttributeNames();
		long crawlWaitReload = configuration.getCrawlRules().getWaitAfterReloadUrl();
		long crawlWaitEvent = configuration.getCrawlRules().getWaitAfterEvent();

		// Determine the requested browser type
		EmbeddedBrowser browser = null;
		EmbeddedBrowser.BrowserType browserType = configuration.getBrowserConfig().getBrowsertype();
		try {
			switch (browserType) {
				case FIREFOX:
					browser =
					        newFireFoxBrowser(filterAttributes, crawlWaitReload, crawlWaitEvent);
					break;
				case INTERNET_EXPLORER:
					browser =
					        WebDriverBackedEmbeddedBrowser.withDriver(
					                new InternetExplorerDriver(),
					                filterAttributes, crawlWaitEvent, crawlWaitReload, plugins);
					break;
				case JBD:
					browser =
					        newJBrowserDriver(filterAttributes, crawlWaitReload, crawlWaitEvent);
					break;
				case CHROME:
					browser = newChromeBrowser(filterAttributes, crawlWaitReload, crawlWaitEvent);
					break;
				case REMOTE:
					browser =
					        WebDriverBackedEmbeddedBrowser.withRemoteDriver(configuration
					                .getBrowserConfig().getRemoteHubUrl(), filterAttributes,
					                crawlWaitEvent, crawlWaitReload, plugins);
					break;
				case PHANTOMJS:
					browser =
					        newPhantomJSDriver(filterAttributes, crawlWaitReload, crawlWaitEvent);
					break;
				default:
					throw new IllegalStateException("Unrecognized browsertype "
					        + configuration.getBrowserConfig().getBrowsertype());
			}
		} catch (IllegalStateException e) {
			LOGGER.error("Crawling with {} failed: {}", browserType.toString(), e.getMessage());
			throw e;
		}
		plugins.runOnBrowserCreatedPlugins(browser);
		return browser;
	}

	private EmbeddedBrowser newFireFoxBrowser(ImmutableSortedSet<String> filterAttributes,
	        long crawlWaitReload, long crawlWaitEvent) {
		FirefoxOptions options = new FirefoxOptions();

		// XXX Marionette does not yet handle the user prompts:
		// https://bugzilla.mozilla.org/show_bug.cgi?id=1264259
		// Once it's implemented it could be called:
		// options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT);
		// and removed the following custom UnexpectedAlertHandler.
		UnexpectedAlertHandler unexpectedAlertHandler = (b, alertText) -> {
			try {
				b.switchTo().alert().accept();
			} catch (NoAlertPresentException e) {
				// Since Firefox 59 the alerts are dismissed:
				// https://bugzilla.mozilla.org/show_bug.cgi?id=1416284
				// but keep the handler for older versions.
				LOGGER.debug("Firefox already handled the alert {}", alertText, e);
			}
			return true;
		};
		
		if (configuration.getProxyConfiguration() != null) {
			String lang = configuration.getBrowserConfig().getLangOrNull();
			if (!Strings.isNullOrEmpty(lang)) {
				options.addPreference("intl.accept_languages", lang);
			}

			options.addPreference("network.proxy.http", configuration.getProxyConfiguration()
				.getHostname());
			options.addPreference("network.proxy.http_port", configuration
				.getProxyConfiguration().getPort());
			options.addPreference("network.proxy.type", configuration.getProxyConfiguration()
				.getType().toInt());
			/* use proxy for everything, including localhost */
			options.addPreference("network.proxy.no_proxies_on", "");
		}
		options.addArguments("--whitelisted-ips=\"\"");
		options.addPreference("network.protocol-handler.warn-external-default", false);
//		options.addArguments("--disable-popup-blocking");

		if (configuration.getBrowserConfig().isHeadless()) {
			options.addArguments(HEADLESS_ARG);
		}

		FirefoxDriver firefoxDriver = new FirefoxDriver(options);
		firefoxDriver.manage().window().maximize();
		
		return WebDriverBackedEmbeddedBrowser.withDriver(firefoxDriver,
		        filterAttributes, crawlWaitEvent, crawlWaitReload, unexpectedAlertHandler, plugins);
	}

	private EmbeddedBrowser newChromeBrowser(ImmutableSortedSet<String> filterAttributes,
	        long crawlWaitReload, long crawlWaitEvent) {
		ChromeOptions optionsChrome = new ChromeOptions();
		optionsChrome.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT);
		if (configuration.getProxyConfiguration() != null
		        && configuration.getProxyConfiguration().getType() != ProxyType.NOTHING) {
			String lang = configuration.getBrowserConfig().getLangOrNull();
			if (!Strings.isNullOrEmpty(lang)) {
				optionsChrome.addArguments("--lang=" + lang);
			}
			optionsChrome.addArguments("--proxy-server=http://"
			        + configuration.getProxyConfiguration().getHostname() + ":"
			        + configuration.getProxyConfiguration().getPort());
		}

		if (configuration.getBrowserConfig().isHeadless()) {
			optionsChrome.addArguments(HEADLESS_ARG);
		}

		optionsChrome.addArguments("--whitelisted-ips=\"\"");
		optionsChrome.addArguments("--disable-popup-blocking");
        optionsChrome.addArguments("--disable-features=PasswordLeakDetection,PasswordCheck,AccountConsistency,SafeBrowsingEnhancedProtection");
		optionsChrome.addArguments("--disable-default-apps");
		optionsChrome.addArguments("--disable-gpu");
		optionsChrome.addArguments("--disable-dev-shm-usage");
		optionsChrome.addArguments("--window-size=1920,1080");
		/**
		 * Remove the external protocol handler
		 *
		 * https://stackoverflow.com/questions/29554564/bypass-external-protocol-request-popup-during-selenium-automation/52717415#52717415
		 */
		Map<String, Boolean> protocolBlockMap = new Hashtable<>();
		protocolBlockMap.put("webcal", false);
		protocolBlockMap.put("mailto", false);
		optionsChrome.setExperimentalOption("prefs", ImmutableMap.of("protocol_handler", ImmutableMap.of("excluded_schemes", protocolBlockMap)));

		ChromeDriver driverChrome = new ChromeDriver(optionsChrome);
		driverChrome.manage().window().maximize();

		return WebDriverBackedEmbeddedBrowser.withDriver(driverChrome, filterAttributes,
		        crawlWaitEvent, crawlWaitReload, plugins);
	}

	private EmbeddedBrowser newJBrowserDriver(ImmutableSortedSet<String> filterAttributes,
	        long crawlWaitReload, long crawlWaitEvent) {
		Settings.Builder settingsBuilder = Settings.builder();
		settingsBuilder.headless(configuration.getBrowserConfig().isHeadless());

		ProxyConfiguration proxyConf = configuration.getProxyConfiguration();
		if (proxyConf != null && proxyConf.getType() != ProxyType.NOTHING) {
			settingsBuilder.proxy(new ProxyConfig(ProxyConfig.Type.HTTP, proxyConf.getHostname(),
			        proxyConf.getPort()));
		}

		JBrowserDriver jbDriver = new JBrowserDriver(settingsBuilder.build());
		jbDriver.manage().window().maximize();
		
		return WebDriverBackedEmbeddedBrowser.withDriver(
		        jbDriver,
		        filterAttributes, crawlWaitEvent, crawlWaitReload, plugins);
	}

	private EmbeddedBrowser newPhantomJSDriver(ImmutableSortedSet<String> filterAttributes,
	        long crawlWaitReload, long crawlWaitEvent) {

		DesiredCapabilities caps = new DesiredCapabilities();
		caps.setCapability("takesScreenshot", true);
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--webdriver-loglevel=WARN"});
		final ProxyConfiguration proxyConf = configuration
				.getProxyConfiguration();
		if (proxyConf != null && proxyConf.getType() != ProxyType.NOTHING) {
			final String proxyAddrCap = "--proxy=" + proxyConf.getHostname()
					+ ":" + proxyConf.getPort();
			final String proxyTypeCap = "--proxy-type=http";
			final String[] args = new String[] { proxyAddrCap, proxyTypeCap };
			caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args);
		}
		
		PhantomJSDriver phantomJsDriver = new PhantomJSDriver(caps);
		phantomJsDriver.manage().window().maximize();
		return WebDriverBackedEmbeddedBrowser.withDriver(phantomJsDriver, filterAttributes,
		        crawlWaitEvent, crawlWaitReload, plugins);
	}

}
