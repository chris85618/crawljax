package ntut.edu.tw.irobot.adapter;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.WebSnapShot;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.iRobotState;
import ntut.edu.tw.irobot.utility.TestHelper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class WebSnapShotMapperTest {


    @Test
    public void mappingStateVertexAndCandidateElementListToWebSnapShot() {
        WebSnapShotMapper webSnapShotMapper = new WebSnapShotMapper();
        ImmutableList<CandidateElement> candidateElements = TestHelper
                .createCandidateElements();
        StateVertex vertex = TestHelper.createStateVertex();


        WebSnapShot webSnapShot = webSnapShotMapper
                .mappingFrom(candidateElements, vertex);


        iRobotState iRobotState = (iRobotState)webSnapShot
                .getState();
        assertThat(iRobotState.getSource(), equalTo(vertex));
        ImmutableList<Action> actions = webSnapShot.getActions();
        TestHelper
                .assertCandidateElementsTransformSuccessfully(actions
                        , candidateElements);
    }
}

