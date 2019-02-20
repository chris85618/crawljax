package ntut.edu.tw.irobot.lock;

import ntut.edu.tw.irobot.interaction.Interactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitingLock implements Locker {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingLock.class);

    private Interactor source;
    private boolean locker;


    public WaitingLock() {
        this.source = new Interactor();
        this.locker = false;
    }

    public Interactor getSource() {
        return this.source;
    }



    public synchronized void waitForRobotCommand() throws RuntimeException {
        if (locker) {
            try {
                wait();
            } catch (InterruptedException e) {
                LOGGER.info("It's seems there is something interrupted waiting thread.");
                LOGGER.debug(e.getMessage());
                e.printStackTrace();
            }
        }
        locker = true;
    }

    public synchronized void waitForCrawlerResponse() throws RuntimeException {
        if (!locker) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.debug("Get the crawler actions successfully~~");
        locker = false;

    }

    @Override
    public void notifySever() {

    }

    @Override
    public void releaseLock() {
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
