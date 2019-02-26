package ntut.edu.tw.irobot.crawldata;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.action.iRobotAction;
import ntut.edu.tw.irobot.state.iRobotState;
import ntut.edu.tw.irobot.state.State;


public class CrawlingData implements Cloneable{
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingData.class);

    private State currentState;
    private ImmutableList<Action> actions;

    private CandidateElement targetAction;
    private boolean restartSignal;
    private boolean terminate;
    private boolean executeActionFailure;
    private String targetValue;


    public CrawlingData() {
        this.currentState = null;
        this.targetAction = null;
        this.targetValue = "";
        this.terminate = false;
        this.restartSignal = false;
        this.executeActionFailure = false;
        this.actions = ImmutableList.of();
    }

    public void convertToRobotAction (ImmutableList<CandidateElement> candidateElements) {
        LOGGER.info("Convert CadidateElements to iRobotActions...");
        List<Action> transformActions = new ArrayList<Action>();
        LOGGER.info("Transfer Ready, CandidateElements have {} items.", candidateElements.size());

        for (CandidateElement element : candidateElements) {
            LOGGER.info("Transfer CandidateAction: {}, to iRobotAction...", element);
            transformActions.add(new iRobotAction(element));
        }

        actions = ImmutableList.copyOf(transformActions);
        LOGGER.info("Transfer complete, iRobotActions have {} items.", transformActions.size());
    }

    /**
     * @return actions
     *              All actions from current page
     */
    public ImmutableList<Action> getActions() {
        LOGGER.info("Get the Action...");
        return actions;
    }

    /**
     * This step will convert the StateVertex to iRobotState
     *
     * @param state
     *              The current State
     */
    public void convertToRobotState (StateVertex state) {
        LOGGER.info("Convert StateVertex to iRobotState...");
        currentState = new iRobotState(state);
    }

    /**
     * @return the currentState
     */
    public State getState() {
        LOGGER.info("Get the State...");
        return currentState;
    }

    /**
     * This step will convert Action to CandidateElement
     *
     * @param action
     *          The target action which iRobot assigned
     */
    public void setTargetAction(Action action, String value) {
        LOGGER.info("Get the target Action({}) from robot, transform it... ", action);
        targetAction = (CandidateElement) action.getSource();
        targetValue = value;
    }

    /**
     * @return targetAction
     *          The target action which has been transfer to {@link com.crawljax.core.CandidateElement}
     */
    public CandidateElement getTargetElment() {
        LOGGER.info("Get the target element...");
        return targetAction;
    }

    /**
     * @param restartSignal
     *          The restart signal which iRobot gave.
     */
    public void setRestartSignal(boolean restartSignal) {
        LOGGER.info("Setting the restart signal : {}", restartSignal);
        this.restartSignal = restartSignal;
    }

    public void setTerminateSignal(boolean terminateSignal) {
        LOGGER.info("Setting the terminate signal : {}", terminateSignal);
        this.terminate = terminateSignal;
    }

    public boolean isExecuteSuccess() {
        LOGGER.info("Get the execute failure signal : {}", executeActionFailure);
        return executeActionFailure;
    }

    /**
     * @return the restart signal.
     */
    public boolean isRestart() {
        LOGGER.info("Get the restart signal : {}", restartSignal);
        return restartSignal;
    }

    public String getTargetElementType() {
        LOGGER.info("Get the target element type : {}", targetAction.getElement().getTagName());
        return targetAction.getElement().getTagName();
    }

    public String getTargetXpath() {
        LOGGER.info("Get the target element xpath : {}", targetAction.getIdentification().getValue());
        return targetAction.getIdentification().getValue();
    }

    public String getTargetValue() {
        LOGGER.info("Get the target value : {}", targetValue);
        return targetValue;
    }

    public void setExecuteActionSignal(boolean successOrNot) {
        LOGGER.info("Setting the execute signal : {}", successOrNot);
        executeActionFailure = successOrNot;
    }

    public void resetData() {
        LOGGER.info("Resetting data....");
        terminate = false;
        restartSignal = false;
        executeActionFailure = false;

    }

    @Override
    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }
}
