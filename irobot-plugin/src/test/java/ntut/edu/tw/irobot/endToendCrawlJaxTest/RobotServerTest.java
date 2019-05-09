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

import static org.hamcrest.MatcherAssert.assertThat;

public class RobotServerTest {

    RobotServer robotServer;
    InformationDecorator decorator;

    public class SatisifyInteractionOrderMatcher extends TypeSafeMatcher<ArrayList<String>> {
        protected final int expectedTimes;

        public SatisifyInteractionOrderMatcher(int times) {
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

    public SatisifyInteractionOrderMatcher satisifyInteractionOrder(int times) {
        return new SatisifyInteractionOrderMatcher(times);
    }


    @Before
    public void setUp() {
        decorator = new InformationDecorator(new CrawlingInformation());
        WaitingLock waitingLock = new WaitingLock(decorator);

        robotServer = new RobotServer(waitingLock);
        robotServer.run();
        robotServer.setUrl("http://localhost:8888/age", false);
        WebSnapShot webSnapShot = robotServer.getWebSnapShot();
        robotServer.executeAction(webSnapShot.getActions().get(2), "10");
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

    @Test
    public void GivenURLWhenSendSignalToServiceThenResponseOfCrawlJaxIsCorrect() {
        WebSnapShot webSnapShot = robotServer.getWebSnapShot();
        System.out.println(webSnapShot);
        robotServer.executeAction(webSnapShot.getActions().get(2), "10");

        webSnapShot = robotServer.getWebSnapShot();
        robotServer.executeAction(webSnapShot.getActions().get(2), "20");



        webSnapShot = robotServer.getWebSnapShot();
//        robotServer.getState().getDom();
//        action = robotServer.getActions().get(1);

        final int FOUR_TIMES = 4;

        ArrayList<String> threadSequence = decorator.getThreadSequence();

        assertThat(threadSequence, satisifyInteractionOrder(FOUR_TIMES));

    }
}