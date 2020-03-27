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
                                        PreStateCrawlingPlugin, PostCrawlingPlugin, OnHtmlAttributeFilteringPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIGuidePlugin.class);
    private EmbeddedBrowser browser = null;
    private Map<String, Map<String, List<String>>> variableElementList = new HashMap<>();
    private List<StateVertex> inputStates = new LinkedList<>();

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
    private Set<StateVertex> directiveProcessingStateList = new HashSet<>();
    private State targetState;
    private StateVertex lastState = null;
    private boolean isDirectiveProcess = false;

    private StateFlowGraph stateFlowGraph;

    public AIGuidePlugin(Stack<State> directivePath) {
        this.directivesStack = directivePath;
        this.targetState = getTopOfDirective();
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

    // add element value to dom
    @Override
    public String onNewFoundState(String dom) {
        try {
            Document doc = DomUtils.asDocument(dom);

            addValueAttributeToNode(doc);
            return DomUtils.getDocumentToString(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dom;
    }

    private void addValueAttributeToNode(Document doc) {
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

    // if the directive is not finish, control the crawling depth
    @Override
    public void controlDepth(StateVertex currentState, AtomicInteger crawlDepth) {
        try {
            if (!isDirectiveProcess || !directiveProcessingStateList.contains(currentState))
                crawlDepth.incrementAndGet();
        } catch (Exception e) {
            throw new RuntimeException("Something Wrong when counting the depth");
        }
    }

    @Override
    public void preStateCrawling(CrawlerContext context, ImmutableList<CandidateElement> candidateElements, StateVertex currentState) {
        System.out.println("In preStateCrawling");
        if (isDirectiveProcess || isCurrentStateSameAsTargetState(currentState)) {
            System.out.println("Current state is same as directive");
            directiveProcessingStateList.add(currentState);
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
        if (lastState == null && directivesStack.empty())
            lastState = currentState;
        isDirectiveProcess = true;
        List<Action> actionSet = targetState.getNextActionSet();
        CandidateElement newElement = null;
        if (actionSet == null) {
            isDirectiveProcess = false;
            return;
        }

        if (!targetState.hasNextActionSet()) {
            isDirectiveProcess = false;
            targetState = getTopOfDirective();
        }

        for (CandidateElement element : candidateElements) {
            if (isElementInActionSet(element, actionSet)) {
                newElement = createNewCandidateElementWithFormInput(element, actionSet);
                break;
            }
        }

        System.out.println(newElement);

        if (newElement == null)
            throw new RuntimeException("There something problem when create new element in AIGuidePlugin - changeCandidateElementForCurrentState");
        currentState.setElementsFound(new LinkedList<>(Collections.singletonList(newElement)));
    }

    private boolean isElementInActionSet(CandidateElement element, List<Action> actions) {
        String currentElementXpath = element.getIdentification().getValue();
        for (Action action : actions)
            if (action.getActionXpath().equalsIgnoreCase(currentElementXpath))
                return true;
        return false;
    }

    private CandidateElement createNewCandidateElementWithFormInput(CandidateElement element, List<Action> actions) {
        if (actions.size() == 1)
            return element;

        List<FormInput> formInputs = new ArrayList<FormInput>();
        for (Action action : actions)
            formInputs.add(createOneFormInput(action.getValue(), action.getActionXpath()));
        return new CandidateElement(element.getElement(), element.getIdentification(), "", formInputs, "");
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

    public StateFlowGraph getStateFlowGraph() {
        return stateFlowGraph;
    }

    public List<Pair<String, List<Action>>> getActionSequenceSet() {
        List<Pair<String, List<Action>>> result = new LinkedList<>();
        StateVertex index = stateFlowGraph.getInitialState();
        if (directiveStateVertexComparisonTable.size() == 0) {
            for (StateVertex inputPage : inputStates) {
                List<Action> actionSequence = getTwoStateEventPath(index, inputPage);
                LOGGER.debug("Now action sequence are {}", actionSequence);
                result.add(new Pair<>(String.valueOf(inputPage.getDom().hashCode()), actionSequence));
            }
        } else {
            LOGGER.debug("The last state is {}", lastState);
            List<Action> indexToLastDirectiveEventPath = getTheEventPathBetweenEachDirectiveToLastDirective();
            LOGGER.debug("Index to last state path are {}", indexToLastDirectiveEventPath);

            for (StateVertex inputPage : inputStates) {
                LOGGER.debug("Adding input page {} to result", inputPage);
                addLastDirectiveToInputPagePathToResult(result, inputPage, indexToLastDirectiveEventPath);
                LOGGER.debug("The total input page size is {}", result.size());
            }
        }
        return result;
    }

    private void addLastDirectiveToInputPagePathToResult(List<Pair<String, List<Action>>> result, StateVertex inputPage, List<Action> indexToLastDirectiveEventPath) {
        ImmutableList<Eventable> eventPath = stateFlowGraph.getShortestPath(lastState, inputPage);
        if (eventPath.size() != 0) {
            LOGGER.info("Last state can go to input page {}", inputPage);
            List<Action> completePath = mergeTwoPath(indexToLastDirectiveEventPath, convertToActionList(eventPath));
            result.add(new Pair<>(String.valueOf(inputPage.getDom().hashCode()), completePath));
        }
        else {
            LOGGER.info("Last state can not go to input page {}", inputPage);
        }
    }

    private List<Action> getTheEventPathBetweenEachDirectiveToLastDirective() {
        Pair<State, StateVertex> directivePair = directiveStateVertexComparisonTable.poll();
        StateVertex lastState = directivePair.getValue();
        StateVertex root = stateFlowGraph.getInitialState();


        List<Action> actionSequence = new LinkedList<>(getTwoStateEventPath(root, lastState));

        while(!directiveStateVertexComparisonTable.isEmpty()) {
            directivePair = directiveStateVertexComparisonTable.poll();
            actionSequence.addAll(getTwoStateEventPath(lastState, directivePair.getValue()));
            lastState = directivePair.getValue();
        }
        return actionSequence;
    }

    private List<Action> getTwoStateEventPath(StateVertex firstState, StateVertex secondState) {
        if (stateFlowGraph.canGoTo(firstState, secondState)) {
            ImmutableList<Eventable> eventPath = stateFlowGraph.getShortestPath(firstState, secondState);
            return convertToActionList(eventPath);
        } else {
            return new ArrayList<>(0);
        }
    }

    private List<Action> mergeTwoPath(List<Action> firstPath, List<Action> secondPath) {
        List<Action> result = new LinkedList<>(firstPath);
        result.addAll(secondPath);
        return result;
    }

    private List<Action> convertToActionList(ImmutableList<Eventable> eventPath) {
        List<Action> actionSequence = new ArrayList<>(eventPath.size());
        for (Eventable event : eventPath) {
            if (event.getEventType() == Eventable.EventType.click) {
                String xpath = event.getIdentification().getValue();
                String value = "";
                actionSequence.add(new Action(xpath, value));
            } else if (event.getEventType() == Eventable.EventType.input) {
                List<FormInput> formInputs = event.getRelatedFormInputs();
                for (FormInput formInput : formInputs) {
                    if (formInput.getInputValues().size() == 0)
                        continue;
                    String xpath = formInput.getIdentification().getValue();
                    String value = formInput.getInputValues().iterator().next().getValue();
                    actionSequence.add(new Action(xpath, value));
                }
            }
        }
        return actionSequence;
    }
}
