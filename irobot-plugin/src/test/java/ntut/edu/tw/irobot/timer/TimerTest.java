package ntut.edu.tw.irobot.timer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TimerTest {
    private Timer timer;
    @Before
    public void setUp() {
        timer = new Timer();
    }

    @Test
    public void timerTest() {
        assertEquals(0, timer.getTotalCostTime());

        timer.start();
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        timer.stop();

        assertEquals(600, timer.getTotalCostTime(), 5);
    }

    @Test
    public void timerAddTest() {
        assertEquals(0, timer.getTotalCostTime());

        try {
            timer.start();
            Thread.sleep(600);
            timer.stop();

            assertEquals(600, timer.getTotalCostTime(), 5);

            timer.start();
            Thread.sleep(600);
            timer.stop();
            assertEquals(1200, timer.getTotalCostTime(), 5);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getTime() {
        assertEquals(0, timer.getTotalCostTime());

        try {
            timer.start();
            Thread.sleep(600);
            timer.stop();

            assertEquals("0:0:0", timer.getDurationTime());

            timer.start();
            Thread.sleep(600);
            timer.stop();
            assertEquals("0:0:1", timer.getDurationTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}