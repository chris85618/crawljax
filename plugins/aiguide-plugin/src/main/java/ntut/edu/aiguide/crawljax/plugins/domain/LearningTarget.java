package ntut.edu.aiguide.crawljax.plugins.domain;

import java.util.ArrayList;
import java.util.List;

public class LearningTarget {

    private final String dom;
    private final String targetURL;
    private final List<String> formXPaths;
    private final List<HighLevelAction> actionSequence;

    public LearningTarget(String dom, String targetURL, List<String> formXPaths, List<HighLevelAction> actionSet) {
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
        final List<List<Action>> result = new ArrayList<>();
        for (HighLevelAction highLevelAction : this.actionSequence) {
            result.add(highLevelAction.getActionSequence());
        }
        return result;
    }
}
