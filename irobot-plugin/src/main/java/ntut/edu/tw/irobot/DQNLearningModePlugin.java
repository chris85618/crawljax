package ntut.edu.tw.irobot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.crawljax.core.*;
import com.crawljax.core.state.Eventable;
import com.crawljax.util.DomUtils;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.lock.WaitingLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.state.StateVertex;
import com.crawljax.core.plugin.HostInterface;
import ntut.edu.tw.irobot.interaction.CrawlerInteractor;

import com.crawljax.core.plugin.OnFireEventFailedPlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.plugin.AfterReceiveRobotActionPlugin;
import com.crawljax.core.plugin.OnNewFoundStatePlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;


public class DQNLearningModePlugin implements PreStateCrawlingPlugin, OnFireEventFailedPlugin, AfterReceiveRobotActionPlugin, OnNewFoundStatePlugin{
	private static final Logger LOGGER = LoggerFactory.getLogger(DQNLearningModePlugin.class);

    private HostInterface hostInterface;
    private WaitingLock locker;
	private CrawlerInteractor crawlingData;

	public DQNLearningModePlugin(HostInterface hostInterface, WaitingLock waiting) {
		this.hostInterface = hostInterface;
        this.locker =  waiting;
        this.crawlingData = null;
	}

	/**
	 *	This method will be called from three pace:
	 *		1. prepare crawling index state
	 *		2.
	 *
	 * This step will wait the robot command and
	 * 		put the target element to the top of the candidateElements
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
			crawlingData = locker.getSource();
			crawlingData.convertToRobotAction(candidateElements);
			crawlingData.convertToRobotState(state);
			// will wait the robot command
			locker.waitForRobotCommand();

			crawlingData = locker.getSource();
			LinkedList<CandidateElement> result = reConstructTheCandidateList(candidateElements);
			state.setElementsFound(result);
			LOGGER.info("Setting Target Action successfully...");
		}
		catch (Exception e) {
			LOGGER.info("Something happened when waiting for the robot respond.");
			LOGGER.debug(e.getMessage());
		}

	}

	private LinkedList<CandidateElement> reConstructTheCandidateList(ImmutableList<CandidateElement> candidateElements) {
		List<CandidateElement> reconstructList = new ArrayList<CandidateElement>();

		for (CandidateElement element : candidateElements)
			reconstructList.add(element);

		CandidateElement target = crawlingData.getTargetElment();
		LOGGER.info("Get the target action is {}", target);
		for (CandidateElement element : reconstructList) {
			if (target == element) {
				reconstructList.remove(element);
				reconstructList.set(0, target);
				break;
			}
		}

		return new LinkedList<CandidateElement>(reconstructList);
	}

	/**
	 * This step will send a signal to robot when fire action failure.
	 *
	 * @param context
	 *            The per crawler context.
	 * @param eventable
	 *            the eventable that failed to execute
	 * @param pathToFailure
	 */
	@Override
	public void onFireEventFailed(CrawlerContext context, Eventable eventable, List<Eventable> pathToFailure) {
		// feature work
		crawlingData.setExecuteActionFailureSignal(true);
	}

	/**
	 * This step the Crawljax will get the robot reset signal.
	 *
	 * @return the boolean which the robot want to reset or not.
	 */
	@Override
	public boolean isRestartOrNot() {
		boolean isRestart = crawlingData.isRestart();
		if (isRestart)
			crawlingData.setToDefaultSignal();
		return isRestart;
	}

	/**
	 * This step will add value attribute in dom, like <input> tag
	 *
	 * @param dom
	 *          The dom capture from current browser
	 * @return
	 * 			the dom which has been modify
	 */
	@Override
	public String onNewFoundState(String dom) {
		// if event is not input tag, then return original dom
		if (!isInputTag())
			return dom;

		LOGGER.info("Adding input value ");

		try {
			Document doc = DomUtils.asDocument(dom);
			Node target = DomUtils.getElementByXpath(doc, crawlingData.getTargetXpath());
			LOGGER.info("Found a new state, target action element : {} ", target.getNodeName());
			if (isTargetNodeValueEqualToRobotGave(target) && crawlingData.getTargetValue() != "")
				addValueAttributeToNode(target);

			return DomUtils.getDocumentToString(doc);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean isInputTag() {
		String targetElementType = crawlingData.getTargetElementType();
		return targetElementType.equalsIgnoreCase("input");
	}

	private void addValueAttributeToNode(Node target) {
		LOGGER.info("Adding value to target Node...");
		((org.w3c.dom.Element) target).setAttribute("value", crawlingData.getTargetValue());
	}

	private boolean isTargetNodeValueEqualToRobotGave(Node target) {
		return target.getNodeValue() == crawlingData.getTargetValue();
	}
}
