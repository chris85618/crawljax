package ntut.edu.aiguide.crawljax.plugins;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathExpressionException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.browser.WebDriverBackedEmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier;
import com.crawljax.core.plugin.OnBrowserCreatedPlugin;
import com.crawljax.core.plugin.OnCountingDepthPlugin;
import com.crawljax.core.plugin.OnHtmlAttributeFilteringPlugin;
import com.crawljax.core.plugin.OnNewFoundStatePlugin;
import com.crawljax.core.plugin.OnUrlLoadPlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateVertex;
import com.crawljax.forms.FormInput;
import com.crawljax.forms.InputValue;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XPathHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ntut.edu.aiguide.crawljax.plugins.domain.FormSubmissionJudger.FormSubmissionJudgeResultBuilder;
import ntut.edu.aiguide.crawljax.plugins.domain.TableOutput.ITableOutput;
import ntut.edu.aiguide.crawljax.plugins.domain.TableOutput.SubmitResultBuilder;
import ntut.edu.aiguide.crawljax.plugins.domain.TableOutput.DummySubmitResultBuilder;
import ntut.edu.aiguide.crawljax.plugins.adapter.TableOutput.CsvTableOutput;

import org.apache.commons.lang3.tuple.Pair;
import ntut.edu.aiguide.crawljax.plugins.domain.Action;
import ntut.edu.aiguide.crawljax.plugins.domain.HighLevelAction;
import ntut.edu.aiguide.crawljax.plugins.domain.EditDistanceComparator;
import ntut.edu.aiguide.crawljax.plugins.domain.InputPage;
import ntut.edu.aiguide.crawljax.plugins.domain.LearningTarget;
import ntut.edu.aiguide.crawljax.plugins.domain.ProcessingDirectiveManagement;
import ntut.edu.aiguide.crawljax.plugins.domain.State;
import ntut.edu.aiguide.crawljax.plugins.domain.XPathGenerator;

