package ntut.edu.aiguide.crawljax.plugins.domain;

import com.google.common.collect.Queues;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class State {
    private final Queue<List<Action>> actionSet;
    private final String stateID;

    public State(String hashDom, LinkedList<List<Action>> actionSet) {
        this.stateID = hashDom;
        this.actionSet = actionSet;
    }

    public String getID() {
        return stateID;
    }

    public List<Action> getNextActionSet() {
        if (actionSet.isEmpty())
            return null;
        return actionSet.poll();
    }

    public boolean hasNextActionSet() {
        if (actionSet.isEmpty())
            return false;
        return true;
    }
}
