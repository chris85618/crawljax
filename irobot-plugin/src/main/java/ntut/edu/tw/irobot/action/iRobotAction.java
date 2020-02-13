package ntut.edu.tw.irobot.action;

import com.crawljax.core.CandidateElement;

public class iRobotAction implements Action {
    private final CandidateElement source;

    public iRobotAction(CandidateElement action) {
        source = action;
    }

    @Override
    public String getName() {

        return source.getUniqueString();
    }

    @Override
    public String getType() {
        return source.getElement().getTagName();
    }

    @Override
    public String getXpath() { return source.getIdentification().getValue(); }

    @Override
    public String getValue() {
        return source.getValue();
    }

    @Override
    public Object getSource() {
        return source;
    }

    @Override
    public String toString(){
        return source.toString();
    }

    @Override
    public boolean equals(Object that) {
        return this.source.getUniqueString()
                          .equals(((iRobotAction) that).source.getUniqueString());
    }
}
