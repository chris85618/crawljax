package ntut.edu.tw.irobot.adapter;

import com.crawljax.core.state.StateVertex;
import ntut.edu.tw.irobot.state.State;
import ntut.edu.tw.irobot.state.iRobotState;

public class StateMapper {
    public State mappingFrom(StateVertex vertex) {
        return new iRobotState(vertex);
    }
}
