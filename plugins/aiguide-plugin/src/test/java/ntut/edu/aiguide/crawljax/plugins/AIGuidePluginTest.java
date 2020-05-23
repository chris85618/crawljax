package ntut.edu.aiguide.crawljax.plugins;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.ExitNotifier;
import com.crawljax.core.state.*;
import com.crawljax.forms.FormInput;
import com.crawljax.util.DomUtils;
import com.google.common.collect.ImmutableList;
import ntut.edu.aiguide.crawljax.plugins.domain.Action;
import ntut.edu.aiguide.crawljax.plugins.domain.LearningTarget;
import ntut.edu.aiguide.crawljax.plugins.domain.State;
import ntut.edu.aiguide.crawljax.plugins.mockObject.MockBrowser;
import ntut.edu.aiguide.crawljax.plugins.mockObject.MockElement;
import ntut.edu.aiguide.crawljax.plugins.mockObject.StateVertexFactoryForTest;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.jetty.util.resource.Resource;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AIGuidePluginTest {
    private AIGuidePlugin aiGuidePlugin;
    @Before
    public void setUp() throws Exception {
        Stack<State> directives = generateFakeDirectives();
        aiGuidePlugin = new AIGuidePlugin(directives, Mockito.mock(ServerInstanceManagement.class), 3000);
    }

    private Stack<State> generateFakeDirectives() {
        try {
            String dom_1 = "state1_DOM";
            Document doc = DomUtils.asDocument(dom_1);
            String covertDom_1 = DomUtils.getDocumentToString(doc);
            int stateHash_1 = covertDom_1.hashCode();
            List<Action> actions_1_1 = new LinkedList<>(Arrays.asList(new Action("/HTML/INPUT[0]", "2"),
                    new Action("/HTML/INPUT[1]", "4")));
            List<Action> actions_1_2 = new LinkedList<>(Collections.singletonList(new Action("/HTML/INPUT[3]", "3")));
            List<Action> actions_1_3 = new LinkedList<>(Collections.singletonList(new Action("/HTML/INPUT[4]", "4")));
            LinkedList<List<Action>> actionSet_1 = new LinkedList<>(Arrays.asList(actions_1_1, actions_1_2, actions_1_3));
            State state_1 = new State(String.valueOf(stateHash_1), actionSet_1);


            String dom_2 = "state3_DOM";
            doc = DomUtils.asDocument(dom_2);
            String covertDom_2 = DomUtils.getDocumentToString(doc);
            int stateHash_2 = covertDom_2.hashCode();
            List<Action> actions_2 = new LinkedList<>(Arrays.asList(new Action("/HTML/BUTTON[0]", "")));
            LinkedList<List<Action>> actionSet_2 = new LinkedList<>(Collections.singletonList(actions_2));
            State state_2 = new State(String.valueOf(stateHash_2), actionSet_2);

            Stack<State> directives = new Stack<>();

            directives.push(state_2);
            directives.push(state_1);
            return directives;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void testIsFilterFunctioningProperly() {
        System.out.println(Resource.newClassPathResource("/site/VariableElementPage.html").getURI().getPath());
        String path = Paths.get(Resource.newClassPathResource("/site/VariableElementPage.html").getURI()).toString();
        String correctPath = Paths.get(Resource.newClassPathResource("/site/RemovedVariableElementPage.html").getURI()).toString();
        try {
            String dom = new String(Files.readAllBytes(Paths.get(path)));
            String correctDOM = new String(Files.readAllBytes(Paths.get(correctPath)));
            String filterDOM = aiGuidePlugin.filterDom(dom.trim(), "http://test_data").trim();
            assertEquals(correctDOM, filterDOM);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void filterDom() {
        System.out.println(Resource.newClassPathResource("/site/VariableElementPage.html").getURI().getPath());
        String path = Paths.get(Resource.newClassPathResource("/site/VariableElementPage.html").getURI()).toString();
        String correctPath = Paths.get(Resource.newClassPathResource("/site/RemovedVariableElementPage.html").getURI()).toString();
        try {
            String dom = new String(Files.readAllBytes(Paths.get(path)));
            String correctDOM = new String(Files.readAllBytes(Paths.get(correctPath)));
            String filterDOM = aiGuidePlugin.filterDom(dom.trim(), "http://test_data").trim();
            assertEquals(correctDOM, filterDOM);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCurrentStateIsDirectiveAndWillGiveOneActionToCrawler() {
        ImmutableList<CandidateElement> candidateElements = generatePageOneCandidateElement();
        StateVertex stateOne = new StateVertexImpl(0, "", "state1", "state1_DOM", "state1_DOM");
        assertNull(stateOne.getCandidateElements());
        AtomicInteger counter = new AtomicInteger();
        aiGuidePlugin.onNewFoundState("state1_DOM");
        aiGuidePlugin.controlDepth(stateOne, counter);
        aiGuidePlugin.preStateCrawling(null, candidateElements, stateOne);
        assertNotNull(stateOne.getCandidateElements());
        assertEquals(1, stateOne.getCandidateElements().size());
        assertEquals(2, stateOne.getCandidateElements().get(0).getFormInputs().size());
    }

    @Test
    public void testGiveFiveStateAndFourEventEdgeAndWillGenerateCorrectActionSequence() {
        InMemoryStateFlowGraph stateFlowGraph = new InMemoryStateFlowGraph(new ExitNotifier(0), new StateVertexFactoryForTest());
        StateVertex index = new StateVertexImpl(0, "", "index", "index_DOM", "index_DOM");
        StateVertex state_1 = new StateVertexImpl(1, "", "state1", "state1_DOM", "state1_DOM");
        StateVertex state_2 = new StateVertexImpl(2, "", "state2", "state2_DOM", "state2_DOM");
        StateVertex state_3 = new StateVertexImpl(3, "", "state3", "state3_DOM", "state3_DOM");
        StateVertex inputPage = new StateVertexImpl(4, "", "state4", "inputPage", "inputPage");
        Eventable event_1 = new Eventable(new Identification(Identification.How.xpath, "path1"), Eventable.EventType.click);
        List<FormInput> formInputs = new ArrayList<>();
        formInputs.add( new FormInput("text", new Identification(Identification.How.xpath, "/HTML/INPUT[3]"), "2"));
        Eventable event_2 = new Eventable(new CandidateElement((Element) new MockElement("input"), "/HTML/INPUT[3]", formInputs), Eventable.EventType.input);
        formInputs = new ArrayList<>();
        formInputs.add( new FormInput("text", new Identification(Identification.How.xpath, "/HTML/INPUT[0]"), "2"));
        formInputs.add( new FormInput("text", new Identification(Identification.How.xpath, "/HTML/INPUT[1]"), "3"));
        Eventable event_3 = new Eventable(new CandidateElement((Element) new MockElement("input"), "/HTML/INPUT[0]", formInputs), Eventable.EventType.input);
        Eventable event_4 = new Eventable(new CandidateElement((Element) new MockElement("button"), "/HTML/BUTTON[0]", new ArrayList<>()), Eventable.EventType.click);
        stateFlowGraph.putIndex(index);
        stateFlowGraph.putIfAbsent(state_1);
        stateFlowGraph.putIfAbsent(state_2);
        stateFlowGraph.putIfAbsent(state_3);
        stateFlowGraph.putIfAbsent(inputPage);
        stateFlowGraph.addEdge(index, state_1, event_1);
        stateFlowGraph.addEdge(state_1, state_2, event_2);
        stateFlowGraph.addEdge(state_2, state_3, event_3);
        stateFlowGraph.addEdge(state_3, inputPage, event_4);

        CrawlSession crawlSession = new CrawlSession(null, stateFlowGraph, index, null);
        aiGuidePlugin.onBrowserCreated(new MockBrowser());
        aiGuidePlugin.postCrawling(crawlSession, null);
        AtomicInteger counter = new AtomicInteger();

        aiGuidePlugin.onNewFoundState(state_1.getStrippedDom());
        aiGuidePlugin.controlDepth(state_1, counter);
        aiGuidePlugin.preStateCrawling(null, generatePageOneCandidateElement(), state_1);

        aiGuidePlugin.onNewFoundState(state_2.getStrippedDom());
        aiGuidePlugin.controlDepth(state_2, counter);
        aiGuidePlugin.preStateCrawling(null, generatePageOneCandidateElement(), state_2);

        aiGuidePlugin.onNewFoundState(state_3.getStrippedDom());
        aiGuidePlugin.controlDepth(state_3, counter);
        aiGuidePlugin.preStateCrawling(null, generatePageOneCandidateElement(), state_3);

        aiGuidePlugin.onNewFoundState(inputPage.getStrippedDom());
        aiGuidePlugin.controlDepth(inputPage, counter);
        aiGuidePlugin.preStateCrawling(null, generatePageOneCandidateElement(), inputPage);

        List<LearningTarget> result = aiGuidePlugin.getLearningTarget();

        assertEquals(1, result.size());
        assertEquals("inputPage", result.get(0).getDom());
        assertEquals(4, result.get(0).getActionSequence().size());
    }

    private ImmutableList<CandidateElement> generatePageOneCandidateElement() {
        ImmutableList.Builder<CandidateElement> candidateElementsBuilder = new ImmutableList.Builder<>();
        CandidateElement e1 = new CandidateElement((Element) new MockElement("input"), "/HTML/INPUT[0]", new ArrayList<>());
        CandidateElement e2 = new CandidateElement((Element) new MockElement("input"), "/HTML/INPUT[1]", new ArrayList<>());
        CandidateElement e3 = new CandidateElement((Element) new MockElement("input"), "/HTML/INPUT[2]", new ArrayList<>());
        CandidateElement e4 = new CandidateElement((Element) new MockElement("input"), "/HTML/INPUT[3]", new ArrayList<>());
        CandidateElement e5 = new CandidateElement((Element) new MockElement("input"), "/HTML/INPUT[4]", new ArrayList<>());
        CandidateElement e6 = new CandidateElement((Element) new MockElement("Button"), "/HTML/BUTTON[0]", new ArrayList<>());
        candidateElementsBuilder.add(e1);
        candidateElementsBuilder.add(e2);
        candidateElementsBuilder.add(e3);
        candidateElementsBuilder.add(e4);
        candidateElementsBuilder.add(e5);
        candidateElementsBuilder.add(e6);
        return candidateElementsBuilder.build();
    }

    @Test
    public void testWhenStateIsDirectiveWAndTheDepthWillNotIncreaseUntilTheDirectiveFinish() throws IOException {
        aiGuidePlugin.onBrowserCreated(new MockBrowser());
        AtomicInteger counter = new AtomicInteger();
        assertEquals(0, counter.get());

        // first time
        Document doc = DomUtils.asDocument("state1_DOM");
        String expectedDOM_1 = DomUtils.getDocumentToString(doc);
        StateVertex stateOne = new StateVertexImpl(1, "fakeURL", "state1", expectedDOM_1, expectedDOM_1);
        ImmutableList<CandidateElement> candidateElements = generatePageOneCandidateElement();
        String generateDOM = aiGuidePlugin.onNewFoundState("state1_DOM");
        assertEquals(expectedDOM_1, generateDOM);

        aiGuidePlugin.preStateCrawling(null, candidateElements, stateOne);
        assertEquals(0, counter.get());

        // second time
        doc = DomUtils.asDocument("state2_DOM");
        NodeList elements = doc.getElementsByTagName("body");
        ((org.w3c.dom.Element) elements.item(0)).setAttribute("name", "state1");
        String expectedDOM_2 = DomUtils.getDocumentToString(doc);
        StateVertex unNecessaryState = new StateVertexImpl(2, "", "state2", expectedDOM_2, expectedDOM_2);
        candidateElements = generatePageOneCandidateElement();
        generateDOM = aiGuidePlugin.onNewFoundState("state2_DOM");
        assertEquals(expectedDOM_2, generateDOM);

        aiGuidePlugin.controlDepth(unNecessaryState, counter);
        assertEquals(0, counter.get());

        aiGuidePlugin.preStateCrawling(null, candidateElements, unNecessaryState);
        System.out.println("------------------------");
        // third time
        doc = DomUtils.asDocument("state4_DOM");
        elements = doc.getElementsByTagName("body");
        ((org.w3c.dom.Element) elements.item(0)).setAttribute("name", "state2");
        String expectedDOM_3 = DomUtils.getDocumentToString(doc);
        StateVertex unNecessaryState_2 = new StateVertexImpl(4, "", "state4", expectedDOM_3, expectedDOM_3);
        candidateElements = generatePageOneCandidateElement();
        generateDOM = aiGuidePlugin.onNewFoundState("state4_DOM");
        assertEquals(expectedDOM_3, generateDOM);

        aiGuidePlugin.controlDepth(unNecessaryState_2, counter);
        assertEquals(0, counter.get());

        aiGuidePlugin.preStateCrawling(null, candidateElements, unNecessaryState_2);


        // forth time
        doc = DomUtils.asDocument("state5_DOM");
        String expectedDOM_4 = DomUtils.getDocumentToString(doc);
        StateVertex unNecessaryState_3 = new StateVertexImpl(5, "", "state5", expectedDOM_4, expectedDOM_4);
        candidateElements = generatePageOneCandidateElement();
        generateDOM = aiGuidePlugin.onNewFoundState("state5_DOM");
        assertEquals(expectedDOM_4, generateDOM);

        aiGuidePlugin.controlDepth(unNecessaryState_3, counter);
        assertEquals(1, counter.get());
        aiGuidePlugin.preStateCrawling(null, candidateElements, unNecessaryState_3);

        aiGuidePlugin.onUrlLoad(null);
        counter.set(0);

        aiGuidePlugin.controlDepth(stateOne, counter);
        assertEquals(0, counter.get());

        aiGuidePlugin.controlDepth(unNecessaryState, counter);
        assertEquals(0, counter.get());

        aiGuidePlugin.controlDepth(unNecessaryState_2, counter);
        assertEquals(0, counter.get());

        aiGuidePlugin.controlDepth(unNecessaryState_3, counter);
        assertEquals(1, counter.get());
    }
}