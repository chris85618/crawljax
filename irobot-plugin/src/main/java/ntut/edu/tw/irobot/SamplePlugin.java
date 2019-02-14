package ntut.edu.tw.irobot;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.ConditionTypeChecker;
import com.crawljax.condition.crawlcondition.CrawlCondition;
import com.crawljax.condition.eventablecondition.EventableConditionChecker;

import com.crawljax.core.*;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.state.Eventable;
import com.crawljax.forms.FormHandler;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.state.StateVertex;

import com.crawljax.core.plugin.HostInterface;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnCloneStatePlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.plugin.OnFireEventFailedPlugin;

public class SamplePlugin implements OnNewStatePlugin, OnCloneStatePlugin, PreStateCrawlingPlugin, OnFireEventFailedPlugin{
	private static final Logger LOGGER = LoggerFactory.getLogger(SamplePlugin.class);

    private HostInterface hostInterface;
    private WaitingLock waiting;
    private CandidateElementExtractor extractor;

	private CandidateElementExtractor newElementExtractor(EmbeddedBrowser browser, CrawljaxConfiguration config) {
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

	public SamplePlugin(HostInterface hostInterface, WaitingLock waiting) {
		this.hostInterface = hostInterface;
        this.waiting =  waiting;
	}

	/**
	 * This step will wait the robot to give the action from the candidates
	 *
	 * @param context
	 *            the current session data.
	 * @param candidateElements
	 *            the candidates for the current state.
	 * @param state
	 * 			  the state which ready to crawl
	 */
	@Override
	public void preStateCrawling(CrawlerContext context, ImmutableList<CandidateElement> candidateElements, StateVertex state) {
		try{
			waiting.setCandidateElements(candidateElements);
		}
		catch (Exception e) {
			LOGGER.info("Something happened when waiting for the robot respond.");
			LOGGER.debug(e.getMessage());
		}
	}

	@Override
	public void onNewState(CrawlerContext context, StateVertex newState) {
		try {
			String dom = context.getBrowser().getStrippedDom();

			System.out.println("------------------------------------------------");
			System.out.println(this.hostInterface.getOutputDirectory());
			System.out.println(context);
			System.out.println(context.getCurrentState());
			System.out.println(context.getCurrentState().getName());
			System.out.println("------------------------------------------------");

			File file = new File(this.hostInterface.getOutputDirectory(), context.getCurrentState().getName() + ".html");
			LOGGER.debug("In Sample Plugin~~");
			FileWriter fw = new FileWriter(file, false);
			fw.write(dom);
			fw.close();



			waiting.getRobotAction();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCloneState(CrawlerContext context, StateVertex newState) {
		extractor = newElementExtractor(context.getBrowser(), context.getConfig());
//		ImmutableList<CandidateElement> extract =
	}

	@Override
	public void onFireEventFailed(CrawlerContext context, Eventable eventable, List<Eventable> pathToFailure) {

	}


}
