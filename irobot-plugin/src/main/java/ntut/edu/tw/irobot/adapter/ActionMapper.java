package ntut.edu.tw.irobot.adapter;

import com.crawljax.core.CandidateElement;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.action.iRobotAction;

import java.util.ArrayList;

public class ActionMapper {
    public Action mappingFrom(CandidateElement candidateElement) {
        return new iRobotAction(candidateElement);
    }

    public ImmutableList<Action>
    mappingImmutableActionFrom(
            ImmutableList<CandidateElement> candidateElements
    ) {

        ArrayList<Action> actions = new ArrayList<Action>();
        for (int i = 0; i < candidateElements.size(); i++) {
            CandidateElement candidateElement = candidateElements.get(i);
            Action action = mappingFrom(candidateElement);
            actions.add(action);
        }
        return ImmutableList.copyOf(actions);
    }

}
