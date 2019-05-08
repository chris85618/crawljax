package ntut.edu.tw.irobot;

import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.utility.TestHelper;
import org.junit.Test;

import static org.junit.Assert.*;

public class CrawlingInformationTest {

    @Test
    public void setCurrentTargetAction() {
        Action action = TestHelper.createAction();

        CrawlingInformation crawlingInformation = new CrawlingInformation();
        crawlingInformation.setTargetAction(action, "");

        assertEquals(crawlingInformation.getTargetAction(), action);
        assertEquals(crawlingInformation.getTargetValue(), "");
    }
}