package ntut.edu.aiguide.crawljax.plugins.domain;

import com.google.common.collect.Queues;
import sun.awt.image.ImageWatched;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class State implements Cloneable {
    private final String stateID;
    private final String dom;
    private final Queue<List<Action>> actionSet;
    private Queue<List<Action>> lastActionSet = new LinkedList<>();
	private boolean isIgnoreCrawling;
	private boolean isToCheckFormFillingResult;

    public State(String hashDom, String dom, LinkedList<List<Action>> actionSet, boolean isIgnoreCrawling, boolean isToCheckFormFillingResult) {
        this.stateID = hashDom;
        this.dom = dom;
        this.actionSet = actionSet;
        this.isIgnoreCrawling = isIgnoreCrawling;
		this.isToCheckFormFillingResult = isToCheckFormFillingResult;
    }

    public State(String hashDom, String dom, LinkedList<List<Action>> actionSet) {
        this(hashDom, dom, actionSet, false, false);
    }

    public String getID() {
        return stateID;
    }

    public String getDom() {
        return dom;
    }

	public boolean getIgnoreCrawling() {
		return isIgnoreCrawling;
	}

	public void resetIgnoreCrawling() {
		isIgnoreCrawling = false;
	}

	public boolean getToCheckFormFillingResult() {
		return isToCheckFormFillingResult;
	}

	public void resetToCheckFormFillingResult() {
		isToCheckFormFillingResult = false;
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

