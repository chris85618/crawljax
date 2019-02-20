package ntut.edu.tw.irobot;

import ntut.edu.tw.irobot.lock.WaitingLock;

public class RobotServer implements Runnable {
    private WaitingLock lock;

    public RobotServer(WaitingLock lock) {
        this.lock = lock;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(60000);
                lock.waitForCrawlerResponse();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
