package ntut.edu.tw.irobot.state;

import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.StateVertexImpl;
import org.junit.Test;

import static org.junit.Assert.*;

public class iRobotStateTest {
    @Test(expected = ClassCastException.class)
    public void testEqual() {
        StateVertex a = new StateVertexImpl(0, "", "state1", "<div></div>", "");
        iRobotState b = (iRobotState) a;
        System.out.println(b.getId());
    }

}