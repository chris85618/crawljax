package ntut.edu.tw.irobot.lock;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawljaxRunner;
import com.google.common.collect.ImmutableList;
import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
import ntut.edu.tw.irobot.CrawlingInformation;

import ntut.edu.tw.irobot.WebSnapShot;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class WaitingLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingLock.class);

    private CrawlingInformation crawlingInformation;

    private Mutex initMutex = new Mutex();

    private Object lock = new Object();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private CrawljaxRunner crawler = null;
    private Future<CrawlSession> future = null;

    public WaitingLock() {
        this.crawlingInformation = new CrawlingInformation();
        this.crawlingInformation.resetData();
    }

    public WaitingLock(CrawlingInformation crawlingInformation) {
        this.crawlingInformation = crawlingInformation;
        this.crawlingInformation.resetData();
    }


    public void terminateCrawler()  {

        try {
            crawler.stop();
        } finally {
            future = null;
            crawler = null;
            crawlingInformation.resetData();
        }
    }
    /**
     * @return data
     */
    public CrawlingInformation getCrawlingInformation() {
        return this.crawlingInformation;
    }
    /**
     * Caller is robot server
     *
     * @param executor
     *              Thread executor
     * @param crawler
     *              The crawler instance
     */


    public void init(CrawljaxRunner crawljaxRunner) {
        try {
            this.crawler = crawljaxRunner;
            future = this.executorService.submit(crawler);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Caller is robot server
     */
    public synchronized void initCrawler() {
        try {
            LOGGER.info("Acquire the initial lock and wait for Crawljax response...");
            initMutex.acquire();
            notify();
            wait();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Caller is Crawljax
     */
    public synchronized void initReady() {
        LOGGER.info("Release the initial lock...");
        initMutex.release();
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
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setRestartSignal(boolean signal) {

        this.crawlingInformation.setRestartSignal(signal);
    }

    public State getState() {

        return this.crawlingInformation.getState();
    }

    public ImmutableList<Action> getActions() {

        return this.crawlingInformation.getActions();
    }

    public WebSnapShot getWebSnapShot() {

        return this.crawlingInformation.getWebSnapShot();
    }
}
