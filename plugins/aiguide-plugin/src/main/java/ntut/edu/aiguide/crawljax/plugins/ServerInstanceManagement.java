package ntut.edu.aiguide.crawljax.plugins;

public interface ServerInstanceManagement {
    void createServerInstance();
    void closeServerInstance();
    void recordCoverage();
}
