package ntut.edu.tw.irobot;

import com.crawljax.core.CandidateElement;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;

public interface Information {

    CandidateElement getTargetElement();

    String getTargetElementType();

    String getTargetXpath();

    String getTargetValue();

    void setWebSnapShot(WebSnapShot webSnapShot);

    ImmutableList<Action> getActions();

    State getState();

    void setTargetAction(Action action, String value);

    void setRestartSignal(boolean restartSignal);

    boolean isExecuteSuccess();

    boolean isRestart();


    void setExecuteSignal(boolean successOrNot);

    void resetData();
}
