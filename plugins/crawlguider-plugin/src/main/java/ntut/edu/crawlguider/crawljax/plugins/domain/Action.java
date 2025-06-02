package ntut.edu.crawlguider.crawljax.plugins.domain;

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

    @Override
    public String toString() {
        return "Action{Xpath = " + actionXpath + ", value = " + value + "}";
    }
}
