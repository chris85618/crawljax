package ntut.edu.tw.irobot;

import com.crawljax.core.CrawljaxRunner;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.lock.WaitingLock;
import ntut.edu.tw.irobot.state.State;

import ntut.edu.tw.irobot.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RobotServer implements Runnable {

    private GatewayServer server;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private CrawljaxRunner crawlJaxRunner;

    private CrawlJaxRunnerFactory factory = new CrawlJaxRunnerFactory();

    private WaitingLock waitingLock;

    private Timer crawlerTimer;

    private static final Logger LOGGER = LoggerFactory.getLogger(RobotServer.class);

    public RobotServer() {
        this(new WaitingLock());
    }

    public RobotServer(WaitingLock waitingLock) {
        this.waitingLock = waitingLock;
        this.server = new GatewayServer(this);
        this.crawlerTimer = new Timer();
    }

    @Override
    public void run() {
        server.start();
    }

    /**
     *  Setting the crawling target
     * @param url
     */
    public boolean setUrl(String url, boolean wrapElement) {
        factory.setWrapElementMode(wrapElement);
        boolean performResult = initializeCrawlJax(url);
        return performResult;
    }

    private boolean initializeCrawlJax(String url) {
        try {
            beginCrawlerTimer();

            performCrawlJax(url);

            stopCrawlerTimer();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void performCrawlJax(String url) {
        this.crawlJaxRunner = factory.createCrawlJaxRunner(url, this.waitingLock);

        this.executorService.submit(this.crawlJaxRunner);

        this.waitingLock.waitForCurrentWebSnapShot();
    }

    private void beginCrawlerTimer() {
        crawlerTimer.start();
    }

    private void stopCrawlerTimer() {
        crawlerTimer.stop();
    }

    /**
     *  Restart to index state
     */
    public void restart() {
        LOGGER.info("Set the restart signal and initialize crawler response...");

        waitingLock.setRestartSignal(true);
        // begin counting time
        beginCrawlerTimer();

        waitingLock.initCrawler();
        // stop counting time
        stopCrawlerTimer();
    }

    public WebSnapShot getWebSnapShot() {
        return this.waitingLock.getWebSnapShot();
    }
    /**
     * This step will set the action
     *          and wait the crawler response
     *
     * @param action
     *              The action which the robot assigned
     * @param value
     *              The value which the robot gave
     * @return
     *              The boolean which the Action is execute success or not
     */

    public boolean executeAction(Action action, String value) {
        LOGGER.info("Execute Action {}, and the value is {}...", action, value);
        beginCrawlerTimer();
        boolean result = waitingLock.setTargetAction(action, value);
        stopCrawlerTimer();
        return result;
    }

    public String getCrawlerSpendingTime() {
        return crawlerTimer.getDurationTime();
    }

    public boolean terminateCrawler() {
        LOGGER.info("Terminate Crawler ...");
        try {
            beginCrawlerTimer();
            this.waitingLock.wakeUpSleepingThread();
            this.crawlJaxRunner.stop();
        } catch (Exception e) {
            e.printStackTrace();
            stopCrawlerTimer();
            LOGGER.warn("Terminate Crawler Failure...");
            return false;
        } finally {
            stopCrawlerTimer();
            executorService = Executors.newSingleThreadExecutor();
        }
        LOGGER.info("Terminate Crawler Successfully...");
        return true;
    }

    public void resetTimer() {
        crawlerTimer.reset();
    }

    public void  setRecordBoolean(boolean isRecord) {
        factory.setRecordMode(isRecord);
    }
}