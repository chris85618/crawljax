package ntut.edu.aiguide.crawljax.plugins.domain;

import com.crawljax.core.state.StateVertex;

import java.util.*;

public class ProcessingDirectiveManagement {
    private String firstDirectiveID;
    private String secondDirectiveID;
    private String targetDirectiveID;
    private String lastTargetDirectiveID = "";
    private Stack<State> directiveStack;
    private Stack<State> processedDirectiveStack = new Stack<>();
    private Map<String, LinkedList<String>> directiveAppendStateNameMap = new HashMap<>();

    public ProcessingDirectiveManagement(Stack<State> directivePath) {
        directiveStack = directivePath;
        // put the first directive
        firstDirectiveID = getFirstDirectiveID();
        secondDirectiveID = getNextDirectiveID();
        targetDirectiveID = firstDirectiveID;
    }

    private String getFirstDirectiveID() {
        if (!directiveStack.isEmpty()) {
            State firstDirective = directiveStack.pop();
            return firstDirective.getID();
        }
        return "";
    }


    public boolean isCurrentStateIsDirective(String dom) {
        String currentDomHash = String.valueOf(dom.hashCode());
        if (currentDomHash.equalsIgnoreCase(targetDirectiveID)) {
            lastTargetDirectiveID = targetDirectiveID;
            targetDirectiveID = getNextDirectiveID();
            return true;
        }
        else if (targetDirectiveID.equalsIgnoreCase(firstDirectiveID) && currentDomHash.equalsIgnoreCase(secondDirectiveID)) {
            lastTargetDirectiveID = secondDirectiveID;
            targetDirectiveID = getNextDirectiveID();
            return true;
        }
        return false;
    }

    private String getNextDirectiveID() {
        if (!directiveStack.isEmpty()) {
            State state = directiveStack.pop();
            processedDirectiveStack.push(state);
            return state.getID();
        }
        return "";
    }

    public void recordCurrentState(State targetState, StateVertex currentState) {
        LinkedList<String> processingStateName = directiveAppendStateNameMap.get(targetState.getID());
        if (processingStateName == null) {
            processingStateName = new LinkedList<>();
            processingStateName.push(null);
            directiveAppendStateNameMap.put(targetState.getID(), processingStateName);
        }
        processingStateName.addFirst(currentState.getName());
    }

    public void removeLastStateInRecordList(State targetState) {
        LinkedList<String> processingStateName = directiveAppendStateNameMap.get(targetState.getID());
        if (processingStateName == null)
            throw new RuntimeException("Something wrong when get the process directive...");
        processingStateName.removeFirst();
    }

    public String getAppendStateName() {
        LinkedList<String> processingDirective = directiveAppendStateNameMap.get(lastTargetDirectiveID);
        if (processingDirective == null)
            return null;
        String stateName = processingDirective.pollFirst();
        processingDirective.addLast(stateName);
        return stateName;
    }

    public void resetTargetDirective() {
        targetDirectiveID = this.firstDirectiveID;
        lastTargetDirectiveID = "";
        while (!processedDirectiveStack.isEmpty()) {
            directiveStack.push(processedDirectiveStack.pop());
        }

        for (LinkedList<String> stateNameList : directiveAppendStateNameMap.values()) {
            if (stateNameList.getLast() != null) {
                while (true) {
                    String stateName = stateNameList.pollFirst();
                    if (stateName == null) {
                        stateNameList.addLast(stateName);
                        break;
                    }
                    stateNameList.addLast(stateName);
                }
            }
        }
    }
}
