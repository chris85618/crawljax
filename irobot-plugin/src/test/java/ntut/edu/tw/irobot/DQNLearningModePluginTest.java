package ntut.edu.tw.irobot;

import ntut.edu.tw.irobot.lock.WaitingLock;
import ntut.edu.tw.irobot.plugin.DQNLearningModePlugin;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class DQNLearningModePluginTest {
    private static DQNLearningModePlugin dqnPlugin = new DQNLearningModePlugin(new WaitingLock(), 3000);

    @Test
    public void filterDom() {
        System.out.println(Resource.newClassPathResource("/site/VariableElementPage.html").getURI().getPath());
        String path = Paths.get(Resource.newClassPathResource("/site/VariableElementPage.html").getURI()).toString();
        String correctPath = Paths.get(Resource.newClassPathResource("/site/RemovedVariableElementPage.html").getURI()).toString();
        try {
            String dom = new String(Files.readAllBytes(Paths.get(path)));
            String correctDOM = new String(Files.readAllBytes(Paths.get(correctPath)));
            String filterDOM = dqnPlugin.filterDom(dom.trim(), "http://test_data").trim();
            assertEquals(correctDOM, filterDOM);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}