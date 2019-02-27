package ntut.edu.tw.irobot;


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


public class CrawlingInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingInformation.class);

    private State currentState;
    private ImmutableList<Action> actions;

    private CandidateElement targetAction;
    private boolean restartSignal;
    private boolean terminate;
    private boolean executeActionSuccessOrNot;
    private String targetValue;


    public CrawlingInformation() {
        defaultValue();
    }

    private void defaultValue() {
        this.currentState = null;
        this.targetAction = null;
        this.targetValue = "";
        this.terminate = false;
        this.restartSignal = false;
        this.executeActionSuccessOrNot = false;
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
    public CandidateElement getTargetElement() {
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

    /**
     * @param terminateSignal
     *          The terminate signal which iRobot gave.
     */
    public void setTerminateSignal(boolean terminateSignal) {
        LOGGER.info("Setting the terminate signal : {}", terminateSignal);
        this.terminate = terminateSignal;
    }

    /**
     * @return the boolean that the target action is execute success or not.
     */
    public boolean isExecuteSuccess() {
        LOGGER.info("Get the execute execute signal : {}", executeActionSuccessOrNot);
        return executeActionSuccessOrNot;
    }

    /**
     * @return the restart signal.
     */
    public boolean isRestart() {
        LOGGER.info("Get the restart signal : {}", restartSignal);
        return restartSignal;
    }

    /**
     * @return the target element type. ex. input, a, button
     */
    public String getTargetElementType() {
        if (targetAction != null){
            LOGGER.info("Get the target element type : {}", targetAction.getElement().getTagName());
            return targetAction.getElement().getTagName();
        }

        LOGGER.info("The target element is null, return empty string");
        return "";
    }

    /**
     * @return the target element xpath.
     */
    public String getTargetXpath() {
        if (targetAction != null){
            LOGGER.info("Get the target element xpath : {}", targetAction.getIdentification().getValue());
            return targetAction.getIdentification().getValue();
        }

        LOGGER.info("The target element is null, return empty string");
        return "";
    }

    /**
     * @return the value which iRobot gave.
     */
    public String getTargetValue() {
        LOGGER.info("Get the target value : {}", targetValue);
        return targetValue;
    }

    /**
     * @param successOrNot
     *          The execute success signal which crawler gave.
     */
    public void setExecuteSignal(boolean successOrNot) {
        LOGGER.info("Setting the execute signal : {}", successOrNot);
        executeActionSuccessOrNot = successOrNot;
    }

    /**
     * Reset the data
     */
    public void resetData() {
        LOGGER.info("Resetting data....");
        defaultValue();
    }
}
