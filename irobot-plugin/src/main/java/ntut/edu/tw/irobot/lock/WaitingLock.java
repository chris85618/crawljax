package ntut.edu.tw.irobot.lock;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawljaxRunner;
import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
import ntut.edu.tw.irobot.CrawlingInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class WaitingLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingLock.class);

    private CrawlingInformation source;
    private Mutex initMutex;
    private Mutex crawljaxMutex;
    private Mutex robotMutex;
    private Mutex terminateMutex;
    private CrawljaxRunner crawler;
    private Future<CrawlSession> future;


    public WaitingLock() {
        this.source = new CrawlingInformation();
        this.initMutex = new Mutex();
        this.crawljaxMutex = new Mutex();
        this.robotMutex = new Mutex();
        this.terminateMutex = new Mutex();
        this.crawler = null;
        this.future = null;
    }

    public void terminateCrawler() throws ExecutionException, InterruptedException {
        try {
            resetMutex();
            crawler.stop();
            CrawlSession sec = future.get();
//            future.cancel(true);
//            while(!future.cancel(true));
//            System.out.println(future.isDone());
//            System.out.println(future.isCancelled());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            future = null;
            crawler = null;
            source.resetData();
        }
    }

    public synchronized void resetMutex() {
        initMutex.release();
        crawljaxMutex.release();
        robotMutex.release();
        notify();
    }

    /**
     * @return data
     */
    public CrawlingInformation getSource() {
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
            LOGGER.info("Robot init the crawler, and wait for crawler response....");
            initMutex.acquire();
            this.crawler = crawler;
            future = executor.submit(crawler);
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

    /**
     * Caller is Crawljax
     *
     * @throws RuntimeException
     */
    public synchronized void waitForRobotCommand() throws RuntimeException {
        try {
            crawljaxMutex.acquire();
            notify();
            LOGGER.info("Crawling is done, wait for Robot command...");
            wait();
            crawljaxMutex.release();
        } catch (InterruptedException e) {
            LOGGER.info("It's seems there is something interrupted waiting thread.");
            LOGGER.debug(e.getMessage());
            e.printStackTrace();
        }
        LOGGER.debug("Get the Robot actions successfully~~");
    }

    /**
     * Caller is Robot
     *
     * @throws RuntimeException
     */
    public synchronized void waitForCrawlerResponse() throws RuntimeException {
        try {
            robotMutex.acquire();
            notify();
            LOGGER.info("The robot is finished setting data, waiting for Crawler response...");
            wait();
            robotMutex.release();
        } catch (InterruptedException e) {
            LOGGER.info("It's seems there is something interrupted waiting thread.");
            e.printStackTrace();
        }
        LOGGER.debug("Get the Crawler actions successfully~~");
    }
}
