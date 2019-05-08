package ntut.edu.tw.irobot.utility;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.StateVertexImpl;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.WebSnapShot;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.action.iRobotAction;
import ntut.edu.tw.irobot.state.State;
import ntut.edu.tw.irobot.state.iRobotState;

import java.util.ArrayList;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestHelper {
    public static StateVertex createStateVertex() {
        return new StateVertexImpl(1, "http://google", "Vertex", "DOM", "");
    }

    public static ImmutableList<CandidateElement> createCandidateElements() {
        ArrayList<CandidateElement> candidateElements = new ArrayList<CandidateElement>();
        candidateElements.add(createCandidateElement());
        candidateElements.add(createCandidateElement());
        candidateElements.add(createCandidateElement());
        return ImmutableList.copyOf(candidateElements);
    }

    public static CandidateElement createCandidateElement() {
        return new CandidateElement(null, null, "");
    }

    public static ImmutableList<Action> createAcitons() {
        return ImmutableList.of();
    }

    public static State createState() {
        return new iRobotState(createStateVertex());
    }

    public static Action createAction() {
        return new iRobotAction(createCandidateElement());
    }

    public static WebSnapShot createWebStatus() {
        return new WebSnapShot(createAcitons(), createState());
    }

    public static void assertCandidateElementsTransformSuccessfully(ImmutableList<Action> actions, ImmutableList<CandidateElement> candidateElements) {
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            assertThat(action.getSource(), equalTo(candidateElements.get(i)));
            assertTrue(action instanceof iRobotAction);
        }
    }
}
