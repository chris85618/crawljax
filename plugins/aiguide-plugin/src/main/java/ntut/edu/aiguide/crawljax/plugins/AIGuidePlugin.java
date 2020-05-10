package ntut.edu.aiguide.crawljax.plugins;


import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier;
import com.crawljax.core.plugin.*;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateVertex;
import com.crawljax.forms.FormInput;
import com.crawljax.forms.InputValue;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XPathHelper;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.util.Pair;
import ntut.edu.aiguide.crawljax.plugins.domain.Action;
import ntut.edu.aiguide.crawljax.plugins.domain.LearningTarget;
import ntut.edu.aiguide.crawljax.plugins.domain.ProcessingDirectiveManagement;
import ntut.edu.aiguide.crawljax.plugins.domain.State;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AIGuidePlugin implements OnBrowserCreatedPlugin, OnNewFoundStatePlugin, OnCountingDepthPlugin,
                                        PreStateCrawlingPlugin, PostCrawlingPlugin, OnHtmlAttributeFilteringPlugin,
                                        OnUrlLoadPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIGuidePlugin.class);
    private final ServerInstanceManagement serverInstanceManagement;
    private final int serverPort;
    private int resetCounter = 0;
    private EmbeddedBrowser browser = null;
    private List<StateVertex> inputStates = new LinkedList<>();
    private final ProcessingDirectiveManagement processingDirectiveManagement ;
    private Map<String, Map<String, List<String>>> variableElementList = new HashMap<>();


    /**
     * directive stack perform like following example:
     *
     *  | root directive |
     *  |    directive   |
     *  |    directive   |
     *  | leaf directive |
     *  ------------------
     */
    private final Stack<State> directivesStack;

    private Queue<Pair<State, StateVertex>> directiveStateVertexComparisonTable = new LinkedList<>();
    private State targetState;
    private StateFlowGraph stateFlowGraph;
    private boolean isDirectiveProcess = false;
    private StateVertex lastDirectiveStateVertex = null;

    public AIGuidePlugin(Stack<State> directivePath, ServerInstanceManagement serverInstanceManagement, int serverPort) {
        this.directivesStack = directivePath;
        processingDirectiveManagement = new ProcessingDirectiveManagement((Stack<State>) directivePath.clone());
        this.targetState = getTopOfDirective();
        this.serverInstanceManagement = serverInstanceManagement;
        this.serverPort = serverPort;
        createVariableElementsList();
    }

    private State getTopOfDirective() {
        if (directivesStack.empty())
            return null;
        return directivesStack.pop();
    }

    private void createVariableElementsList() {
        JsonParser jsonParser = new JsonParser();
        try {
            File veList = new File("variableElement/variableElementList.json");
            System.out.println(veList.getAbsolutePath());
            JsonArray VEJson = ((JsonObject) jsonParser.parse(new FileReader(veList.getAbsoluteFile()))).getAsJsonArray("variableList");
            for(JsonElement jsonElement : VEJson) {
                String url = jsonElement.getAsJsonObject().get("url").getAsString();
                url = String.format(url, serverPort);
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

    @Override
    public void onBrowserCreated(EmbeddedBrowser browser) {
        this.browser = browser;
    }

    // remove all variable list on dom
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
                element.removeAttribute(type);
            } catch (XPathExpressionException | NullPointerException e) {
                LOGGER.info("Can not find element with xpath: {}, keeping wrap other variable element...", xpath);
            }
        }
    }

    // add element value to dom
    @Override
    public String onNewFoundState(String dom) {
        try {
            Document doc = DomUtils.asDocument(dom);
            String convertDom = DomUtils.getDocumentToString(doc);
            if (isDirectiveProcess) {
                addValueAttributeToNode(doc);
                String appendStateName = processingDirectiveManagement.getAppendStateName();
                if (appendStateName == null) {
                    LOGGER.debug("Get the appendStateName is null...");
                    isDirectiveProcess = false;
                    return DomUtils.getDocumentToString(doc);
                }
                LOGGER.debug("Get the appendStateName is {}", appendStateName);
                NodeList elements = doc.getElementsByTagName("body");
                ((org.w3c.dom.Element) elements.item(0)).setAttribute("name", appendStateName);
                return DomUtils.getDocumentToString(doc);
            }


//            if (targetState == null) {
//                return DomUtils.getDocumentToString(doc);
//            }
//            System.out.println("Target state ID : " + targetState.getID());
//            System.out.println("Current state ID : " + dom.hashCode());
//            System.out.println("Current state ID (After DomUtils): " + DomUtils.getDocumentToString(doc).hashCode());

            if (processingDirectiveManagement.isCurrentStateIsDirective(convertDom))
                isDirectiveProcess = true;

            return DomUtils.getDocumentToString(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dom;
    }


    /**
     * Add value, assigned by Directive, to Dom
     * @param doc
     *      which convert dom to doc
     */
    private void addValueAttributeToNode(Document doc) {
        NodeList inputNodes = doc.getElementsByTagName("INPUT");
        List<Action> actions =  targetState.getLastActionSet();
//        System.out.print("Target state actions :");
//        System.out.println(Arrays.toString(new List[]{actions}));
//        System.out.println("Current URL is : " + browser.getCurrentUrl());
        for(int i = 0; i < inputNodes.getLength(); i++) {
            // get the value from current page, not from stripped dom
            String xpathExpr = XPathHelper.getXPathExpression(inputNodes.item(i));
            if (!isElementInActionSet(xpathExpr, actions))
                continue;
            Identification item = new Identification(Identification.How.xpath, xpathExpr);
            WebElement element = browser.getWebElement(item);
            String value = element.getAttribute("value");

            if (value.isEmpty())
                continue;

            ((org.w3c.dom.Element) inputNodes.item(i)).setAttribute("value", value);
        }
    }

    // if the directive is not finish, control the crawling depth
    @Override
    public void controlDepth(StateVertex currentState, AtomicInteger crawlDepth) {
        System.out.println("In Control depth ");
        try {
            if (!isDirectiveProcess)
                crawlDepth.incrementAndGet();
            LOGGER.info("In AI Plugin, after control Depth is {}", crawlDepth.get());
        } catch (Exception e) {
            throw new RuntimeException("Something Wrong when counting the depth");
        }
    }

    @Override
    public void preStateCrawling(CrawlerContext context, ImmutableList<CandidateElement> candidateElements, StateVertex currentState) {
        System.out.println("In preStateCrawling");
        if (isDirectiveProcess) {
            LOGGER.info("Current state {} is same as directive or is Processing State", currentState);
            isDirectiveProcess = true;
            processingDirectiveManagement.recordCurrentState(targetState, currentState);
            //            processingStates.add(currentState);
            lastDirectiveStateVertex = currentState;
            changeCandidateElementForCurrentState(candidateElements, currentState);
        }
        else if (isCurrentStateIsInputPage(candidateElements)) {
            System.out.println("Current state is Input state");
            LOGGER.info("Current page is input page, not going to crawled");
            currentState.setElementsFound(new LinkedList<>());
            inputStates.add(currentState);
            for (StateVertex s : inputStates) {
                System.out.println(s.getId());
            }
        }
        else {
            currentState.setElementsFound(new LinkedList<>(candidateElements));
            LOGGER.info("Current page is not input page, moving on...");
        }
    }

    private boolean isCurrentStateIsInputPage(ImmutableList<CandidateElement> candidateElements) {
        List<CandidateElement> isInteractableInputElements = candidateElements.parallelStream()
                .filter(candidateElement -> candidateElement.getElement().getTagName().equalsIgnoreCase("input"))
                .filter(candidateElement -> browser.isInteractive(candidateElement.getIdentification().getValue()))
                .collect(Collectors.toList());
        return !isInteractableInputElements.isEmpty();
    }

    private boolean isCurrentStateSameAsTargetState(StateVertex state) {
        if (targetState == null)
            return false;
        if (String.valueOf(state.getDom().hashCode()).equalsIgnoreCase(targetState.getID())) {
            directiveStateVertexComparisonTable.add(new Pair<>(targetState, state));
            return true;
        }
        return false;
    }

    private void changeCandidateElementForCurrentState(ImmutableList<CandidateElement> candidateElements, StateVertex currentState) {
        List<Action> actionSet = targetState.getNextActionSet();
        CandidateElement newElement = null;
        if (actionSet == null) {
            isDirectiveProcess = false;
//            processingStates.remove(currentState);
            processingDirectiveManagement.removeLastStateInRecordList(targetState);
            currentState.setElementsFound(new LinkedList<> (candidateElements));
            return;
        }

        if (!targetState.hasNextActionSet()) {
            isDirectiveProcess = false;
            processingDirectiveManagement.removeLastStateInRecordList(targetState);
            targetState = getTopOfDirective();
        }

        for (CandidateElement element : candidateElements) {
            if (isElementInActionSet(element.getIdentification().getValue(), actionSet)) {
                newElement = createNewCandidateElementWithFormInput(element, actionSet);
                break;
            }
        }

        try {
            if (newElement == null) {
                Identification identification = new Identification(Identification.How.xpath, actionSet.get(0).getActionXpath());
                Element element = findElementInCurrentState(identification, currentState);
                if (element != null) {
                    newElement = new CandidateElement(element, identification, "", new ArrayList<>(), actionSet.get(0).getValue());
                }
                else {
                    element = findCorrespondElement(identification, currentState.getDocument(), "input");
                    List<FormInput> formInputs = new ArrayList<FormInput>();
                    for (Action action : actionSet)
                        formInputs.add(createOneFormInput(action.getValue(), action.getActionXpath()));
                    newElement = new CandidateElement(element, identification, "",formInputs, actionSet.get(0).getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        currentState.setElementsFound(new LinkedList<>(Collections.singletonList(newElement)));
    }

    private Element findElementInCurrentState(Identification identification, StateVertex currentState) {
        Element findElement = null;
        try {
            findElement = findCorrespondElement(identification, currentState.getDocument(), "a");
            if (findElement == null)
                findElement = findCorrespondElement(identification, currentState.getDocument(), "button");
        } catch (IOException e) {
            LOGGER.info("Something wrong when finding target action element");
        }
        return findElement;
    }

    private Element findCorrespondElement(Identification identification, Document document, String tag) {
        NodeList elements =  document.getElementsByTagName(tag);
        for (int i = 0; i < elements.getLength(); i++) {
            String xpath = XPathHelper.getXPathExpression(elements.item(i));
            if (xpath.equalsIgnoreCase(identification.getValue())) {
                return (Element) elements.item(i);
            }
        }
        return null;
    }

    private boolean isElementInActionSet(String elementXpath, List<Action> actions) {
        if (actions == null)
            return false;

        for (Action action : actions)
            if (action.getActionXpath().equalsIgnoreCase(elementXpath))
                return true;
        return false;
    }

    private CandidateElement createNewCandidateElementWithFormInput(CandidateElement element, List<Action> actions) {
        if (actions.size() == 1 && isLinkOrButton(actions.get(0)))
            return element;

        List<FormInput> formInputs = new ArrayList<FormInput>();
        for (Action action : actions)
            formInputs.add(createOneFormInput(action.getValue(), action.getActionXpath()));
        return new CandidateElement(element.getElement(), element.getIdentification(), "", formInputs, "");
    }

    private boolean isLinkOrButton(Action action) {
        boolean isButton = action.getActionXpath().toUpperCase().contains("BUTTON");
        boolean isLink = action.getActionXpath().toUpperCase().contains("A");
        return isButton || isLink;
    }

    private FormInput createOneFormInput(String inputValue, String elementXpath) {
        FormInput formInput = new FormInput();
        if (!inputValue.equalsIgnoreCase("null")) {
            formInput.setType("text");
            formInput.setIdentification(new Identification(Identification.How.xpath, elementXpath));
            formInput.setInputValues(createValueList(inputValue));
            LOGGER.info("New Form is create : {}", formInput);
        }
        return formInput;
    }

    private Set<InputValue> createValueList(String value) {
        Set<InputValue> transformList = new HashSet<InputValue>();
        transformList.add(new InputValue(value, true));
        return transformList;
    }

    @Override
    public void postCrawling(CrawlSession session, ExitNotifier.ExitStatus exitReason) {
        stateFlowGraph = session.getStateFlowGraph();
    }

    @Override
    public void onUrlLoad(CrawlerContext context) {
        processingDirectiveManagement.resetTargetDirective();
        resetCounter++;
        browser.deleteAllCookies();
        LOGGER.debug("Now reset counter is {}", resetCounter);
        serverInstanceManagement.recordCoverage();
        LOGGER.debug("Resetting ServerInstance...");
        serverInstanceManagement.closeServerInstance();
        serverInstanceManagement.createServerInstance();
        LOGGER.debug("Resetting ServerInstance complete, keep crawling new state");
    }

    public StateFlowGraph getStateFlowGraph() {
        return stateFlowGraph;
    }

    public List<LearningTarget> getLearningTarget() {
        List<LearningTarget> result = new LinkedList<>();
        StateVertex index = stateFlowGraph.getInitialState();
        if (directiveStateVertexComparisonTable.size() == 0) {
            for (StateVertex inputPage : inputStates) {
                List<List<Action>> actionSequence = getTwoStateEventPath(index, inputPage);
                LOGGER.debug("Now action sequence are {}", actionSequence);
                result.add(new LearningTarget(inputPage.getDom(), inputPage.getUrl(), actionSequence));
            }
        } else {
            LOGGER.debug("The last state is {}", lastDirectiveStateVertex);
            List<List<Action>> indexToLastDirectiveEventPath = getTheEventPathBetweenEachDirectiveToLastState();
            LOGGER.debug("Index to last state path are {}", indexToLastDirectiveEventPath);

            for (StateVertex inputPage : inputStates) {
                LOGGER.debug("Adding input page {} to result", inputPage);
                addLastDirectiveToInputPagePathToResult(result, inputPage, indexToLastDirectiveEventPath);
                LOGGER.debug("The total input page size is {}", result.size());
            }
        }
        return result;
    }

    private void addLastDirectiveToInputPagePathToResult(List<LearningTarget> result, StateVertex inputPage, List<List<Action>> indexToLastDirectiveEventPath) {
        ImmutableList<Eventable> eventPath = stateFlowGraph.getShortestPath(lastDirectiveStateVertex, inputPage);
        if (eventPath.size() != 0) {
            LOGGER.info("Last state can go to input page {}", inputPage);
            List<List<Action>> completePath = mergeTwoPath(indexToLastDirectiveEventPath, convertToActionList(eventPath));
            result.add(new LearningTarget(inputPage.getDom(), inputPage.getUrl(), completePath));
        }
        else {
            LOGGER.info("Last state can not go to input page {}", inputPage);
        }
    }

    private List<List<Action>> getTheEventPathBetweenEachDirectiveToLastState() {
        Pair<State, StateVertex> directivePair = directiveStateVertexComparisonTable.poll();
        StateVertex lastDirectiveState = directivePair.getValue();
        StateVertex root = stateFlowGraph.getInitialState();

        List<List<Action>> actionSequence = new LinkedList<>(getTwoStateEventPath(root, lastDirectiveState));
        LOGGER.debug("Find index to directive : {} path is {}", lastDirectiveState, actionSequence);
        while(!directiveStateVertexComparisonTable.isEmpty()) {
            directivePair = directiveStateVertexComparisonTable.poll();
            List<List<Action>> betweenDirectivesPath = getTwoStateEventPath(lastDirectiveState, directivePair.getValue());
            LOGGER.debug("Find directive : {} to directive : {} path is {}", lastDirectiveState, directivePair.getValue(), betweenDirectivesPath);
            actionSequence.addAll(betweenDirectivesPath);
            LOGGER.debug("Now total Path is {}", actionSequence);
            lastDirectiveState = directivePair.getValue();
        }

        List<List<Action>> betweenDirectivesPath = getTwoStateEventPath(lastDirectiveState, lastDirectiveStateVertex);
        LOGGER.debug("Find directive : {} to directive : {} path is {}", lastDirectiveState, lastDirectiveStateVertex, betweenDirectivesPath);
        actionSequence.addAll(betweenDirectivesPath);
        LOGGER.debug("Now total Path is {}", actionSequence);

        return actionSequence;
    }

    private List<List<Action>> getTwoStateEventPath(StateVertex firstState, StateVertex secondState) {
        ImmutableList<Eventable> eventPath = stateFlowGraph.getShortestPath(firstState, secondState);
        if (eventPath.size() != 0) {
            return convertToActionList(eventPath);
        } else {
            return new ArrayList<>(0);
        }
    }

    private List<List<Action>> mergeTwoPath(List<List<Action>> firstPath, List<List<Action>> secondPath) {
        List<List<Action>> result = new LinkedList<>(firstPath);
        result.addAll(secondPath);
        return result;
    }

    private List<List<Action>> convertToActionList(ImmutableList<Eventable> eventPath) {
        List<List<Action>> highLevelActions = new ArrayList<>(eventPath.size());
        for (Eventable event : eventPath) {
            if (event.getEventType() == Eventable.EventType.click) {
                String xpath = event.getIdentification().getValue();
                String value = "";
                LOGGER.info("Now action is eventType is Click, action Xpath is : {}", xpath);
                highLevelActions.add(Collections.singletonList(new Action(xpath, value)));
            } else if (event.getEventType() == Eventable.EventType.input) {
                List<Action> actionSequence = new ArrayList<>(eventPath.size());
                List<FormInput> formInputs = event.getRelatedFormInputs();
                for (FormInput formInput : formInputs) {
                    if (formInput.getInputValues().size() == 0)
                        continue;
                    String xpath = formInput.getIdentification().getValue();
                    String value = formInput.getInputValues().iterator().next().getValue();
                    LOGGER.info("Now action is eventType is input, action Xpath is : {}, value is : {}", xpath, value);
                    actionSequence.add(new Action(xpath, value));
                }
                highLevelActions.add(actionSequence);
            }
        }
        return highLevelActions;
    }
}
