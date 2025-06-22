package ntut.edu.tw.irobot.lock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.FutureTask;

import static org.junit.Assert.*;

public class WaitingLockTest {

    // need to test
    @Test
    public void givenWhenThreadSleepingThen() {
        WaitingLock waitingLock = new WaitingLock();
        Thread waitingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                waitingLock.waitForRobotCommand();
            }
        });

        Thread wakeUpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                waitingLock.wakeUpSleepingThread();
            }
        });

        waitingThread.start();

        wakeUpThread.start();
    }

    @Test
    public void waitForCrawlerResponse() {

    }
}