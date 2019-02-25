package ntut.edu.tw.irobot;

import java.io.IOException;
import java.util.*;

import com.crawljax.core.*;
import com.crawljax.core.state.Eventable;
import com.crawljax.forms.FormInput;
import com.crawljax.forms.InputValue;
import com.crawljax.util.DomUtils;
import com.google.common.collect.ImmutableList;
import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
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
    private WaitingLock lock;
	private CrawlerInteractor crawlingData;
	private Mutex loopMutex;
	public DQNLearningModePlugin(HostInterface hostInterface, WaitingLock waiting) {
		this.hostInterface = hostInterface;
        this.lock =  waiting;
        this.crawlingData = null;
        this.loopMutex = new Mutex();
	}

	/**
	 *	This method will be called from three pace:
	 *		1. prepare crawling index state
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
			crawlingData = lock.getSource();
			crawlingData.convertToRobotAction(candidateElements);
			crawlingData.convertToRobotState(state);

			// will wait the robot command
			loopMutex.acquire();
			try {
				lock.initReady();
				if (crawlingData.isRestart()) {
					crawlingData.setToDefaultSignal();
					lock.initCrawler();
				}
				else
					lock.waitForRobotCommand();
			} finally {
				loopMutex.release();
			}

			crawlingData = lock.getSource();
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
		CandidateElement newElement;

		for (CandidateElement element : candidateElements)
			reconstructList.add(element);

		CandidateElement target = crawlingData.getTargetElment();
		LOGGER.info("Get the target action is {}", target);
		for (CandidateElement element : reconstructList) {
			if (target == element) {
				reconstructList.remove(element);
				newElement = generateNewCandidateElement(element);
				reconstructList.set(0, newElement);
				break;
			}
		}

		return new LinkedList<CandidateElement>(reconstructList);
	}

	private CandidateElement generateNewCandidateElement(CandidateElement oldElement) {
		org.w3c.dom.Element cloneElement = (org.w3c.dom.Element) oldElement.getElement().cloneNode(true);
		String targetXpath = crawlingData.getTargetXpath();
		CandidateElement newElement = new CandidateElement( cloneElement, targetXpath, generateFormInput(oldElement));
		return newElement;
	}

	private List<FormInput> generateFormInput(CandidateElement oldElement) {
		List<FormInput> formInputs = new ArrayList<FormInput>();
		FormInput input = new FormInput();
		input.setType("text");
		input.setIdentification(oldElement.getIdentification());
		input.setInputValues(getValueList());
		formInputs.add(input);
		return formInputs;
	}

	private Set<InputValue> getValueList() {
		Set<InputValue> transformList = new HashSet<InputValue>();
		transformList.add(new InputValue(crawlingData.getTargetValue(), true));
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
