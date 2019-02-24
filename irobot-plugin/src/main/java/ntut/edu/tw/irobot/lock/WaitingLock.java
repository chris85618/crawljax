package ntut.edu.tw.irobot.lock;

import com.crawljax.core.CrawljaxRunner;
import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
import ntut.edu.tw.irobot.interaction.Interactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

public class WaitingLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingLock.class);

    private Interactor source;
    private boolean token;
    private Mutex initMutex;


    public WaitingLock() {
        this.source = new Interactor();
        this.token = false;
    }

    public Interactor getSource() {
        return this.source;
    }

    /**
     * Caller is robot server
     *
     * @param executor
     *              Thread executor
     * @param crawler
     *              The crawler instance
     */
    public synchronized void init(ExecutorService executor, CrawljaxRunner crawler) {
        try {
            initMutex.acquire();
            executor.submit(crawler);
            wait();
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

    /**
     * Caller is Crawljax
     *
     * @throws RuntimeException
     */
    public synchronized void waitForRobotCommand() throws RuntimeException {
        if (token) {
            try {
                LOGGER.info("Crawling is done, wait for Robot command...");
                wait();
            } catch (InterruptedException e) {
                LOGGER.info("It's seems there is something interrupted waiting thread.");
                LOGGER.debug(e.getMessage());
                e.printStackTrace();
            }
        }
        LOGGER.debug("Get the Robot actions successfully~~");
        token = true;
    }

    public synchronized void waitForCrawlerResponse() throws RuntimeException {
        if (!token) {
            try {
                LOGGER.info("Robot is already setting data, waiting for Crawler response...");
                wait();
            } catch (InterruptedException e) {
                LOGGER.info("It's seems there is something interrupted waiting thread.");
                e.printStackTrace();
            }
        }
        LOGGER.debug("Get the Crawler actions successfully~~");
        token = false;

    }

}
