package ntut.edu.tw.irobot;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.*;
import com.crawljax.core.plugin.*;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.forms.FormInput;
import com.crawljax.forms.InputValue;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XPathHelper;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.adapter.WebSnapShotMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import ntut.edu.tw.irobot.lock.WaitingLock;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.state.StateVertex;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;

public class DQNLearningModePlugin implements PreStateCrawlingPlugin,
		OnFireEventFailedPlugin, AfterReceiveRobotActionPlugin,
		OnNewFoundStatePlugin, OnRestartCrawlingStatePlugin, OnHtmlAttributeFilteringPlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(DQNLearningModePlugin.class);

	private CrawlingInformation crawlingInformation;
	private WaitingLock waitingLock;
	private boolean isRestart;
	private boolean isExecuteSuccess;
	private EmbeddedBrowser browser;
	private Map<String, Map<String, List<String>>> variableElementList = new HashMap<>();


	public DQNLearningModePlugin(WaitingLock waitingLock) {
        this.waitingLock =  waitingLock;
        this.crawlingInformation = null;
        this.isRestart = false;
        this.isExecuteSuccess = true;
		createVariableElementsList();
	}

	public  void printVariableElementList() {
		for(Map.Entry<String, Map<String, List<String>>> set : variableElementList.entrySet()) {
			System.out.println("Key : " + set.getKey());
			System.out.println("Value : " + set.getValue());
		}
	}

	private void createVariableElementsList() {
		JsonParser jsonParser = new JsonParser();
		try {
			File veList = new File("variableElement/variableElementList.json");
			JsonArray VEJson = ((JsonObject) jsonParser.parse(new FileReader(veList.getAbsoluteFile()))).getAsJsonArray("variableList");
			for(JsonElement jsonElement : VEJson) {
				String url = jsonElement.getAsJsonObject().get("url").getAsString();

				Map<String, List<String>> elementPair;
				if (variableElementList.get(url) != null)
					elementPair = variableElementList.get(url);
				else {
					elementPair = new HashMap<>();
				}

				String type = jsonElement.getAsJsonObject().get("attribute").getAsString();
				List<String> list;
				if (elementPair.get(type) != null)
					list = elementPair.get(type);
				else
					list = new ArrayList<>();

				String xpath = jsonElement.getAsJsonObject().get("element").getAsString();
				list.add(xpath);
				elementPair.put(type, list);
				variableElementList.put(url, elementPair);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			crawlingInformation = waitingLock.getCrawlingInformation();
			crawlingInformation.resetData();

			WebSnapShot webSnapShot = new WebSnapShotMapper().mappingFrom(candidateElements, state);

			crawlingInformation.setWebSnapShot(webSnapShot);

			crawlingInformation.setExecuteSignal(isExecuteSuccess);

			try {
				waitingLock.waitForRobotCommand();
			} finally {
				crawlingInformation = waitingLock.getCrawlingInformation();
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

		CandidateElement target = crawlingInformation.getTargetElement();
		if (target == null && crawlingInformation.getTargetElements().size() != 0)
			target = crawlingInformation.getTargetElements().entrySet().iterator().next().getKey();
		if (target == null)
			return new LinkedList<CandidateElement>(reconstructList);

		LOGGER.info("Get the target action is {}", target);
		CandidateElement newElement;
		for (CandidateElement element : candidateElements) {
			if (target == element) {
				if (isInputTag())
					newElement = generateNewCandidateElement(element);
				else
					newElement = target;

				reconstructList.add(newElement);
				break;
			}
		}

		return new LinkedList<CandidateElement>(reconstructList);
	}

	private CandidateElement generateNewCandidateElement(CandidateElement oldElement) {
		org.w3c.dom.Element cloneElement = (org.w3c.dom.Element) oldElement.getElement().cloneNode(true);
		String targetXpath = crawlingInformation.getTargetXpath();
		return new CandidateElement(cloneElement, targetXpath, generateFormInput(oldElement));
	}

	private List<FormInput> generateFormInput(CandidateElement oldElement) {
		List<FormInput> formInputs = new ArrayList<FormInput>();
		String inputValue = crawlingInformation.getTargetValue();
		Map<CandidateElement, String> elementValueMap = crawlingInformation.getTargetElements();
		if (inputValue == null && elementValueMap.size() == 0)
			return formInputs;

		if (elementValueMap.size() != 0)
			formInputs = generateMultipleFormInput();
		else
			formInputs.add(generateOneFormInput(inputValue, oldElement.getIdentification()));
		return formInputs;
	}

	private List<FormInput> generateMultipleFormInput() {
		List<FormInput> formInputs = new ArrayList<FormInput>();
		for (Map.Entry<CandidateElement, String> candidateElementStringEntry : crawlingInformation.getTargetElements().entrySet()) {
			formInputs.add(generateOneFormInput(candidateElementStringEntry.getValue(),
								 candidateElementStringEntry.getKey().getIdentification()));
		}
		return formInputs;
	}

	private FormInput generateOneFormInput(String inputValue, Identification elementIdentification) {
		FormInput formInputs = new FormInput();
		if (!inputValue.equalsIgnoreCase("null")) {
			formInputs.setType("text");
			formInputs.setIdentification(elementIdentification);
			formInputs.setInputValues(createValueList(inputValue));
			LOGGER.info("New Form is create : {}", formInputs);
		}
		return formInputs;
	}

	private Set<InputValue> createValueList(String value) {
		Set<InputValue> transformList = new HashSet<InputValue>();
		transformList.add(new InputValue(value, true));
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

		LOGGER.info("Adding input value...");

		try {
			Document doc = DomUtils.asDocument(dom);

			addValueAttributeToNode(doc);
			return DomUtils.getDocumentToString(doc);
		} catch (IOException e) {
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

	private void addValueAttributeToNode(Document doc) {
		LOGGER.info("Adding value to target Node...");
		NodeList inputNodes = doc.getElementsByTagName("INPUT");

		for(int i = 0; i < inputNodes.getLength(); i++) {
			// get the value from current page, not from stripped dom
			String xpathExpr = XPathHelper.getXPathExpression(inputNodes.item(i));
			Identification item = new Identification(Identification.How.xpath, xpathExpr);
			WebElement element = browser.getWebElement(item);
			String value = element.getAttribute("value");

			if (value.isEmpty())
				continue;

			((org.w3c.dom.Element) inputNodes.item(i)).setAttribute("value", value);
		}
	}

	/**
	 * Remove the variable element
	 *
	 * @param dom
	 * @param url
	 * @return the dom without variable element
	 */
	@Override
	public String filterDom(String dom, String url) {
		if (variableElementList.get(url) == null)
			return dom;

		return removeTheVariableElements(dom, variableElementList.get(url));
	}

	private String removeTheVariableElements(String dom, Map<String, List<String>> elementPair) {
		String strippedDOM = "";
		try {
			Document doc = DomUtils.asDocument(dom);
			for (Map.Entry<String, List<String>> pair : elementPair.entrySet())
				removeAttribute(doc, pair.getKey(), pair.getValue());

			strippedDOM = DomUtils.getDocumentToString(doc);
		} catch (IOException e) {
			LOGGER.warn("Something wrong when removed the attribute....");
			e.printStackTrace();
		}
		return strippedDOM;
	}

	private void removeAttribute(Document doc, String type, List<String> xpathList) {
		for (String xpath : xpathList) {
			try {
				Element element = DomUtils.getElementByXpath(doc, xpath);
				element.setAttribute(type, "");
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
	}
}
