package ntut.edu.aiguide.crawljax.plugins.mockObject;

import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.StateVertexFactory;
import com.crawljax.core.state.StateVertexImpl;

public class StateVertexFactoryForTest extends StateVertexFactory {
    @Override
    public StateVertex newStateVertex(int id, String url, String name, String dom, String strippedDom) {
        return new StateVertexImpl(id, url, name, dom, strippedDom);
    }
}
