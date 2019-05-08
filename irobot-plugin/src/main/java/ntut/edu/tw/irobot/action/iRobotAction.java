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
    public Object getSource() {
        return source;
    }

    @Override
    public String toString(){
        return source.toString();
    }
}
