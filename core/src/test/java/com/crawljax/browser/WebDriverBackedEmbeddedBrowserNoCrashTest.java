// Copyright 2010 Google Inc. All Rights Reserved.

package com.crawljax.browser;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.crawljax.core.CrawljaxException;
import com.crawljax.core.plugin.Plugins;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Eventable.EventType;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.Identification.How;
import com.crawljax.forms.FormInput;
import com.crawljax.test.BrowserTest;
import com.crawljax.test.RunWithWebServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;

/**
 * This Test checks the 'default' behavior of {@link EmbeddedBrowser} implemented by
 * {@link WebDriverBackedEmbeddedBrowser} on invalid input while the used browser is still active.
 */
@Category(BrowserTest.class)
public class WebDriverBackedEmbeddedBrowserNoCrashTest {

	@ClassRule
	public static final RunWithWebServer SERVER = new RunWithWebServer("site");

	@Rule
	public final BrowserProvider provider = new BrowserProvider();

	private EmbeddedBrowser browser;

	private Plugins pluigins;

	@BeforeClass
	public static void setupBeforeClass() {
		// XXX JBrowserDriver hangs(?) if no URL is accessed before closing the browser
		// which most of these tests do.
		assumeThat("hangs(?) if no URL is accessed before closing the browser",
		        BrowserProvider.getBrowserType(), is(not(EmbeddedBrowser.BrowserType.JBD)));
	}

	/**
	 * Make a new Browser for every test.
	 */
	@Before
	public void setUp() {
		browser = WebDriverBackedEmbeddedBrowser.withDriver(provider.newBrowser(), pluigins);
	}

	/**
	 * Test method for {@link com.crawljax.browser.EmbeddedBrowser#close()}.
	 */
	@Test
	public final void testClose() {
		browser.close();
	}

	/**
	 * Test method for {@link com.crawljax.browser.EmbeddedBrowser#closeOtherWindows()}.
	 */
	@Test
	public final void testCloseOtherWindows() {
		browser.closeOtherWindows();
	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#executeJavaScript(java.lang.String)}.
	 * 
	 * @throws CrawljaxException
	 *             when the script can not be executed
	 */
	@Test
	@Ignore
	public final void testExecuteJavaScript() throws CrawljaxException {
		try {
			browser.executeJavaScript("alert('testing');");

		} catch (CrawljaxException e) {
			fail("A WebDriverException needed to be thrown");
		}

	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#fireEventAndWait(com.crawljax.core.state.Eventable)}
	 * .
	 * 
	 * @throws CrawljaxException
	 *             when the event can not be fired.
	 */
	@Test
	public final void testFireEvent() throws Exception {
		browser.goToUrl(SERVER.getSiteUrl().resolve("simple.html"));
		browser.fireEventAndWait(new Eventable(new Identification(How.xpath, "//H1"),
		        EventType.click));
	}

	/**
	 * Test method for {@link com.crawljax.browser.EmbeddedBrowser#getCurrentUrl()}.
	 */
	@Test
	public final void testGetCurrentUrl() {
		browser.getCurrentUrl();
	}

	/**
	 * Test method for {@link com.crawljax.browser.EmbeddedBrowser#getStrippedDom()}.
	 * 
	 * @throws CrawljaxException
	 *             when the dom can not be downloaded.
	 */
	@Test
	public final void testGetDom() throws CrawljaxException, URISyntaxException {
		// XXX Firefox issue: https://bugzilla.mozilla.org/show_bug.cgi?id=1332122
		assumeThat("file:// leads to \"hangs\"", BrowserProvider.getBrowserType(),
		        is(not(EmbeddedBrowser.BrowserType.FIREFOX)));

		URL index = WebDriverBackedEmbeddedBrowserTest.class.getResource("/site/simple.html");
		browser.goToUrl(index.toURI());
		browser.getStrippedDom();
	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#getStrippedDomWithoutIframeContent()}.
	 * 
	 * @throws CrawljaxException
	 *             when the the dom can not be downloaded.
	 */
	@Test
	public final void testGetDomWithoutIframeContent() throws CrawljaxException {
		browser.getStrippedDomWithoutIframeContent();
	}

	/**
	 * Test method for {@link com.crawljax.browser.EmbeddedBrowser#goBack()}.
	 */
	@Test
	public final void testGoBack() {
		browser.goBack();
	}

	/**
	 * Test method for {@link com.crawljax.browser.EmbeddedBrowser#goToUrl(java.net.URI)}.
	 */
	@Test
	public final void testGoToUrl() throws CrawljaxException, MalformedURLException {
		// TODO Stefan; bug in WebDriver iff you specify bla:// will end up in NullPointer.
		browser.goToUrl(URI.create("http://non.exsisting.domain"));
	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#input(com.crawljax.core.state.Identification, java.lang.String)}
	 * .
	 * 
	 * @throws CrawljaxException
	 *             when the input can not be found
	 */
	@Test
	public final void testInput() throws CrawljaxException {
		assertFalse("Wrong Xpath so false because of error",
		        browser.input(new Identification(How.xpath, "/RUBISH"), "some"));
	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#isVisible(com.crawljax.core.state.Identification)}
	 * .
	 */
	@Test
	public final void testIsVisible() {
		assertFalse("Wrong Xpath so not visible",
		        browser.isVisible(new Identification(How.xpath, "/RUBISH")));
	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#getInputWithRandomValue(com.crawljax.forms.FormInput)}
	 * .
	 */
	@Test
	public final void testGetInputWithRandomValue() {
		assertNull("Wrong Xpath so null as result of InputWithRandomValue",
		        browser.getInputWithRandomValue(new FormInput("text", new Identification(
		                How.xpath, "/RUBISH"), "abc")));
	}

	/**
	 * Test method for {@link com.crawljax.browser.EmbeddedBrowser#getFrameDom(java.lang.String)}.
	 */
	@Test
	public final void testGetFrameDom() {
		assertTrue("Wrong FrameID so empty", browser.getFrameDom("123").equals(""));
	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#elementExists(com.crawljax.core.state.Identification)}
	 * .
	 */
	@Test
	public final void testElementExists() {
		assertFalse("Wrong Xpath so element does not exsist",
		        browser.elementExists(new Identification(How.xpath, "/RUBISH")));
	}

	/**
	 * Test method for
	 * {@link com.crawljax.browser.EmbeddedBrowser#getWebElement(com.crawljax.core.state.Identification)}
	 * .
	 */
	@Test
	public final void testGetWebElement() {
		try {
			browser.getWebElement(new Identification(How.xpath, "/RUBISH"));
		} catch (NoSuchElementException | TimeoutException e) {
			// Expected behavior
			return;
		}
		Assert.fail("NoSuchElementException/TimeoutException should have been thrown");
	}

}
