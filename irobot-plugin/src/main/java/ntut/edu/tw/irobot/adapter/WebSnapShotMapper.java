package ntut.edu.tw.irobot.adapter;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.WebSnapShot;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;

public class WebSnapShotMapper {
    public WebSnapShot
    mappingFrom(
            ImmutableList<CandidateElement> candidateElements
            , StateVertex vertex) {
        ImmutableList<Action> actions = new ActionMapper()
                .mappingImmutableActionFrom(candidateElements);
        State state = new StateMapper().mappingFrom(vertex);
        return new WebSnapShot(actions, state);
    }
}

