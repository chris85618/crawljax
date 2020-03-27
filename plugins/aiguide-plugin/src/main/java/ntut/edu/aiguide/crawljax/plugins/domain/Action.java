package ntut.edu.aiguide.crawljax.plugins.domain;

import com.crawljax.core.CandidateElement;

import java.util.List;

public class Action {
    private final String actionXpath;
    private final String value;

    public Action(String actionXpath, String value) {
        this.actionXpath = actionXpath;
        this.value = value;
    }

    public String getActionXpath() {
        return actionXpath;
    }

    public String getValue() {
        return value;
    }
}
