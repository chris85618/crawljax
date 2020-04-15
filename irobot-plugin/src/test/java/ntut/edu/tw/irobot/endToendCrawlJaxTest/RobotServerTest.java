package ntut.edu.tw.irobot.endToendCrawlJaxTest;

import ntut.edu.tw.irobot.CrawlingInformation;
import ntut.edu.tw.irobot.RobotServer;
import ntut.edu.tw.irobot.WebSnapShot;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.lock.WaitingLock;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class RobotServerTest {

    RobotServer robotServer;
    InformationDecorator decorator;

    public class SatisfyInteractionOrderMatcher extends TypeSafeMatcher<ArrayList<String>> {
        protected final int expectedTimes;

        public SatisfyInteractionOrderMatcher(int times) {
            expectedTimes = times;
        }

        @Override
        protected boolean matchesSafely(ArrayList<String> item) {
            return calculateNumberOfInteractionOrder(item) == expectedTimes;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("thread interaction order happens: ")
                    .appendValue(expectedTimes)
                    .appendText(" times");
        }

        @Override
        public void describeMismatchSafely(ArrayList<String> actual, Description mismatchDescription) {
            mismatchDescription.appendText("actually happends: ")
                    .appendValue(calculateNumberOfInteractionOrder(actual))
                    .appendText(" Times, Maybe invalid interaction order");
        }


        ArrayList<String> preprocessActual(ArrayList<String> actual) {
            ArrayList<String> result = new ArrayList<>();

            String tempAction = "";
            for (int i = 0; i < actual.size(); i++) {
                String threadAction = actual.get(i);
                if (threadAction.contains("CrawlJax")) {
                    threadAction = "CrawlJax";
                } else if (threadAction.contains("Robot")) {
                    threadAction = "Robot";
                }

                if (tempAction != threadAction) {
                    tempAction = threadAction;
                    result.add(threadAction);
                }
            }
            return result;
        }

        public boolean isValidThreadOrder(ArrayList<String> actual) {
            String[] result = {"CrawlJax", "Robot"};
            for (int i = 0; i < actual.size(); i++)
                if (actual.get(i) != result[i % 2])
                    return false;

            return true;
        }

        private int calculateActual(ArrayList<String> actual) {
            boolean isCrawlJaxThreadAction = false;
            boolean isRobotThreadAction = false;
            int count = 0;

            for (int i = 0; i <  actual.size(); i++) {
                if (actual.get(i).contains("CrawlJax")) {
                    isCrawlJaxThreadAction = true;
                }

                if (actual.get(i).contains("Robot")) {
                    isRobotThreadAction = true;
                }

                if (isRobotThreadAction && isCrawlJaxThreadAction) {
                    isCrawlJaxThreadAction = false;
                    isRobotThreadAction = false;
                    count++;
                }
            }
            return count;
        }

        private int calculateNumberOfInteractionOrder(ArrayList<String> actual) {
            ArrayList<String> processedActual = preprocessActual(actual);
            if (!isValidThreadOrder(processedActual))
                throw new RuntimeException("Thread Order is not valid");
            return calculateActual(processedActual);
        }
    }

    public SatisfyInteractionOrderMatcher satisfyInteractionOrder(int times) {
        return new SatisfyInteractionOrderMatcher(times);
    }


    @Before
    public void setUp() {
        decorator = new InformationDecorator(new CrawlingInformation());
        WaitingLock waitingLock = new WaitingLock(decorator);

        robotServer = new RobotServer(waitingLock);
        robotServer.setUrl("http://localhost:8888/age", false);
    }

    @After
    public void tearDown() {
        robotServer.terminateCrawler();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void executeAction(int actionIndex, String value) {
        WebSnapShot webSnapShot = robotServer.getWebSnapShot();
        robotServer.executeAction(webSnapShot.getActions().get(actionIndex), value);
    }


    @Test
    public void GivenURLWhenSendSignalToServiceThenResponseOfCrawlJaxIsCorrect() {
        executeAction(3, "10");

        executeAction(2, "10");

        executeAction(2, "20");

        final int THREE_TIMES = 3;

        ArrayList<String> threadSequence = decorator.getThreadSequence();

        assertThat(threadSequence, satisfyInteractionOrder(THREE_TIMES));
    }

    private void assertCurrentUrl(String url) {
        WebSnapShot webSnapShot = robotServer.getWebSnapShot();
        assertEquals(url, webSnapShot.getState().getUrl());
    }

    @Test
    public void WhenRestartCrawlerThenCrawlerBackToRootIndex() {
        executeAction(3, "10");

        executeAction(2, "test@test.com");

        executeAction(1, "");

        assertCurrentUrl("http://localhost:8888/view/concept.html?");

        robotServer.restart();

        assertCurrentUrl("http://localhost:8888/age");

        final int FIVE_TIMES = 5;

        ArrayList<String> threadSequence = decorator.getThreadSequence();
        assertThat(threadSequence, satisfyInteractionOrder(FIVE_TIMES));
    }

    @Test
    public void ContinuousTerminateCrawljaxAndCheckIfThereIsGracefullyTerminate() {
        for (int i = 0; i < 10; i++) {
            robotServer.terminateCrawler();
            robotServer.setUrl("http://localhost:8888/age", false);
            WebSnapShot webSnapShot = robotServer.getWebSnapShot();
            robotServer.executeAction(webSnapShot.getActions().get(3), "10");
            robotServer.getWebSnapShot();
        }
    }

    @Test
    public void CheckIsElementsInteractableWillReturnCorrectOrder() {
        List<Boolean> result = robotServer.isElementsInteractable(robotServer.getWebSnapShot().getActions());
        List<Boolean> groundTrue = Arrays.asList(true, false, true, true);
        assertEquals(groundTrue, result);
    }

}