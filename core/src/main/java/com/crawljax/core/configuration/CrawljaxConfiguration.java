package com.crawljax.core.configuration;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.core.Crawler;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.configuration.CrawlRules.CrawlRulesBuilder;
import com.crawljax.core.plugin.Plugin;
import com.crawljax.core.state.StateVertexFactory;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Configures the {@link Crawler}. Set it up using the {@link #builderFor(String)} function.
 */
public class CrawljaxConfiguration {

	public static class CrawljaxConfigurationBuilder {

		private final ImmutableList.Builder<Plugin> pluginBuilder = ImmutableList.builder();
		private final CrawljaxConfiguration config;
		private final CrawlRulesBuilder crawlRules;

		private CrawljaxConfigurationBuilder(URI url) {
			Preconditions.checkNotNull(url);
			config = new CrawljaxConfiguration();
			config.url = url;
			crawlRules = CrawlRules.builder(this);
		}

		/**
		 * Sets the crawl scope.
		 * <p>
		 * If {@code null}, then a {@link DefaultCrawlScope} is used.
		 * 
		 * @param crawlScope
		 *            the crawl scope
		 * @return this {@code CrawljaxConfigurationBuilder} for method chaining.
		 * @since 3.7
		 */
		public CrawljaxConfigurationBuilder setCrawlScope(CrawlScope crawlScope) {
			config.crawlScope = crawlScope;
			return this;
		}

		/**
		 * If the website uses <a
		 * href="http://en.wikipedia.org/wiki/Basic_access_authentication">Basic auth</a> you can
		 * set the username and password here.
		 *
		 * @param username
		 *            The username for the website.
		 * @param password
		 *            The password for the website.
		 * @return {@link CrawljaxConfigurationBuilder} for method chaining.
		 */
		public CrawljaxConfigurationBuilder setBasicAuth(String username, String password) {
			try {
				String encodedUsername = URLEncoder.encode(username, "UTF-8");
				String encodedPassword = URLEncoder.encode(password, "UTF-8");
				String hostPrefix = encodedUsername + ":" + encodedPassword + "@";
				config.basicAuthUrl =
				        URI.create(config.url.toString().replaceFirst("://", "://" + hostPrefix));

			} catch (UnsupportedEncodingException e) {
				throw new CrawljaxException("Could not parse the username/password to a URL", e);
			}
			return this;
		}

		/**
		 * @param states
		 *            The maximum number of states the Crawler should crawl. The default is
		 *            unlimited.
		 */
		public CrawljaxConfigurationBuilder setMaximumStates(int states) {
			checkArgument(states > 1, "Number of maximum states should be larger than 1");
			config.maximumStates = states;
			return this;
		}

		/**
		 * Crawl without a maximum state limit.
		 */
		public CrawljaxConfigurationBuilder setUnlimitedStates() {
			config.maximumStates = 0;
			return this;
		}

		/**
		 * @param time
		 *            The maximum time the crawler should run. Default is one hour.
		 */
		public CrawljaxConfigurationBuilder setMaximumRunTime(long time, TimeUnit unit) {
			checkArgument(time >= 0, "Time should be larger than 0, or 0 for infinate.");
			config.maximumRuntime = unit.toMillis(time);
			return this;
		}

		/**
		 * Set the maximum runtime to unlimited.
		 */
		public CrawljaxConfigurationBuilder setUnlimitedRuntime() {
			config.maximumRuntime = 0;
			return this;
		}

		/**
		 * @param depth
		 *            The maximum depth the crawler can reach. The default is <code>2</code>.
		 */
		public CrawljaxConfigurationBuilder setMaximumDepth(int depth) {
			Preconditions.checkArgument(depth >= 0,
			        "Depth should be 0 for infinite, or larger for a certain depth.");
			config.maximumDepth = depth;
			return this;
		}

		/**
		 * Set the crawl depth to unlimited. The default depth is <code>2</code>.
		 */
		public CrawljaxConfigurationBuilder setUnlimitedCrawlDepth() {
			config.maximumDepth = 0;
			return this;
		}

		/**
		 * Add plugins to Crawljax. Note that without plugins, Crawljax won't give any ouput. For
		 * basic output at least enable the CrawlOverviewPlugin.
		 * <p>
		 * You can call this method several times to add multiple plugins
		 * </p>
		 *
		 * @param plugins
		 *            the plugins you would like to enable.
		 */
		public CrawljaxConfigurationBuilder addPlugin(Plugin... plugins) {
			pluginBuilder.add(plugins);
			return this;
		}

		/**
		 * @param configuration
		 *            The proxy configuration. Default is {@link ProxyConfiguration#noProxy()}
		 */
		public CrawljaxConfigurationBuilder setProxyConfig(ProxyConfiguration configuration) {
			Preconditions.checkNotNull(configuration);
			config.proxyConfiguration = configuration;
			return this;
		}

		/**
		 * @return The {@link CrawlRulesBuilder} to define crawling rules.
		 */
		public CrawlRulesBuilder crawlRules() {
			return crawlRules;
		}

		/**
		 * @param configuration
		 *            a custom {@link BrowserConfiguration}. The default is a single
		 *            {@link BrowserType#FIREFOX} browser.
		 */
		public CrawljaxConfigurationBuilder setBrowserConfig(BrowserConfiguration configuration) {
			Preconditions.checkNotNull(configuration);
			config.browserConfig = configuration;
			return this;
		}

		/**
		 * Set a custom {@link com.crawljax.core.state.StateVertexFactory} to be able to use your
		 * own {@link com.crawljax.core.state.StateVertex} objects. This is useful when you want to
		 * have a custom comparator in the stateflowgraph which relies on the
		 * {@link Object#hashCode()} or {@link Object#equals(Object)} of the
		 * {@link com.crawljax.core.state.StateVertex}.
		 *
		 * @param vertexFactory
		 *            The factory you want to use.
		 * @return The builder for method chaining.
		 */
		public CrawljaxConfigurationBuilder setStateVertexFactory(StateVertexFactory vertexFactory) {
			Preconditions.checkNotNull(vertexFactory);
			config.stateVertexFactory = vertexFactory;
			return this;
		}

		/**
		 * Set the output folder for any {@link Plugin} you might configure. Crawljax itself doesn't
		 * need an output folder but many plug-ins do.
		 *
		 * @param output
		 *            The output folder. If it does not exist it will be created.
		 * @throws IllegalStateException
		 *             if the specified file is not writable or exists but isn't a folder.
		 */
		public CrawljaxConfigurationBuilder setOutputDirectory(File output) {
			config.output = output;
			checkOutputDirWritable();
			return this;
		}

		/**
		 * Switch to the DQN learning mode.
		 * It will disable 
		 * 		1. Crawler.follow, no need to follow or back to last state
		 * 		2. Crawler.handleInputElement, no need to set the default value in the input
		 * 										just fillin the value which the robot gave.
		 * 
		 * @param isLearningMode
		 *            Only for DQN Leaning, other learning mode need to refactor the Crawljax core code.
		 * @return The builder for method chaining.
		 */
		public CrawljaxConfigurationBuilder setDQNLearningMode(boolean isLearningMode) {
			Preconditions.checkNotNull(isLearningMode);
			config.DQNLearningMode = isLearningMode;
			return this;
		} 

		private void checkOutputDirWritable() {
			if (!config.output.exists()) {
				Preconditions.checkState(config.output.mkdirs(),
				        "Could not create the output directory %s ", config.output);
			} else {
				Preconditions.checkArgument(config.output.isDirectory(),
				        "Output directory %s is not a folder", config.output);
				Preconditions.checkState(config.output.canWrite(),
				        "Output directory %s is not writable", config.output);
			}
		}

		public void setWrapUninteractiveElement(boolean wrapElement) {
			config.wrapElement = wrapElement;
		}

		public CrawljaxConfiguration build() {
			config.plugins = pluginBuilder.build();
			config.crawlRules = crawlRules.build();

			if (config.crawlScope == null) {
				config.crawlScope = new DefaultCrawlScope(config.getUrl());
			}

			return config;
		}

	}

	/**
	 * @param url
	 *            The url you want to setup a configuration for
	 * @return The builder to configure the crawler.
	 */
	public static CrawljaxConfigurationBuilder builderFor(URI url) {
		Preconditions.checkNotNull(url, "URL was null");
		return new CrawljaxConfigurationBuilder(url);
	}

	/**
	 * @param url
	 *            The url you want to setup a configuration for
	 * @return The builder to configure the crawler.
	 */
	public static CrawljaxConfigurationBuilder builderFor(String url) {
		return new CrawljaxConfigurationBuilder(URI.create(url));
	}

	private URI url;
	private URI basicAuthUrl;

	private CrawlScope crawlScope;

	private BrowserConfiguration browserConfig = new BrowserConfiguration(BrowserType.FIREFOX);
	private ImmutableList<Plugin> plugins;
	private ProxyConfiguration proxyConfiguration = ProxyConfiguration.noProxy();

	private CrawlRules crawlRules;

	private int maximumStates = 0;
	private long maximumRuntime = TimeUnit.HOURS.toMillis(1);;
	private int maximumDepth = 2;
	private File output = new File("out");
	private boolean DQNLearningMode = false;

	private boolean wrapElement = false;

	private StateVertexFactory stateVertexFactory;

	private CrawljaxConfiguration() {
	}

	public URI getUrl() {
		return url;
	}

	public URI getBasicAuthUrl() {
		return basicAuthUrl;
	}

	public CrawlScope getCrawlScope() {
		return crawlScope;
	}

	public BrowserConfiguration getBrowserConfig() {
		return browserConfig;
	}

	public ImmutableList<Plugin> getPlugins() {
		return plugins;
	}

	public ProxyConfiguration getProxyConfiguration() {
		return proxyConfiguration;
	}

	public CrawlRules getCrawlRules() {
		return crawlRules;
	}

	public int getMaximumStates() {
		return maximumStates;
	}

	public long getMaximumRuntime() {
		return maximumRuntime;
	}

	public int getMaximumDepth() {
		return maximumDepth;
	}

	public File getOutputDir() {
		return output;
	}

	public boolean getDQNLearningMode() {
		return DQNLearningMode;
	}

	public boolean getWrapElement() {
		return wrapElement;
	}

	public StateVertexFactory getStateVertexFactory() {
		return stateVertexFactory;
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, crawlScope, browserConfig, plugins, proxyConfiguration,
		        crawlRules, maximumStates, maximumRuntime, maximumDepth);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof CrawljaxConfiguration) {
			CrawljaxConfiguration that = (CrawljaxConfiguration) object;
			return Objects.equals(this.url, that.url)
			        && Objects.equals(this.browserConfig, that.browserConfig)
			        && Objects.equals(this.plugins, that.plugins)
			        && Objects.equals(this.proxyConfiguration, that.proxyConfiguration)
			        && Objects.equals(this.crawlRules, that.crawlRules)
			        && Objects.equals(this.crawlScope, that.crawlScope)
			        && Objects.equals(this.maximumStates, that.maximumStates)
			        && Objects.equals(this.maximumRuntime, that.maximumRuntime)
			        && Objects.equals(this.maximumDepth, that.maximumDepth);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("url", url)
		        .add("crawlScope", crawlScope).add("browserConfig", browserConfig)
		        .add("plugins", plugins).add("proxyConfiguration", proxyConfiguration)
		        .add("crawlRules", crawlRules).add("maximumStates", maximumStates)
		        .add("maximumRuntime", maximumRuntime).add("maximumDepth", maximumDepth)
		        .toString();
	}

}