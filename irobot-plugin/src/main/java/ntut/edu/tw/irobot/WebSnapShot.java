package ntut.edu.tw.irobot;

import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;

public class WebSnapShot {

    private ImmutableList<Action> actions;
    private State state;

    public WebSnapShot(ImmutableList<Action> actions
            , State state) {
        this.actions = actions;
        this.state = state;
    }

    public ImmutableList<Action> getActions() {
        return actions;
    }

    public State getState() {
        return state;
    }
}

