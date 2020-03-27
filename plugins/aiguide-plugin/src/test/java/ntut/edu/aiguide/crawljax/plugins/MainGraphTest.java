package ntut.edu.aiguide.crawljax.plugins;

import com.crawljax.core.ExitNotifier;
import com.crawljax.core.state.*;
import org.junit.Before;
import org.junit.Test;
import sun.applet.Main;

import static org.junit.Assert.*;

public class MainGraphTest {
    private StateVertexFactory stateVertexFactory = new DefaultStateVertexFactory();
    private InMemoryStateFlowGraph graph_1 = new InMemoryStateFlowGraph(new ExitNotifier(0), stateVertexFactory);
    private InMemoryStateFlowGraph graph_2 = new InMemoryStateFlowGraph(new ExitNotifier(0), stateVertexFactory);


    @Before
    public void setUp() {
        createBasicGraph(graph_1);
        createBasicGraph(graph_2);
    }

    /**
     *           state2
     *        /
     *  Index
     *        ＼
     *           state1
     *                  ＼
     *                     state3
     */
    private void createBasicGraph(InMemoryStateFlowGraph inMemoryStateFlowGraph) {
        StateVertex index = stateVertexFactory.createIndex("", "0", "0");
        StateVertex state1 = stateVertexFactory.newStateVertex(1, "", "state1", "1", "1");
        StateVertex state2 = stateVertexFactory.newStateVertex(2, "", "state2", "2", "2");
        StateVertex state3 = stateVertexFactory.newStateVertex(3, "", "state3", "3", "3");
        inMemoryStateFlowGraph.putIndex(index);
        inMemoryStateFlowGraph.putIfAbsent(state1);
        inMemoryStateFlowGraph.putIfAbsent(state2);
        inMemoryStateFlowGraph.putIfAbsent(state3);
        inMemoryStateFlowGraph.addEdge(index, state1, new Eventable());
        inMemoryStateFlowGraph.addEdge(index, state2, new Eventable());
        inMemoryStateFlowGraph.addEdge(state1, state3, new Eventable());
    }

    @Test
    public void mergingGraph() {
        StateVertex state4 = stateVertexFactory.newStateVertex(4, "", "state4", "4", "4");
        assertNull(graph_1.putIfAbsent(state4));
        assertNull(graph_2.putIfAbsent(state4));
        graph_1.addEdge(graph_1.getById(2), state4, new Eventable());
        graph_2.addEdge(graph_2.getById(1), state4, new Eventable());

        MainGraph mainGraph = new MainGraph(graph_1);
        mainGraph.mergingGraph(graph_2);
        StateFlowGraph stateFlowGraph = mainGraph.getMainGraph();
        assertEquals(6, stateFlowGraph.getAllStates().size());

    }
}