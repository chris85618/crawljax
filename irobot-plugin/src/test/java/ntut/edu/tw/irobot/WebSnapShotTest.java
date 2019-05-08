package ntut.edu.tw.irobot;

import com.crawljax.core.state.StateVertexImpl;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;
import ntut.edu.tw.irobot.state.iRobotState;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class WebSnapShotTest {

    @Test
    public void initializeWebStatus() {
        ImmutableList<Action> actions = ImmutableList.of();
        State state = new iRobotState(
                new StateVertexImpl(0, "", "", "", ""));
        WebSnapShot webStatus = new WebSnapShot(actions, state);
        assertThat(webStatus.getActions(), equalTo(actions));
        assertThat(webStatus.getState(), equalTo(state));
    }
}