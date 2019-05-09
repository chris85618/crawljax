package ntut.edu.tw.irobot.lock;

import ntut.edu.tw.irobot.CrawlingInformation;

import ntut.edu.tw.irobot.WebSnapShot;
import ntut.edu.tw.irobot.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WaitingLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingLock.class);

    private CrawlingInformation crawlingInformation;

    private Object lock = new Object();

    public WaitingLock() {
        this.crawlingInformation = new CrawlingInformation();
        this.crawlingInformation.resetData();
    }

    public WaitingLock(CrawlingInformation crawlingInformation) {
        this.crawlingInformation = crawlingInformation;
        this.crawlingInformation.resetData();
    }

    /**
     * @return data
     */
    public CrawlingInformation getCrawlingInformation() {
        return this.crawlingInformation;
    }

    /**
     * Caller is robot server, restart will call this method
     */
    public synchronized void initCrawler() {
        try {
            LOGGER.info("Acquire the initial lock and wait for Crawljax response...");
            notify();
            wait();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean setTargetAction(Action action, String value) {
        this.crawlingInformation.resetData();
        this.crawlingInformation.setTargetAction(action, value);
        this.wakeUpSleepingThread();

        return crawlingInformation.isExecuteSuccess();
    }

    public void wakeUpSleepingThread() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * Caller is Crawljax
     *
     * @throws RuntimeException
     */
    public void waitForRobotCommand() throws RuntimeException {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
            }
        }
    }

    public void setRestartSignal(boolean signal) {
        this.crawlingInformation.setRestartSignal(signal);
    }

    public WebSnapShot getWebSnapShot() {
        return this.crawlingInformation.getWebSnapShot();
    }
}
