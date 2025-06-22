package ntut.edu.aiguide.crawljax.plugins.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class State implements Cloneable {
    private final String stateID;
    private final String dom;
    private final Queue<List<Action>> actionSet;
    private Queue<List<Action>> lastActionSet = new LinkedList<>();

    public State(String hashDom, String dom, LinkedList<List<Action>> actionSet) {
        this.stateID = hashDom;
        this.dom = dom;
        this.actionSet = actionSet;
    }

    public String getID() {
        return stateID;
    }

    public String getDom() {
        return dom;
    }

    public List<Action> getNextActionSet() {
        if (actionSet.isEmpty())
            return null;
        lastActionSet.add(actionSet.peek());
        return actionSet.poll();
    }

    public List<Action> getLastActionSet() {
        if (lastActionSet.isEmpty())
            return null;
        return lastActionSet.poll();
    }

    public boolean hasNextActionSet() {
        return !actionSet.isEmpty();
    }

    public State clone() throws CloneNotSupportedException {
        return (State) super.clone();
    }
}

