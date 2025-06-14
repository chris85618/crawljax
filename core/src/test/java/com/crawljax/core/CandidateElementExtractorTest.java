package com.crawljax.core;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.crawljax.browser.BrowserProvider;
import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.ConditionTypeChecker;
import com.crawljax.condition.crawlcondition.CrawlCondition;
import com.crawljax.condition.eventablecondition.EventableConditionChecker;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.core.plugin.Plugins;
import com.crawljax.core.state.DefaultStateVertexFactory;
import com.crawljax.core.state.StateVertex;
import com.crawljax.forms.FormHandler;
import com.crawljax.test.BrowserTest;
import com.crawljax.test.RunWithWebServer;
import com.google.common.io.Resources;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(BrowserTest.class)
@RunWith(MockitoJUnitRunner.class)
public class CandidateElementExtractorTest {

	private static final Logger LOG = LoggerFactory
	        .getLogger(CandidateElementExtractorTest.class);
	private static final StateVertex DUMMY_STATE = new DefaultStateVertexFactory().createIndex("http://localhost",
	        "", "");

	private static final String defaultFile= "/candidateElementExtractorTest/domWithOneExternalAndTwoInternal.html";

	@Mock
	private Plugins plugins;

	@ClassRule
	public static final RunWithWebServer DEMO_SITE_SERVER = new RunWithWebServer("/demo-site");

	@Rule
	public final BrowserProvider provider = new BrowserProvider();

	private EmbeddedBrowser browser;


	@Test
	public void testExtract() throws InterruptedException, CrawljaxException {
		CrawljaxConfigurationBuilder builder =
		        CrawljaxConfiguration.builderFor(DEMO_SITE_SERVER.getSiteUrl());
		builder.crawlRules().click("a");
		builder.crawlRules().clickOnce(true);
		CrawljaxConfiguration config = builder.build();

		CandidateElementExtractor extractor = newElementExtractor(config);
		browser.goToUrl(DEMO_SITE_SERVER.getSiteUrl());
		List<CandidateElement> candidates = extractor.extract(DUMMY_STATE);

		assertNotNull(candidates);
		assertEquals(15, candidates.size());

	}

	private CandidateElementExtractor newElementExtractor(CrawljaxConfiguration config) {
		browser = provider.newEmbeddedBrowser();
		FormHandler formHandler = new FormHandler(browser, config.getCrawlRules());

		EventableConditionChecker eventableConditionChecker =
		        new EventableConditionChecker(config.getCrawlRules());
		ConditionTypeChecker<CrawlCondition> crawlConditionChecker =
		        new ConditionTypeChecker<>(config.getCrawlRules().getPreCrawlConfig()
		                .getCrawlConditions());
		ExtractorManager checker =
		        new CandidateElementManager(eventableConditionChecker, crawlConditionChecker);
		CandidateElementExtractor extractor =
		        new CandidateElementExtractor(checker, browser, formHandler, config);

		return extractor;
	}

	@Test
	public void testExtractExclude() throws Exception {
		CrawljaxConfigurationBuilder builder =
		        CrawljaxConfiguration.builderFor(DEMO_SITE_SERVER.getSiteUrl());
		builder.crawlRules().click("a");
		builder.crawlRules().dontClick("div").withAttribute("id", "menubar");
		builder.crawlRules().clickOnce(true);
		CrawljaxConfiguration config = builder.build();

		CandidateElementExtractor extractor = newElementExtractor(config);
		browser.goToUrl(DEMO_SITE_SERVER.getSiteUrl());

		List<CandidateElement> candidates = extractor.extract(DUMMY_STATE);

		assertNotNull(candidates);
		assertThat(candidates, hasSize(11));

	}

	@Test
	public void testExtractIframeContents() throws Exception {
		// XXX JBrowserDriver issue: https://github.com/MachinePublishers/jBrowserDriver/issues/235
		assumeThat("iframe tests lead to hangs/loops", BrowserProvider.getBrowserType(),
		        is(not(EmbeddedBrowser.BrowserType.JBD)));

		RunWithWebServer server = new RunWithWebServer("/site");
		server.before();
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration
		        .builderFor(server.getSiteUrl().resolve("iframe/"));
		builder.crawlRules().click("a");
		CrawljaxConfiguration config = builder.build();

		CandidateElementExtractor extractor = newElementExtractor(config);
		browser.goToUrl(server.getSiteUrl().resolve("iframe/"));
		List<CandidateElement> candidates = extractor.extract(DUMMY_STATE);

		for (CandidateElement e : candidates) {
			LOG.debug("candidate: " + e.getUniqueString());
		}

		server.after();

		assertNotNull(extractor);
		assertNotNull(candidates);
		assertThat(candidates, hasSize(9));

	}

	@Test
	public void whenNoFollowExternalUrlDoNotFollow() throws IOException, URISyntaxException {
		// XXX Firefox issue: https://bugzilla.mozilla.org/show_bug.cgi?id=1332122
		assumeThat("file:// leads to \"hangs\"", BrowserProvider.getBrowserType(),
		        is(not(EmbeddedBrowser.BrowserType.FIREFOX)));

		CrawljaxConfigurationBuilder builder =
		        CrawljaxConfiguration.builderFor("http://example.com");
		builder.crawlRules().click("a");
		CrawljaxConfiguration config = builder.build();
		CandidateElementExtractor extractor = newElementExtractor(config);

		List<CandidateElement> extract = extractFromTestFile(extractor, defaultFile);

		assertThat(config.getCrawlRules().followExternalLinks(), is(false));
		assertThat(extract, hasSize(2));
	}

	@Test
	public void whenFollowExternalUrlDoFollow() throws IOException, URISyntaxException {
		// XXX Firefox issue: https://bugzilla.mozilla.org/show_bug.cgi?id=1332122
		assumeThat("file:// leads to \"hangs\"", BrowserProvider.getBrowserType(),
		        is(not(EmbeddedBrowser.BrowserType.FIREFOX)));

		CrawljaxConfigurationBuilder builder =
		        CrawljaxConfiguration.builderFor("http://example.com");
		builder.crawlRules().click("a");
		builder.crawlRules().followExternalLinks(true);
		CrawljaxConfiguration config = builder.build();
		CandidateElementExtractor extractor = newElementExtractor(config);

		List<CandidateElement> extract = extractFromTestFile(extractor, defaultFile);

		assertThat(config.getCrawlRules().followExternalLinks(), is(true));
		assertThat(extract, hasSize(3));
	}

	@Test
	public void testExtractShouldIgnoreDownloadFiles() throws Exception {
		// XXX crawler issue: https://github.com/zaproxy/crawljax/pull/76
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor("http://example.com");
		builder.crawlRules().click("a");
		CrawljaxConfiguration config = builder.build();

		CandidateElementExtractor extractor = newElementExtractor(config);

		String file = "/candidateElementExtractorTest/domWithFourTypeDownloadLink.html";
		List<CandidateElement> candidates = extractFromTestFile(extractor, file);

		for (CandidateElement e : candidates) {
			LOG.debug("candidate: " + e.getUniqueString());
		}

		assertNotNull(candidates);
		assertEquals(12, candidates.size());
	}

	private List<CandidateElement> extractFromTestFile(CandidateElementExtractor extractor, String file) throws URISyntaxException {
		StateVertex currentState = Mockito.mock(StateVertex.class);
		URL dom = Resources.getResource(getClass(), file);
		browser.goToUrl(dom.toURI());
		List<CandidateElement> extract = extractor.extract(currentState);
		return extract;
	}

}