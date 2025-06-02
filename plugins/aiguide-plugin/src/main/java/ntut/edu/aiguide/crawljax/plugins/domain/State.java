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
	private boolean shouldCheckFormFillingResult;

    public State(String hashDom, String dom, LinkedList<List<Action>> actionSet, boolean shouldCheckFormFillingResult) {
        // Handle shouldCheckFormFillingResult
        if (shouldCheckFormFillingResult) {
            final int sizeOfActionSet = actionSet.size();
            if (sizeOfActionSet > 0) {
                final int lastIndexOfActionSet = sizeOfActionSet - 1;
                this.lastActionSet.add(actionSet.get(lastIndexOfActionSet));
                // Set the first action
                for (List<Action> actionList : actionSet) {
                    if (!actionList.isEmpty()) {
                        actionList.get(0).setDomRecordAction(Action.DomRecordAction.RECORD_BEFORE);
                        break;
                    }
                }
                // Set the last action
                for (int i = lastIndexOfActionSet; i >= 0; i--) {
                    List<Action> actionList = actionSet.get(i);
                    if (!actionList.isEmpty()) {
                        final int sizeOfActionList = actionList.size();
                        if (sizeOfActionList > 0) {
                            final int lastIndex = sizeOfActionList - 1;
                            actionList.get(lastIndex).setDomRecordAction(Action.DomRecordAction.RECORD_AFTER);
                            break;
                        }
                    }
                }
            }
        }
        // Initialization
        this.stateID = hashDom;
        this.dom = dom;
        this.actionSet = actionSet;
		this.shouldCheckFormFillingResult = shouldCheckFormFillingResult;
    }

    public State(String hashDom, String dom, LinkedList<List<Action>> actionSet) {
        this(hashDom, dom, actionSet, false);
    }

    public String getID() {
        return stateID;
    }

    public String getDom() {
        return dom;
    }

	public boolean getShouldCheckFormFillingResult() {
		return shouldCheckFormFillingResult;
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

