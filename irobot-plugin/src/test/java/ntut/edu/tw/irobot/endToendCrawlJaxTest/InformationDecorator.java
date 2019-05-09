package ntut.edu.tw.irobot.endToendCrawlJaxTest;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.CrawlingInformation;
import ntut.edu.tw.irobot.WebSnapShot;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;

import java.util.ArrayList;

public class InformationDecorator extends CrawlingInformation {
    private CrawlingInformation infoDecorator;
    public ArrayList<String> threadSequence = new ArrayList<>();

    public InformationDecorator(CrawlingInformation crawlingInformation) {
        this.infoDecorator = crawlingInformation;
    }

    public ArrayList<String> getThreadSequence() {
        return threadSequence;
    }


    @Override
    public void setWebSnapShot(WebSnapShot webSnapShot) {
        this.infoDecorator.setWebSnapShot(webSnapShot);
        this.threadSequence.add("CrawlJax:setState");
        this.threadSequence.add("CrawlJax:setAction");
    }

    @Override
    public void waitForCurrentWebSnapShot() {
        this.infoDecorator.waitForCurrentWebSnapShot();
    }

    @Override
    public WebSnapShot getCurrentWebSnapShot() {
        WebSnapShot webSnapShot = this.infoDecorator.getCurrentWebSnapShot();
        this.threadSequence.add("Robot: getActions");
        this.threadSequence.add("Robot: getState");
        return webSnapShot;
    }

    /**
     * @return actions
     *              All actions from current page
     */
    @Override
    public ImmutableList<Action> getActions() {
        return this.infoDecorator.getActions();
    }


    /**
     * @return the currentState
     */
    @Override
    public State getState() {
        return this.infoDecorator.getState();
    }

    /**
     * This step will convert Action to CandidateElement
     *
     * @param action
     *          The target action which iRobot assigned
     */
    @Override
    public void setTargetAction(Action action, String value) {
        this.infoDecorator.setTargetAction(action, value);
    }

    /**
     * @return targetAction
     *          The target action which has been transfer to {@link com.crawljax.core.CandidateElement}
     */
    @Override
    public CandidateElement getTargetElement() {
        return this.infoDecorator.getTargetElement();
    }

    /**
     * @param restartSignal
     *          The restart signal which iRobot gave.
     */
    @Override
    public void setRestartSignal(boolean restartSignal) {
        this.infoDecorator.setRestartSignal(restartSignal);
    }


    /**
     * @return the target element type. ex. input, a, button
     */
    @Override
    public String getTargetElementType() {
        return this.infoDecorator.getTargetElementType();
    }

    /**
     * @return the target element xpath.
     */
    @Override
    public String getTargetXpath() {
        return this.infoDecorator.getTargetXpath();
    }

    /**
     * @return the value which iRobot gave.
     */
    @Override
    public String getTargetValue() {
        return this.infoDecorator.getTargetValue();
    }

    /**
     * @return the boolean that the target action is execute success or not.
     */
    @Override
    public boolean isExecuteSuccess() {
        return this.infoDecorator.isExecuteSuccess();
    }

    /**
     * @return the restart signal.
     */
    @Override
    public boolean isRestart() {
        return this.infoDecorator.isRestart();
    }


    /**
     * @param successOrNot
     *          The execute success signal which crawler gave.
     */
    @Override
    public void setExecuteSignal(boolean successOrNot) {
        this.infoDecorator.setExecuteSignal(successOrNot);
    }

    /**
     * Reset the data
     */
    @Override
    public void resetData() {
        this.infoDecorator.resetData();
    }
}
