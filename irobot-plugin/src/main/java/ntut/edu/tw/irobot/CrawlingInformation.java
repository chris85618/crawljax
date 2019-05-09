package ntut.edu.tw.irobot;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.CandidateElement;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;


public class CrawlingInformation implements Information {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingInformation.class);

    private WebSnapShot currentWebSnapShot;
    private BlockingQueue<WebSnapShot> webSnapShotBlockingQueue = new ArrayBlockingQueue<>(1);

    private CandidateElement targetAction;
    private String targetValue;

    private Action currentTargetAction;

    private boolean restartSignal;
    private boolean executeActionSuccessOrNot;

    public CrawlingInformation() {
        resetInformation();
    }

    private void resetInformation() {
        this.targetAction = null;
        this.targetValue = "";
        this.restartSignal = false;
        this.executeActionSuccessOrNot = false;
    }

    public WebSnapShot getCurrentWebSnapShot() {
        return currentWebSnapShot;
    }

    public void waitForCurrentWebSnapShot() {

        try {
            System.out.println("Getting websnapshot");
            System.out.println(webSnapShotBlockingQueue.size());
            this.currentWebSnapShot = webSnapShotBlockingQueue.take();
            System.out.println("Done");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setWebSnapShot(WebSnapShot webSnapShot) {
        try {
            System.out.println("setting websnapshot");

            webSnapShotBlockingQueue.put(webSnapShot);
//            webSnapShotBlockingQueue.notify();

            System.out.println("setting websnapshot Done");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ImmutableList<Action> getActions() {
        LOGGER.info("Get the Action...");
        return this.currentWebSnapShot.getActions();
    }

    /**
     * @return the currentState
     */
    @Override
    public State getState() {
        LOGGER.info("Get the State...");
        return this.currentWebSnapShot.getState();
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
        targetValue = value;
        this.currentTargetAction = action;
    }

    /**
     * @return targetAction
     *          The target action which has been transfer to {@link com.crawljax.core.CandidateElement}
     */
    @Override
    public CandidateElement getTargetElement() {
        LOGGER.info("Get the target element...");
        return targetAction;
    }




    /**
     * @param restartSignal
     *          The restart signal which iRobot gave.
     */
    @Override
    public void setRestartSignal(boolean restartSignal) {
        LOGGER.info("Setting the restart signal : {}", restartSignal);
        this.restartSignal = restartSignal;
    }

    /**
     * @return the boolean that the target action is execute success or not.
     */
    @Override
    public boolean isExecuteSuccess() {
        LOGGER.info("Get the execute execute signal : {}", executeActionSuccessOrNot);
        return executeActionSuccessOrNot;
    }

    /**
     * @return the restart signal.
     */
    @Override
    public boolean isRestart() {
        LOGGER.info("Get the restart signal : {}", restartSignal);
        return restartSignal;
    }

    /**
     * @return the target element type. ex. input, a, button
     */
    @Override
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
    @Override
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
    @Override
    public String getTargetValue() {
        LOGGER.info("Get the target value : {}", targetValue);
        return targetValue;
    }

    /**
     * @param successOrNot
     *          The execute success signal which crawler gave.
     */
    @Override
    public void setExecuteSignal(boolean successOrNot) {
        LOGGER.info("Setting the execute signal : {}", successOrNot);
        executeActionSuccessOrNot = successOrNot;
    }

    /**
     * Reset the data
     */
    @Override
    public void resetData() {
        LOGGER.info("Resetting data....");
        resetInformation();
    }

    public Action getTargetAction() {
        return this.currentTargetAction;
    }
}