public class AIGuidePlugin implements OnBrowserCreatedPlugin, OnNewFoundStatePlugin, OnCountingDepthPlugin,
                                        PreStateCrawlingPlugin, PostCrawlingPlugin, OnHtmlAttributeFilteringPlugin,
                                        OnUrlLoadPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIGuidePlugin.class);
    private static final Set<String> editableTagNameSet =  new HashSet<>(Arrays.asList("input", "textarea", "select"));
    private final ProcessingDirectiveManagement processingDirectiveManagement ;
    private final ServerInstanceManagement serverInstanceManagement;
    private FormSubmissionJudgeResultBuilder formSubmissionJudgeResultBuilder;
    private final SubmitResultBuilder submitResultBuilder;
    private final SubmitResultBuilder.RowBuilder submitResultRowBuilder;
    private final int serverPort;
    private Map<String, Map<String, List<String>>> variableElementList = new HashMap<>();
    private Queue<Pair<State, StateVertex>> directiveStateVertexComparisonTable = new LinkedList<>();
    private StateVertex lastDirectiveStateVertex = null;
    private boolean isDirectiveProcess = false;
    private EmbeddedBrowser browser = null;
    private List<InputPage> inputPages = new LinkedList<>();
    private StateFlowGraph stateFlowGraph;
    private int resetCounter = 0;

    /**
     * @param directivePath
     *      directive stack perform like following example:
     *          | root directive |
     *          |    directive   |
     *          |    directive   |
     *          | leaf directive |
     *          ------------------
     * @param serverInstanceManagement
     *      manage the server instance
     * @param serverPort
     *      server port is a parameter that wrapped variable element mechanism
     */
    public AIGuidePlugin(Stack<State> directivePath, ServerInstanceManagement serverInstanceManagement, int serverPort, String submitReportPath) {
        processingDirectiveManagement = new ProcessingDirectiveManagement(directivePath);
        this.serverInstanceManagement = serverInstanceManagement;
        this.serverPort = serverPort;
        if (submitReportPath == null || submitReportPath.isEmpty()) {
            this.submitResultBuilder = new DummySubmitResultBuilder();
        } else {
            final CsvTableOutput csvTableOutput = new CsvTableOutput(submitReportPath);
            this.submitResultBuilder = new SubmitResultBuilder(csvTableOutput);
            if (!csvTableOutput.isEmpty()) {
                this.submitResultBuilder.addTitleRow().writeToFile();
            }
        }
        this.submitResultRowBuilder = submitResultBuilder.getRowBuilder();
        createVariableElementsList();
        this.formSubmissionJudgeResultBuilder = null;
    }

    private void createVariableElementsList() {
        JsonParser jsonParser = new JsonParser();
        try {
            File veList = new File("variableElement/variableElementList.json");
            LOGGER.debug("The file variableElementList.json path is {}", veList.getAbsolutePath());
            JsonArray VEJson = ((JsonObject) jsonParser.parse(new FileReader(veList.getAbsoluteFile()))).getAsJsonArray("variableList");
            if (VEJson != null) {
                for (JsonElement jsonElement : VEJson) {
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
        Map<String, List<String>> elementPair = variableElementList.get(url);

        if (elementPair == null) {
            elementPair = variableElementList.get(url + "/");
            if (elementPair == null)
                return dom;
        }
        return removeTheVariableElements(dom, elementPair);
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
        LOGGER.debug("In onNewFoundState");
        try {
            if (this.formSubmissionJudgeResultBuilder != null) {
                // TODO: Refactor this
                formSubmissionJudgeResultBuilder.setAfterDom(browser.getStrippedDom()); // Added to record after DOM
                final boolean formSubmissionResult = formSubmissionJudgeResultBuilder.build();
                // Add row
                this.submitResultRowBuilder.setResult(Boolean.toString(formSubmissionResult));
                this.submitResultRowBuilder.build();
                this.submitResultBuilder.writeToFile();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            this.formSubmissionJudgeResultBuilder = null;
        }
        try {
            Document doc = DomUtils.asDocument(dom);
            String convertDom = DomUtils.getDocumentToString(doc);
            LOGGER.debug("Now process is {}", isDirectiveProcess);
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

            if (processingDirectiveManagement.isCurrentStateIsDirective(convertDom))
                isDirectiveProcess = true;

            return convertDom;
        } catch (IOException e) {
            LOGGER.warn("There something went wrong when covert to Document...");
        }
        return dom;
    }


    /**
     * Add value, assigned by Directive, to Dom
     * @param doc
     *      which convert dom to doc
     */
    private void addValueAttributeToNode(Document doc) {
        final List<Action> actions =  processingDirectiveManagement.getProcessingStateLastActionSet();
        // handle select
        {
            final NodeList nodes = doc.getElementsByTagName("select");
            for(int i = 0; i < nodes.getLength(); i++) {
                // get the value from current page, not from stripped dom
                final Node node = nodes.item(i);
                final String xpathExpr = XPathHelper.getXPathExpression(node);
                if (!isElementInActionSet(xpathExpr, actions))
                    continue;
                final Identification item = new Identification(Identification.How.xpath, xpathExpr);
                // get browser's element
                final WebElement element = browser.getWebElement(item);
                if (element == null)
                    continue;
                // get all selected options
                final List<WebElement> optionList = element.findElements(By.tagName("option"));
                // TODO: if there are multiple options with same value due to toSet(), the result may be wrong.
                //       This also means the web's design is weird.
                final Set<String> selectedValueSet = optionList.stream()
                                                                .filter(WebElement::isSelected)
                                                                .map(option -> option.getAttribute("value"))
                                                                .collect(Collectors.toSet());
                // set the value into doc
                final org.w3c.dom.Element docSelectElement = (org.w3c.dom.Element) node;
                final NodeList docOptionNodes = docSelectElement.getElementsByTagName("option");
                for (int j = 0; j < docOptionNodes.getLength(); j++) {
                    final Element domOptionElement = (Element) docOptionNodes.item(j);
                    if (selectedValueSet.contains(domOptionElement.getAttribute("value"))) {
                        domOptionElement.setAttribute("selected", "selected");
                    } else {
                        domOptionElement.removeAttribute("selected");
                    }
                }
            }
        }
        // handle the other tags
        {
            final Map<String,Function<WebElement,String>> getValue = ImmutableMap.of(
                "input", element -> element.getAttribute("value"),
                "textarea", element -> element.getText()
            );

            final Map<String,BiConsumer<Element,String>> setValue = ImmutableMap.of(
                "input", (element, value) -> { element.setAttribute("value", value); },
                "textarea", (element, value) -> {
                // Clear the origin text
                while (element.hasChildNodes()) {
                    element.removeChild(element.getFirstChild());
                }
                // Add value into textarea as a TextNode
                final Text textNode = doc.createTextNode(value);
                element.appendChild(textNode);
                }
            );

            final Set<String> editableTagNameSetWithoutSelect = new HashSet<>(Arrays.asList("input", "textarea"));
            for (String editableTagName : editableTagNameSetWithoutSelect) {
                final NodeList nodes = doc.getElementsByTagName(editableTagName);
                for(int i = 0; i < nodes.getLength(); i++) {
                    // get the value from current page, not from stripped dom
                    final Node node = nodes.item(i); 
                    final String xpathExpr = XPathHelper.getXPathExpression(node);
                    if (!isElementInActionSet(xpathExpr, actions))
                        continue;
                    final Identification item = new Identification(Identification.How.xpath, xpathExpr);
                    final WebElement element = browser.getWebElement(item);
                    if (element == null)
                        continue;
                    final Function<WebElement, String> getValueFunc = getValue.get(editableTagName);
                    final String value = getValueFunc.apply(element);
                    // set the value into doc
                    final Element docElement = (Element) node;
                    final BiConsumer<Element, String> setValueFunc = setValue.get(editableTagName);
                    setValueFunc.accept(docElement, value);
                }
            }
        }
    }

    // if the directive is not finish, control the crawling depth
    @Override
    public void controlDepth(StateVertex currentState, AtomicInteger crawlDepth) {
        LOGGER.debug("In Control depth");
        try {
            if (!isDirectiveProcess && !processingDirectiveManagement.isCurrentStateIsProcessingState(currentState))
                crawlDepth.incrementAndGet();
            LOGGER.info("In AI Plugin, after control Depth is {}", crawlDepth.get());
        } catch (Exception e) {
            throw new RuntimeException("Something Wrong when counting the depth");
        }
    }

    @Override
    public void preStateCrawling(CrawlerContext context, ImmutableList<CandidateElement> candidateElements, StateVertex currentState) {
        LOGGER.debug("In preStateCrawling");
        boolean isCurrentStateIsDirective = false;
        if (!isDirectiveProcess) {
            isCurrentStateIsDirective = processingDirectiveManagement.isCurrentStateIsDirective(currentState.getStrippedDom());
        }
        LOGGER.debug("isDirectiveProcess is {}, isCurrentStateIsDirective is {}", isDirectiveProcess, isCurrentStateIsDirective);
        LOGGER.debug("StateVertex: Name is {}, Url is {}, ID is {}, Dom.hashCode is {}, StrippedDom.hashCode is {}",
                currentState.getName(), currentState.getUrl(), currentState.getId(),
                currentState.getDom().hashCode(), currentState.getStrippedDom().hashCode());

        if (isDirectiveProcess || isCurrentStateIsDirective) {
            LOGGER.info("Current state {} is same as directive or is Processing State", currentState);
            isDirectiveProcess = true;
            processingDirectiveManagement.recordCurrentState(currentState);
            lastDirectiveStateVertex = currentState;

            final List<Action> actionSet = changeCandidateElementForCurrentState(candidateElements, currentState);

            // TODO: refactor this
            if (this.formSubmissionJudgeResultBuilder != null) {
                // The last submission failed if the origin this.formSubmissionJudgeResultBuilder existed here
                // (It's a new submission!!!)
                final boolean formSubmissionResult = false;
                this.submitResultRowBuilder.setResult(Boolean.toString(formSubmissionResult));
                this.submitResultRowBuilder.build();
                this.submitResultBuilder.writeToFile();
            }

            this.formSubmissionJudgeResultBuilder = new FormSubmissionJudgeResultBuilder();
            this.formSubmissionJudgeResultBuilder.setBeforeDom(browser.getStrippedDom());
            // Form XPath
            String actionXpath = "";
            // Get Action Input Value
            final StringBuilder actionValueBuilder = new StringBuilder();
            if (actionSet != null && !actionSet.isEmpty()) {
                for (Action action : actionSet) {
                    final String actionString = action.toString();
                    actionValueBuilder.append(actionString).append(" ");
                }

                actionXpath = actionSet.get(0).getActionXpath();
            }
            final String actionValue = actionValueBuilder.toString().trim();

            submitResultRowBuilder.setUrl(currentState.getUrl());
            submitResultRowBuilder.setXpath(actionXpath);
            submitResultRowBuilder.setValue(actionValue);
        } else if (isCurrentStateIsInputPage(candidateElements)) {
            LOGGER.info("Current page is input page, not going to crawled");
            if (isSimilarDomInInputPageList(currentState.getStrippedDom())) {
                currentState.setElementsFound(new LinkedList<>());
            } else if (isAllDirectiveProcessed()) {
                EmbeddedBrowser browser = context.getBrowser();
                WebDriver driver = ((WebDriverBackedEmbeddedBrowser) browser).getBrowser();
                List<String> formXPaths = new ArrayList<>();
                WebElement rootElement = browser.getWebElement(new Identification(Identification.How.tag, "html"));
                for (WebElement formElement : rootElement.findElements(By.tagName("form"))) {
                    if (formElement.isEnabled() && formElement.isDisplayed()) {
                        formXPaths.add(XPathGenerator.getAbsoluteXPath(driver, formElement));
                    }
                }
                inputPages.add(new InputPage(currentState, formXPaths));
                LOGGER.debug("Get id the inputStats");
                for (InputPage s : inputPages)
                    LOGGER.debug("Get id of the inputState is {}", s.getStateVertex().getId());
            }
        } else {
            LOGGER.info("Current page is not input page, moving on...");
        }
    }

    private boolean isAllDirectiveProcessed() {
        return processingDirectiveManagement.isAllDirectiveIsProcessed();
    }

    private boolean isSimilarDomInInputPageList(String dom) {
        EditDistanceComparator editDistanceComparator = new EditDistanceComparator(0.98D);
        for (InputPage inputPage: inputPages) {
            LOGGER.debug("inputPage = {}", inputPage);
            LOGGER.debug("inputPage.getStateVertex().getStrippedDom().length() = {}", inputPage.getStateVertex().getStrippedDom().length());
            LOGGER.debug("dom.length() = {}", dom.length());
            boolean isEquivalent = editDistanceComparator.isEquivalent(inputPage.getStateVertex().getStrippedDom(), dom);
            LOGGER.debug("isEquivalent = {}", isEquivalent);
            if (isEquivalent) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentStateIsInputPage(ImmutableList<CandidateElement> candidateElements) {
        candidateElements.forEach(candidateElement -> LOGGER.debug("The candidateElement is {}", candidateElement));
        List<CandidateElement> isInteractableElements = candidateElements.parallelStream()
                .filter(candidateElement -> editableTagNameSet.stream().anyMatch(candidateElement.getElement().getTagName()::equalsIgnoreCase))
                .filter(candidateElement -> candidateElement.getIdentification().getValue().toLowerCase().contains("form"))
                .filter(candidateElement -> browser.isInteractive(candidateElement.getIdentification().getValue()))
                .collect(Collectors.toList());
        isInteractableElements.forEach(candidateElement -> LOGGER.debug("The interactableInputElement is {}", candidateElement));
        return !isInteractableElements.isEmpty();
    }

    private List<Action> changeCandidateElementForCurrentState(ImmutableList<CandidateElement> candidateElements, StateVertex currentState) {
        List<Action> actionSet = processingDirectiveManagement.getProcessingStateNextActionSet();
        CandidateElement newElement = null;

        if (actionSet == null) {
            isDirectiveProcess = false;
            processingDirectiveManagement.removeLastStateInRecordList();
            currentState.setElementsFound(new LinkedList<> (candidateElements));
            return actionSet;
        }

        if (!processingDirectiveManagement.isProcessingStateHasNextActionSet()) {
            isDirectiveProcess = false;
            processingDirectiveManagement.removeLastStateInRecordList();
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
                    for (String editableTagName : editableTagNameSet) {
                        element = findCorrespondElement(identification, currentState.getDocument(), editableTagName);
                        if (element != null) {
                            break;
                        }
                    }
                    if (element == null) {
                        LOGGER.error("No corresponding legal Element found in: URL ( {}: {}", identification.getHow(), identification.getValue());
                        LOGGER.error("  in URL: {}", currentState.getUrl());
                        LOGGER.error("  with locator {}: {}", identification.getHow(), identification.getValue());
                        // LOGGER.error("=====================================");
                        // LOGGER.debug("Page DOM: {}", currentState.getDom());
                        // LOGGER.error("=====================================");
                        throw new NoSuchElementException("CandidateElement with null element detected with " + identification.getHow() + ": " + identification.getValue());
                    }
                    List<FormInput> formInputs = new ArrayList<FormInput>();
                    for (Action action : actionSet)
                        formInputs.add(createOneFormInput(action.getValue(), action.getActionXpath()));
                    newElement = new CandidateElement(element, identification, "",formInputs, actionSet.get(0).getValue());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("There something went wrong when create...");
            throw new RuntimeException(e);
        }

        currentState.setElementsFound(new LinkedList<>(Collections.singletonList(newElement)));
        return actionSet;
    }

    private Element findElementInCurrentState(Identification identification, StateVertex currentState) {
        Element findElement = null;
        try {
            findElement = findCorrespondElement(identification, currentState.getDocument(), "a");
            if (findElement == null)
                findElement = findCorrespondElement(identification, currentState.getDocument(), "button");
            if (findElement == null) {
                String type = this.getElementType(identification.getValue());
                LOGGER.debug("element type is {}", type);
                if ("button".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type)
                        || "reset".equalsIgnoreCase(type) || "image".equalsIgnoreCase(type)) {
                    findElement = this.findCorrespondElement(identification, currentState.getDocument(), "input");
                }
            }
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
        String xpath = action.getActionXpath();
        String type = this.getElementType(xpath);
        boolean isButton = xpath.toUpperCase().contains("BUTTON");
        boolean isLink = xpath.toUpperCase().contains("A");
        boolean isInputButton = xpath.toUpperCase().contains("INPUT") &&
                ("button".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type)
                        || "reset".equalsIgnoreCase(type) || "image".equalsIgnoreCase(type));
        return isButton || isLink || isInputButton;
    }

    private FormInput createOneFormInput(String inputValue, String elementXpath) {
        FormInput formInput = new FormInput();
        if (!inputValue.equalsIgnoreCase("null")) {
            String type = getElementType(elementXpath);
            formInput.setType(type);
            formInput.setIdentification(new Identification(Identification.How.xpath, elementXpath));
            formInput.setInputValues(createValueList(type, inputValue));
            LOGGER.info("New Form is create : {}", formInput);
        }
        return formInput;
    }

    private String getElementType(String elementXpath) {
        WebElement element = browser.getWebElement(new Identification(Identification.How.xpath, elementXpath));
        final String typeName = element.getAttribute("type");
        if (typeName != null) {
            return typeName.toLowerCase();
        } else if (editableTagNameSet.stream().anyMatch(element.getTagName()::equalsIgnoreCase)) {
            return "text";
        } else {
            return element.getTagName().toLowerCase();
        }
    }

    private Set<InputValue> createValueList(String type, String value) {
        Set<InputValue> transformList = new HashSet<InputValue>();
        if (type.equalsIgnoreCase("checkbox")) {
            if (value.equalsIgnoreCase("false"))
                transformList.add(new InputValue(value, false));
            else
                transformList.add(new InputValue(value, true));
        }
        else
            transformList.add(new InputValue(value, true));

        return transformList;
    }

    @Override
    public void postCrawling(CrawlSession session, ExitNotifier.ExitStatus exitReason) {
        stateFlowGraph = session.getStateFlowGraph();
    }

    @Override
    public void onUrlLoad(CrawlerContext context) {
        resetCounter++;
        if (resetCounter > 2) {
            isDirectiveProcess = false;
        }
        browser.deleteAllCookies();
        LOGGER.debug("Now reset counter is {}", resetCounter);
        serverInstanceManagement.recordCoverage();
        LOGGER.debug("Resetting ServerInstance...");
        serverInstanceManagement.closeServerInstance();
        serverInstanceManagement.createServerInstance();
        LOGGER.debug("Resetting ServerInstance complete.");
    }

    public StateFlowGraph getStateFlowGraph() {
        return stateFlowGraph;
    }

    public List<LearningTarget> getLearningTarget() {
        List<LearningTarget> result = new LinkedList<>();
        StateVertex index = stateFlowGraph.getInitialState();
        if (directiveStateVertexComparisonTable.size() == 0) {
            for (InputPage inputPage : inputPages) {
                StateVertex stateVertex = inputPage.getStateVertex();
                List<HighLevelAction> actionSequence = getTwoStateEventPath(index, stateVertex);
                LOGGER.debug("Now action sequence are {}", actionSequence);
                result.add(new LearningTarget(stateVertex.getStrippedDom(), stateVertex.getUrl(), inputPage.getFormXPaths(), actionSequence));
            }
        } else {
            LOGGER.debug("The last state is {}", lastDirectiveStateVertex);
            List<HighLevelAction> indexToLastDirectiveEventPath = getTheEventPathBetweenEachDirectiveToLastState();
            LOGGER.debug("Index to last state path are {}", indexToLastDirectiveEventPath);

            for (InputPage inputPage : inputPages) {
                LOGGER.debug("Adding input page {} to result", inputPage.getStateVertex());
                addLastDirectiveToInputPagePathToResult(result, inputPage, indexToLastDirectiveEventPath);
                LOGGER.debug("The total input page size is {}", result.size());
            }
        }

        if (result.isEmpty())
            processingDirectiveManagement.printRetainDirectives();
        return result;
    }

    private void addLastDirectiveToInputPagePathToResult(List<LearningTarget> result, InputPage inputPage, List<HighLevelAction> indexToLastDirectiveEventPath) {
        StateVertex stateVertex = inputPage.getStateVertex();
        ImmutableList<Eventable> eventPath = stateFlowGraph.getShortestPath(lastDirectiveStateVertex, stateVertex);
        if (eventPath.size() != 0) {
            LOGGER.info("Last state can go to input page {}", inputPage);
            List<HighLevelAction> completePath = mergeTwoPath(indexToLastDirectiveEventPath, convertToActionList(eventPath));
            result.add(new LearningTarget(stateVertex.getStrippedDom(), stateVertex.getUrl(), inputPage.getFormXPaths(), completePath));
        }
        else {
            LOGGER.info("Last state can not go to input page {}", inputPage);
        }
    }

    private List<HighLevelAction> getTheEventPathBetweenEachDirectiveToLastState() {
        Pair<State, StateVertex> directivePair = directiveStateVertexComparisonTable.poll();
        StateVertex lastDirectiveState = directivePair.getValue();
        StateVertex root = stateFlowGraph.getInitialState();

        final List<HighLevelAction> actionSequence = new LinkedList<>(getTwoStateEventPath(root, lastDirectiveState));
        LOGGER.debug("Find index to directive : {} path is {}", lastDirectiveState, actionSequence);
        while(!directiveStateVertexComparisonTable.isEmpty()) {
            directivePair = directiveStateVertexComparisonTable.poll();
            List<HighLevelAction> betweenDirectivesPath = getTwoStateEventPath(lastDirectiveState, directivePair.getValue());
            LOGGER.debug("Find directive : {} to directive : {} path is {}", lastDirectiveState, directivePair.getValue(), betweenDirectivesPath);
            actionSequence.addAll(betweenDirectivesPath);
            LOGGER.debug("Now total Path is {}", actionSequence);
            lastDirectiveState = directivePair.getValue();
        }

        List<HighLevelAction> betweenDirectivesPath = getTwoStateEventPath(lastDirectiveState, lastDirectiveStateVertex);
        LOGGER.debug("Find directive : {} to directive : {} path is {}", lastDirectiveState, lastDirectiveStateVertex, betweenDirectivesPath);
        actionSequence.addAll(betweenDirectivesPath);
        LOGGER.debug("Now total Path is {}", actionSequence);

        return actionSequence;
    }

    private List<HighLevelAction> getTwoStateEventPath(StateVertex firstState, StateVertex secondState) {
        ImmutableList<Eventable> eventPath = stateFlowGraph.getShortestPath(firstState, secondState);
        if (eventPath.size() != 0) {
            return convertToActionList(eventPath);
        } else {
            return new ArrayList<>(0);
        }
    }

    private List<HighLevelAction> mergeTwoPath(List<HighLevelAction> firstPath, List<HighLevelAction> secondPath) {
        List<HighLevelAction> result = new LinkedList<>(firstPath);
        result.addAll(secondPath);
        return result;
    }

    private List<HighLevelAction> convertToActionList(ImmutableList<Eventable> eventPath) {
        List<HighLevelAction> highLevelActions = new ArrayList<>(eventPath.size());
        for (Eventable event : eventPath) {
            if (event.getEventType() == Eventable.EventType.click) {
                String xpath = event.getIdentification().getValue();
                String value = "";
                LOGGER.info("Now action is eventType is Click, action Xpath is : {}", xpath);
                final HighLevelAction newHighLevelAction = new HighLevelAction(new Action(xpath, value));
                highLevelActions.add(newHighLevelAction);
            } else if (event.getEventType() == Eventable.EventType.input) {
                List<FormInput> formInputs = event.getRelatedFormInputs();
                final HighLevelAction actionSequence = new HighLevelAction(formInputs.size());
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
