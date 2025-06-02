package ntut.edu.crawlguider.crawljax.plugins.domain;

import java.util.ArrayList;
import java.util.List;

public class LearningTarget {

    private final String dom;
    private final String targetURL;
    private final List<String> formXPaths;
    private final List<List<Action>> actionSequence;

    public LearningTarget(String dom, String targetURL, List<String> formXPaths, List<List<Action>> actionSet) {
        this.dom = dom;
        this.targetURL = targetURL;
        this.formXPaths = new ArrayList<>(formXPaths);
        this.actionSequence = actionSet;
    }

    public String getDom() {
        return dom;
    }

    public String getTargetURL() {
        return targetURL;
    }

    public List<String> getFormXPaths() {
        return formXPaths;
    }

    public List<List<Action>> getActionSequence() {
        return actionSequence;
    }
}
