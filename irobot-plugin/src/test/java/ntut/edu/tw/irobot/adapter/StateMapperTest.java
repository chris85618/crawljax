package ntut.edu.tw.irobot.adapter;

import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.StateVertexImpl;
import ntut.edu.tw.irobot.state.State;
import ntut.edu.tw.irobot.state.iRobotState;
import ntut.edu.tw.irobot.utility.TestHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class StateMapperTest {

    @Test
    public void mappingStateVertexToState() {
        StateMapper stateMapper = new StateMapper();

        StateVertex vertex = TestHelper.createStateVertex();

        State state =  stateMapper.mappingFrom(vertex);

        assertThat(state.getId(), equalTo(vertex.getId()));
        assertThat(state.getDom(), equalTo(vertex.getStrippedDom()));
        assertThat(state.getName(), equalTo(vertex.getName()));
        assertThat(state.getUrl(), equalTo(vertex.getUrl()));
        assertTrue(state instanceof iRobotState);
    }
}


