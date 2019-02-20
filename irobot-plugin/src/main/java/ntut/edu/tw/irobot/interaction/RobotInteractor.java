package ntut.edu.tw.irobot.interaction;

import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;

public interface RobotInteractor {
    /**
     * @return All actions from current page
     */
    public ImmutableList<Action> getActions();

    /**
     * @return the currentState
     */
    public State getState();

    /**
     * This step will convert Action to CandidateElement
     *
     * @param action
     *          The target action which iRobot assigned
     * @param value
     *          The target value which iRobot assigned
     */
    public void setTargetAction(Action action, String value);

    /**
     * @param restartSignal
     *          The restart signal which iRobot gave.
     */
    public void setRestartSignal(boolean restartSignal);

    /**
     * This step will set the signal to tell the crawler stop crawling
     *
     * @param terminateSignal
     *          The terminate signal you want to set
     */
    public void setTerminateSignal(boolean terminateSignal);

    /**
     * @return
     *          The signal that the action is execute successful
     */
    public boolean isExecuteSuccess();
}
