package ntut.edu.aiguide.crawljax.plugins.domain;

import com.crawljax.core.state.StateVertex;

import java.util.List;

public class InputPage {

    private final StateVertex stateVertex;
    private final List<String> formXPaths;

    public InputPage(StateVertex stateVertex, List<String> formXPaths) {
        this.stateVertex = stateVertex;
        this.formXPaths = formXPaths;
    }

    public StateVertex getStateVertex() {
        return stateVertex;
    }

    public List<String> getFormXPaths() {
        return formXPaths;
    }
}
