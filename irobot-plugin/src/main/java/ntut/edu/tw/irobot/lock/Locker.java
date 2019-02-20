package ntut.edu.tw.irobot.lock;

import ntut.edu.tw.irobot.interaction.Interactor;

public interface Locker {

    public Interactor getSource();

    public void notifySever();

    public void releaseLock();
}
