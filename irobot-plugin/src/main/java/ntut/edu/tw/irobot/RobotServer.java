package ntut.edu.tw.irobot;

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
                lock.getCrawlerAction();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
