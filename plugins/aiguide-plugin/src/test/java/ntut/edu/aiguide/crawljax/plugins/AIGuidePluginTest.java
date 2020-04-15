package ntut.edu.aiguide.crawljax.plugins;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.StateVertexImpl;
import com.google.common.collect.ImmutableList;
import ntut.edu.aiguide.crawljax.plugins.domain.Action;
import ntut.edu.aiguide.crawljax.plugins.domain.State;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.jetty.util.resource.Resource;

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
        aiGuidePlugin = new AIGuidePlugin(directives, null);
    }

    private Stack<State> generateFakeDirectives() {
        String dom_1 = "state1_DOM";
        int stateHash_1 = dom_1.hashCode();
        List<Action> actions_1_1 = new LinkedList<>(Arrays.asList(new Action("/HTML/INPUT[0]", "2"),
                                             new Action("/HTML/INPUT[1]", "4")));
        List<Action> actions_1_2 = new LinkedList<>(Arrays.asList(new Action("/HTML/INPUT[3]", "2")));
        LinkedList<List<Action>> actionSet_1_1 = new LinkedList<>(Arrays.asList(actions_1_1, actions_1_2));
        State state_1 = new State(String.valueOf(stateHash_1), actionSet_1_1);

        String dom_2 = "state2_DOM";
        int stateHash_2 = dom_2.hashCode();
        List<Action> actions_2 = new LinkedList<>(Arrays.asList(new Action("/HTML/BUTTON[0]", "")));
        LinkedList<List<Action>> actionSet_2 = new LinkedList<>(Collections.singletonList(actions_2));
        State state_2 = new State(String.valueOf(stateHash_2), actionSet_2);

        Stack<State> directives = new Stack<>();

        directives.push(state_2);
        directives.push(state_1);
        return directives;
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
    public void preStateCrawling() {
        ImmutableList<CandidateElement> candidateElements = generatePageOneCandidateElement();
        StateVertex stateOne = new StateVertexImpl(0, "", "", "state1_DOM", "state1_DOM");
        assertNull(stateOne.getCandidateElements());
        aiGuidePlugin.preStateCrawling(null, candidateElements, stateOne);
        assertNotNull(stateOne.getCandidateElements());
        assertEquals(1, stateOne.getCandidateElements().size());
    }

    private ImmutableList<CandidateElement> generatePageOneCandidateElement() {
        ImmutableList.Builder<CandidateElement> candidateElementsBuilder = new ImmutableList.Builder<>();
        CandidateElement e1 = new CandidateElement(null, "/HTML/INPUT[0]", new ArrayList<>());
        CandidateElement e2 = new CandidateElement(null, "/HTML/INPUT[1]", new ArrayList<>());
        CandidateElement e3 = new CandidateElement(null, "/HTML/INPUT[2]", new ArrayList<>());
        CandidateElement e4 = new CandidateElement(null, "/HTML/INPUT[3]", new ArrayList<>());
        CandidateElement e5 = new CandidateElement(null, "/HTML/INPUT[4]", new ArrayList<>());
        candidateElementsBuilder.add(e1);
        candidateElementsBuilder.add(e2);
        candidateElementsBuilder.add(e3);
        candidateElementsBuilder.add(e4);
        candidateElementsBuilder.add(e5);
        return candidateElementsBuilder.build();
    }

    @Test
    public void testWhenStateIsDirectiveWAndTheDepthWillNotIncreaseUntilTheDirectiveFinish() {
        AtomicInteger counter = new AtomicInteger();
        assertEquals(0, counter.get());
        ImmutableList<CandidateElement> candidateElements = generatePageOneCandidateElement();
        StateVertex stateOne = new StateVertexImpl(0, "fakeURL", "state1", "state1_DOM", "state1_DOM");
        aiGuidePlugin.preStateCrawling(null, candidateElements, stateOne);
        aiGuidePlugin.controlDepth(stateOne, counter);
        assertEquals(0, counter.get());

        StateVertex unNecessaryState = new StateVertexImpl(0, "", "", "state3_DOM", "state3_DOM");
        candidateElements = generatePageOneCandidateElement();
        aiGuidePlugin.preStateCrawling(null, candidateElements, unNecessaryState);
        aiGuidePlugin.controlDepth(unNecessaryState, counter);
        assertEquals(1, counter.get());
    }
}