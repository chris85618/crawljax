package ntut.edu.tw.irobot.adapter;

import com.crawljax.core.CandidateElement;
import com.google.common.collect.ImmutableList;

import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.action.iRobotAction;
import ntut.edu.tw.irobot.utility.TestHelper;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ActionMapperTest {


    @Test
    public void mappingCandidateElementToAction() {

        ActionMapper actionMapper = new ActionMapper();

        CandidateElement candidateElement = TestHelper.createCandidateElement();

        Action action = actionMapper.mappingFrom(candidateElement);

        assertThat(action.getSource(), equalTo(candidateElement));
        assertTrue(action instanceof iRobotAction);
    }

    @Test
    public void mappingImmutableCandidateElementListToImmutableActionList() {
        ActionMapper actionMapper = new ActionMapper();

        ImmutableList<CandidateElement> candidateElements =
                TestHelper.createCandidateElements();

        ImmutableList<Action> actions =
                actionMapper.mappingImmutableActionFrom(candidateElements);
        TestHelper.
                assertCandidateElementsTransformSuccessfully(actions, candidateElements);
    }
}