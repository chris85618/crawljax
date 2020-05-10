package ntut.edu.tw.irobot;

import com.crawljax.core.CrawljaxRunner;
import ntut.edu.tw.irobot.lock.WaitingLock;
import org.junit.Test;

import static org.junit.Assert.*;

public class CrawlJaxRunnerFactoryTest {

    @Test
    public void createCrawlJaxRunner() {
        CrawlJaxRunnerFactory crawlJaxRunnerFactory = new CrawlJaxRunnerFactory();
        CrawljaxRunner crawljaxRunner = crawlJaxRunnerFactory
                .createAgentCrawlJaxRunner("http://localhost:8787/", new WaitingLock());
        assertTrue(crawljaxRunner instanceof CrawljaxRunner);
    }
}


