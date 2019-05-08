package ntut.edu.tw.irobot;

import java.io.IOException;
import java.util.*;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.*;
import com.crawljax.core.plugin.*;
import com.crawljax.core.state.Eventable;
import com.crawljax.forms.FormInput;
import com.crawljax.forms.InputValue;
import com.crawljax.util.DomUtils;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.adapter.WebSnapShotMapper;
import ntut.edu.tw.irobot.lock.WaitingLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.state.StateVertex;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;


public class DQNLearningModePlugin implements PreStateCrawlingPlugin, OnFireEventFailedPlugin, AfterReceiveRobotActionPlugin, OnNewFoundStatePlugin, OnRestartCrawlingStatePlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(DQNLearningModePlugin.class);

	private CrawlingInformation crawlingInformation;
	private HostInterface hostInterface;
	private WaitingLock lock;
	private boolean isInitial;
	private boolean isRestart;
	private boolean isExecuteSuccess;
	private EmbeddedBrowser browser;

	public DQNLearningModePlugin(HostInterface hostInterface, WaitingLock waiting) {
		this.hostInterface = hostInterface;
        this.lock =  waiting;
        this.crawlingInformation = null;
        this.isInitial = true;
        this.isRestart = false;
        this.isExecuteSuccess = true;
	}

	/**
	 * This step will be called when the crawler restart
	 *
	 * @param context
	 *            the current session data.
	 * @param candidateElements
	 *            the candidates for the current state.
	 * @param state
	 */
	@Override
	public void onRestartCrawling(CrawlerContext context, ImmutableList<CandidateElement> candidateElements, StateVertex state) {
		if (isRestart || !isExecuteSuccess) {
			convertAndWaitRobot(candidateElements, state);
		}
	}

	/**
	 *	This method will be called from two pace:
	 *		1. prepare crawling index state
	 *		2. prepare crawling new state was found
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
		browser = context.getBrowser();
		convertAndWaitRobot(candidateElements, state);
	}

	private void convertAndWaitRobot(ImmutableList<CandidateElement> candidateElements, StateVertex state) {
		LOGGER.info("In DQN Plugin, convert data and wait the robot command...");
		try{
			// reset the data
			crawlingInformation = lock.getCrawlingInformation();
			crawlingInformation.resetData();

			WebSnapShot webSnapShot = new WebSnapShotMapper().mappingFrom(candidateElements, state);

			crawlingInformation.setWebSnapShot(webSnapShot);

			crawlingInformation.setExecuteSignal(isExecuteSuccess);

			try {
				lock.waitForRobotCommand();
			} finally {
				crawlingInformation = lock.getCrawlingInformation();
				isExecuteSuccess = true;
				isRestart = crawlingInformation.isRestart();
			}

			if (isRestart){
				LOGGER.info("Get the restart signal, restart crawling...");
				return;
			}

			LinkedList<CandidateElement> result = reConstructTheCandidateList(candidateElements);
			state.setElementsFound(result);
			LOGGER.info("Setting Target Action successfully...");
		}
		catch (Exception e) {
			LOGGER.warn("Something happened when waiting for the robot respond.");
			LOGGER.debug(e.getMessage());
		}
	}

	private LinkedList<CandidateElement> reConstructTheCandidateList(ImmutableList<CandidateElement> candidateElements) {
		List<CandidateElement> reconstructList = new ArrayList<CandidateElement>();

		for (CandidateElement element : candidateElements)
			reconstructList.add(element);

		CandidateElement target = crawlingInformation.getTargetElement();
		LOGGER.info("Get the target action is {}", target);
		for (CandidateElement element : reconstructList) {
			if (target == element) {
				reconstructList.remove(element);
				CandidateElement newElement = target;

				if (isInputTag())
					newElement = generateNewCandidateElement(element);

				reconstructList.add(0, newElement);
				break;
			}
		}

		return new LinkedList<CandidateElement>(reconstructList);
	}

	private CandidateElement generateNewCandidateElement(CandidateElement oldElement) {
		org.w3c.dom.Element cloneElement = (org.w3c.dom.Element) oldElement.getElement().cloneNode(true);
		String targetXpath = crawlingInformation.getTargetXpath();
		CandidateElement newElement = new CandidateElement(cloneElement, targetXpath, generateFormInput(oldElement));
		return newElement;
	}

	private List<FormInput> generateFormInput(CandidateElement oldElement) {
		List<FormInput> formInputs = new ArrayList<FormInput>();
		FormInput input = new FormInput();
		input.setType("text");
		input.setIdentification(oldElement.getIdentification());
		input.setInputValues(getValueList());
		formInputs.add(input);
		LOGGER.info("New Form is create : {}", input);
		return formInputs;
	}

	private Set<InputValue> getValueList() {
		Set<InputValue> transformList = new HashSet<InputValue>();
		transformList.add(new InputValue(crawlingInformation.getTargetValue(), true));
		return transformList;
	}

	/**
	 * This step will send a signal to robot when fire action failure.
	 *
	 * @param context
	 *            The per crawler context.
	 * @param eventable
	 *            the eventable that failed to execute
	 * @param pathToFailure
	 * 			  The Path of failure
	 */
	@Override
	public void onFireEventFailed(CrawlerContext context, Eventable eventable, List<Eventable> pathToFailure) {
		LOGGER.info("Setting Execute action success to false");
		isExecuteSuccess = false;
	}

	/**
	 * This step the Crawljax will get the robot reset signal.
	 *
	 * @return the boolean which the robot want to reset or not.
	 */
	@Override
	public boolean isRestartOrNot() {
		LOGGER.info("Get the restart signal : {}", isRestart);
		return isRestart;
	}

	/**
	 * This step will add value attribute in dom, like input tag
	 *
	 * @param dom
	 *          The dom capture from current browser
	 * @return
	 * 			the dom which has been modify
	 */
	@Override
	public String onNewFoundState(String dom) {
		// if no target element then return original dom
		if (isNoTarget())
			return dom;

		// if event is not input tag then return original dom
		if (!isInputTag())
			return dom;

		LOGGER.info("Adding input value...");

		try {
			Document doc = DomUtils.asDocument(dom);
			Node target = DomUtils.getElementByXpath(doc, crawlingInformation.getTargetXpath());
			LOGGER.info("Target action element in document is {}", target);

			if (isTargetNodeValueEqualToRobotGave())
				addValueAttributeToNode(target);
			else {
				LOGGER.info("Target Element value is not equal to robot gave value....");
			}
			return DomUtils.getDocumentToString(doc);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean isNoTarget() {
		LOGGER.debug("Target element which Robot gave is {}", crawlingInformation.getTargetElement());
		return crawlingInformation.getTargetElement() == null;
	}

	private boolean isInputTag() {
		String targetElementType = crawlingInformation.getTargetElementType();
		return targetElementType.equalsIgnoreCase("input");
	}

	private void addValueAttributeToNode(Node target) {
		LOGGER.info("Adding value to target Node...");
		((org.w3c.dom.Element) target).setAttribute("value", crawlingInformation.getTargetValue());
	}

	private boolean isTargetNodeValueEqualToRobotGave() {
		String targetValue = browser.getWebElement(crawlingInformation.getTargetElement().getIdentification()).getAttribute("value");
		LOGGER.info("Target Element value is {}", targetValue);
		return targetValue.equalsIgnoreCase(crawlingInformation.getTargetValue());
	}


}
