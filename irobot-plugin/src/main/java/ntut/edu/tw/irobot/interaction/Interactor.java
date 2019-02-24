package ntut.edu.tw.irobot.interaction;


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


public class Interactor implements CrawlerInteractor, RobotInteractor{
    private static final Logger LOGGER = LoggerFactory.getLogger(Interactor.class);

    private State currentState;
    private ImmutableList<Action> actions;

    private CandidateElement targetAction;
    private boolean restartSignal;
    private boolean terminate;
    private boolean executeActionFailure;
    private String targetValue;


    public Interactor() {
        this.currentState = null;
        this.targetAction = null;
        this.targetValue = "";
        this.terminate = false;
        this.restartSignal = false;
        this.executeActionFailure = false;
        this.actions = ImmutableList.of();
    }

    @Override
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
    @Override
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
    @Override
    public void convertToRobotState (StateVertex state) {
        LOGGER.info("Convert StateVertex to iRobotState...");
        currentState = new iRobotState(state);
    }

    /**
     * @return the currentState
     */
    @Override
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
    @Override
    public void setTargetAction(Action action, String value) {
        LOGGER.info("Get the target Action({}) from robot, transform it... ", action);
        targetAction = (CandidateElement) action.getSource();
    }

    /**
     * @return targetAction
     *          The target action which has been transfer to {@link com.crawljax.core.CandidateElement}
     */
    @Override
    public CandidateElement getTargetElment() {
        LOGGER.info("Get the target element...");
        return targetAction;
    }

    /**
     * @param restartSignal
     *          The restart signal which iRobot gave.
     */
    @Override
    public void setRestartSignal(boolean restartSignal) {
        LOGGER.info("Setting the restart signal : {}" + restartSignal);
        this.restartSignal = restartSignal;
    }

    @Override
    public void setTerminateSignal(boolean terminateSignal) {
        LOGGER.info("Setting the terminate signal : {}" + terminateSignal);
        this.terminate = terminateSignal;
    }

    @Override
    public boolean isExecuteSuccess() {
        LOGGER.info("Get the execute failure signal : {}" + executeActionFailure);
        return executeActionFailure;
    }

    /**
     * @return the restart signal.
     */
    @Override
    public boolean isRestart() {
        LOGGER.info("Get the restart signal : {}" + restartSignal);
        return restartSignal;
    }

    @Override
    public String getTargetElementType() {
        LOGGER.info("Get the target element type : {}" + targetAction.getElement().getTagName());
        return targetAction.getElement().getTagName();
    }

    @Override
    public String getTargetXpath() {
        LOGGER.info("Get the target element xpath : {}" + targetAction.getIdentification().getValue());
        return targetAction.getIdentification().getValue();
    }

    @Override
    public String getTargetValue() {
        LOGGER.info("Get the target value : {}" + targetValue);
        return targetValue;
    }

    @Override
    public void setToDefaultSignal() {
        LOGGER.info("Setting the signal to default...");
        restartSignal = false;
        terminate = false;
    }

    @Override
    public void setExecuteActionFailureSignal(boolean failSignal) {
        LOGGER.info("Setting the execute failure signal : {}" + failSignal);
        executeActionFailure = failSignal;
    }

}
