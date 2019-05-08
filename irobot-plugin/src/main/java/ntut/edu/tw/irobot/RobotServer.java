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

    private WaitingLock waitingLock = new WaitingLock();

    private Timer crawlerTimer;

    private static final Logger LOGGER = LoggerFactory.getLogger(RobotServer.class);

    public RobotServer() {
        this.server = new GatewayServer(this);
        this.crawlerTimer = new Timer();
    }

    public RobotServer(WaitingLock waitingLock) {
        // dependency injection used for testing
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
    public boolean setUrl(String url) {
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
        CrawlJaxRunnerFactory factory = new CrawlJaxRunnerFactory();
        this.crawlJaxRunner = factory.createCrawlJaxRunner(url, this.waitingLock);
        this.executorService.submit(this.crawlJaxRunner);
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

    /**
     * @return the State
     */
    public State getState() {
        return this.waitingLock.getState();
    }

    /**
     * @return The Action List
     */
    public ImmutableList<Action> getActions() {
        return this.waitingLock.getActions();
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
        return waitingLock.setTargetAction(action, value);
    }

    public String getCrawlerSpendingTime() {
        return crawlerTimer.getDurationTime();
    }

    public boolean terminateCrawler() {
        try {
            crawlerTimer.reset();
            this.crawlJaxRunner.stop();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("Terminate Crawler Failure...");
            return false;
        }
        LOGGER.info("Terminate Crawler Successfully...");
        return true;
    }
}