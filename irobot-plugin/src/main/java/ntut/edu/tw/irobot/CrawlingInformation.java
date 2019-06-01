package ntut.edu.tw.irobot;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.CandidateElement;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;


public class CrawlingInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingInformation.class);

    private WebSnapShot currentWebSnapShot;
    private BlockingQueue<WebSnapShot> webSnapShotBlockingQueue = new ArrayBlockingQueue<>(1);

    private CandidateElement targetAction;
    private String targetValue;

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
            this.currentWebSnapShot = webSnapShotBlockingQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setWebSnapShot(WebSnapShot webSnapShot) {
        try {
            webSnapShotBlockingQueue.put(webSnapShot);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        resetInformation();
    }
}
