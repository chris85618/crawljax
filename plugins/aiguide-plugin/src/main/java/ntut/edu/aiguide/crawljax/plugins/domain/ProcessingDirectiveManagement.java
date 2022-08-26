package ntut.edu.aiguide.crawljax.plugins.domain;

import com.crawljax.core.state.StateVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProcessingDirectiveManagement {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingDirectiveManagement.class);
    private State firstDirectiveState;
    private State targetDirectiveState;
    private State lastTargetDirectiveState = null;
    private Stack<State> directiveStack;
    private Stack<State> processedDirectiveStack = new Stack<>();
    private Map<String, LinkedList<String>> directiveAppendStateNameMap = new HashMap<>();
    private HashSet<StateVertex> processingState = new HashSet<>();
    private EditDistanceComparator editDistanceComparator;

    public ProcessingDirectiveManagement(Stack<State> directivePath) {
        directiveStack = directivePath;
        // put the first directive
        firstDirectiveState = getFirstDirectiveState();
        targetDirectiveState = firstDirectiveState;
        editDistanceComparator = new EditDistanceComparator(0.98D);
    }

    private State getFirstDirectiveState() {
        if (!directiveStack.isEmpty()) {
            return directiveStack.pop();
        }
        return null;
    }

    public boolean isAllDirectiveIsProcessed() {
        return directiveStack.isEmpty() && targetDirectiveState == null;
    }

    public boolean isCurrentStateIsDirective(String dom) {
        if (targetDirectiveState == null)
            return false;
        String currentDomHash = String.valueOf(dom.hashCode());
        if (currentDomHash.equalsIgnoreCase(targetDirectiveState.getID()) || isSimilarDom(targetDirectiveState.getDom(), dom)) {
            LOGGER.debug("Current state is same as directive {}", targetDirectiveState);
            lastTargetDirectiveState = targetDirectiveState;
            targetDirectiveState = getNextDirectiveState();
            LOGGER.debug("lastTargetDirectiveState is {}", lastTargetDirectiveState);
            LOGGER.debug("targetDirectiveState is {}", targetDirectiveState);

            return true;
        }
        return false;
    }

    private boolean isSimilarDom(String originalDom, String newDom) {
        return editDistanceComparator.isEquivalent(originalDom, newDom);
    }

    private State getNextDirectiveState() {
        if (!directiveStack.isEmpty()) {
            State state = directiveStack.pop();
            processedDirectiveStack.push(state);
            return state;
        }
        return null;
    }

    public void recordCurrentState(StateVertex currentState) {
        LinkedList<String> processingStateName = directiveAppendStateNameMap.get(lastTargetDirectiveState.getID());
        if (processingStateName == null) {
            processingStateName = new LinkedList<>();
            processingStateName.push(null);
            directiveAppendStateNameMap.put(lastTargetDirectiveState.getID(), processingStateName);
        }
        processingState.add(currentState);
        processingStateName.addFirst(currentState.getName());
    }

    public void removeLastStateInRecordList() {
        LinkedList<String> processingStateName = directiveAppendStateNameMap.get(lastTargetDirectiveState.getID());
        if (processingStateName == null)
            throw new RuntimeException("Something wrong when get the process directive...");
        processingStateName.removeFirst();
    }

    public String getAppendStateName() {
        LinkedList<String> processingDirective = directiveAppendStateNameMap.get(lastTargetDirectiveState.getID());
        if (processingDirective == null) {
            LOGGER.info("Get the append name from {}, is null...", lastTargetDirectiveState.getID());
            return null;
        }
        String stateName = processingDirective.pollFirst();
        processingDirective.addLast(stateName);
        LOGGER.info("Get the append name from {}, is {}", lastTargetDirectiveState.getID(), stateName);
        return stateName;
    }

    public List<Action> getProcessingStateNextActionSet() {
        return lastTargetDirectiveState.getNextActionSet();
    }

    public boolean isProcessingStateHasNextActionSet() {
        return lastTargetDirectiveState.hasNextActionSet();
    }

    public List<Action> getProcessingStateLastActionSet() {
        return lastTargetDirectiveState.getLastActionSet();
    }

    public boolean isCurrentStateIsProcessingState(StateVertex currentState) {
        return processingState.contains(currentState);
    }

    public void printRetainDirectives() {
        if (directiveStack.isEmpty()) {
            System.out.println("There is no input page in this round");
            LOGGER.info("There is no input page in this round");
        } else {
            System.out.println(String.format("Retaining directives size is %s", directiveStack.size()));
            LOGGER.info("There is something wrong when running this round, retaining directives size is {}", directiveStack.size());
        }
    }
}
