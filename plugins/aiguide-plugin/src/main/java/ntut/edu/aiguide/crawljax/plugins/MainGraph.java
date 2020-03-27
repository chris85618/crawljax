package ntut.edu.aiguide.crawljax.plugins;

import com.crawljax.core.state.*;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MainGraph {
    private InMemoryStateFlowGraph mainGraph;
    private final AtomicInteger nextStateNameCounter = new AtomicInteger();
    private final Map<StateVertex, StateVertex> correspondState = new HashMap<>();

    public MainGraph(InMemoryStateFlowGraph stateFlowGraph) {
        this.mainGraph = stateFlowGraph;
        nextStateNameCounter.set(mainGraph.getAllStates().size());
    }

    public void mergingGraph(StateFlowGraph stateFlowGraph) {
        putAllStateInCurrentStateFlowGraphToMainGraph(stateFlowGraph);
        createAllEdgesInMainGraph(stateFlowGraph.getAllEdges());
    }

    private void putAllStateInCurrentStateFlowGraphToMainGraph(StateFlowGraph stateFlowGraph) {
        StateVertexFactory stateVertexFactory = new DefaultStateVertexFactory();
        for (StateVertex oldState : stateFlowGraph.getAllStates()) {
            if (isStateInMainGraph(oldState))
                continue;
            StateVertex newState = stateVertexFactory.newStateVertex(nextStateNameCounter.incrementAndGet(),
                    oldState.getUrl(),
                    "state" + nextStateNameCounter.get(),
                    oldState.getDom(),
                    oldState.getStrippedDom());
            StateVertex cloneState = mainGraph.putIfAbsent(newState);
            if (cloneState != null) // is a clone state
                correspondState.put(oldState, cloneState);
            else // is a new state
                correspondState.put(oldState, newState);
        }
    }

    private void createAllEdgesInMainGraph(ImmutableSet<Eventable> unMergeGraphEdges) {
        ImmutableSet<Eventable> mainGraphEdges = mainGraph.getAllEdges();
        for (Eventable event : unMergeGraphEdges) {
            if (isSourceStateOrTargetStateInCorrespondState(event.getSourceStateVertex(), event.getTargetStateVertex())) {
                StateVertex source = findCorrespondStateInMainGraph(event.getSourceStateVertex());
                StateVertex target = findCorrespondStateInMainGraph(event.getTargetStateVertex());
                addEdgeToMainGraph(source, target, event);
            }
        }
    }

    private boolean isSourceStateOrTargetStateInCorrespondState(StateVertex source, StateVertex target) {
        return correspondState.get(source) != null || correspondState.get(target) != null;
    }

    private StateVertex findCorrespondStateInMainGraph(StateVertex targetState) {
        StateVertex result = mainGraph.putIfAbsent(targetState);
        if (result == null)
            throw new RuntimeException("There something wrong when merging graph");
        return result;
    }

    private void addEdgeToMainGraph(StateVertex source, StateVertex target, Eventable event) {
        mainGraph.addEdge(source, target, event);
    }



    private boolean isStateInMainGraph(StateVertex stateVertex) {
        StateVertex foundState = mainGraph.getById(stateVertex.getId());
        if (foundState != null)
            return foundState.equals(stateVertex);
        return false;
    }

    public StateFlowGraph getMainGraph() {
        return mainGraph;
    }

}
