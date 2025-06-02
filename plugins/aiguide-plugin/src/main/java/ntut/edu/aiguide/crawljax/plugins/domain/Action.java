package ntut.edu.aiguide.crawljax.plugins.domain;

public class Action {
    public enum DomRecordAction {
        NONE, RECORD_BEFORE, RECORD_AFTER
    }

    private final String actionXpath;
    private final String value;
    private DomRecordAction domRecordAction;

    public Action(String actionXpath, String value) {
        this(actionXpath, value, DomRecordAction.NONE);
    }

    public Action(String actionXpath, String value, DomRecordAction domRecordAction) {
        this.actionXpath = actionXpath;
        this.value = value;
        this.domRecordAction = domRecordAction;
    }

    public String getActionXpath() {
        return actionXpath;
    }

    public String getValue() {
        return value;
    }

    public DomRecordAction getDomRecordAction() {
        return domRecordAction;
    }

    public void setDomRecordAction(DomRecordAction domRecordAction) {
        this.domRecordAction = domRecordAction;
    }

    @Override
    public String toString() {
        return "Action{Xpath = " + actionXpath + ", value = " + value + "}";
    }
}
