package ntut.edu.tw.irobot.interaction;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;

public interface CrawlerInteractor {
    /**
     * This step will convert the candidateElement to iRobot {@link ntut.edu.tw.irobot.action.Action}
     *
     * @param candidateElements
     *              All elements from the current page
     */
    public void convertToRobotAction (ImmutableList<CandidateElement> candidateElements);

    /**
     * This step will convert the StateVertex to iRobotState
     *
     * @param state
     *              The current State
     */
    public void convertToRobotState (StateVertex state);

    /**
     * @return the restart signal.
     */
    public boolean isRestart();

    /**
     * @return The target action which has been transfer to {@link CandidateElement}
     */
    public CandidateElement getTargetElment();

    /**
     * @return The type of the target element
     */
    public String getTargetElementType();

    /**
     * @return The target xpath
     */
    public String getTargetXpath();


    /**
     * @return The target value which robot gave
     */
    public String getTargetValue();

    /**
     * Set the signal to default value
     */
    public void setToDefaultSignal();

    /**
     * This step will set the signal that execute action fail or not
     *
     * @param failSignal
     *          The failure signal you want to set
     */
    public void setExecuteActionFailureSignal(boolean failSignal);
}
