package ntut.edu.tw.irobot;

import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitingLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingLock.class);
    private ImmutableList<Action> actions;


    private boolean locker = false;


    public WaitingLock() {

    }

    public void setCandidateElements(ImmutableList<Action> candidates) {
        this.actions = candidates;

    }

    public synchronized void getRobotAction(ImmutableList<Action> candidates) {
        if (!locker) {
            try {
                this.actions = candidates;
                wait();
            } catch (InterruptedException e) {
                LOGGER.info("It's seems there is something interrupted waiting thread.");
                LOGGER.debug(e.getMessage());
                e.printStackTrace();
            }
        }
        LOGGER.info("Get the Action {} successfully", this.actions);
        locker = false;
        notify();
    }

    public synchronized void getCrawlerAction() throws RuntimeException {
        if (locker) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.debug("Get the crawler actions successfully~~");
        locker = true;
        notify();
    }

//    public synchronized void getRobotAction() throws RuntimeException {
//        if (!locker) {
//            try {
//                wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        LOGGER.debug("Get the Robot action successfully~~");
//        locker = false;
//        notify();
//    }

//    public synchronized void getCrawlerAction() throws RuntimeException {
//        if (locker) {
//            try {
//                wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        LOGGER.debug("Get the crawler actions successfully~~");
//        locker = true;
//        notify();
//    }

}
