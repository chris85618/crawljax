package ntut.edu.aiguide.crawljax.plugins.domain;

import com.google.common.collect.Queues;
import sun.awt.image.ImageWatched;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class State  implements Cloneable{
    private final String stateID;
    private final Queue<List<Action>> actionSet;
    private Queue<List<Action>> lastActionSet = new LinkedList<>();

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
        lastActionSet.add(actionSet.peek());
        List<Action> result = actionSet.poll();
        return result;
    }

    public List<Action> getLastActionSet() {
        if (lastActionSet.isEmpty())
            return  null;
        return lastActionSet.poll();
    }

    public boolean hasNextActionSet() {
        if (actionSet.isEmpty())
            return false;
        return true;
    }

    public State clone() throws CloneNotSupportedException {
        return (State) super.clone();
    }
}
