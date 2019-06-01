package ntut.edu.tw.irobot;

import org.eclipse.jetty.util.resource.Resource;
import org.junit.Test;
import org.mockito.Mock;

public class DQNLearningModePluginTest {
    @Mock
    private static DQNLearningModePlugin dqnPlugin;

    @Test
    public void onRestartCrawling() {
    }

    @Test
    public void preStateCrawling() {
    }

    @Test
    public void onFireEventFailed() {
    }

    @Test
    public void isRestartOrNot() {
    }

    @Test
    public void onNewFoundState() {
    }

    @Test
    public void filterDom() {
        System.out.println(Resource.newClassPathResource("/site").getURI());
//        String dom = new FileReader(getClass().getResource("variableState.txt").getPath());
//        dqnPlugin.filterDom()
    }
}