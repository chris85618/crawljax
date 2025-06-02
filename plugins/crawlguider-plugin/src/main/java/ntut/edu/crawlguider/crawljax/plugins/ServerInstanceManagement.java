package ntut.edu.crawlguider.crawljax.plugins;

public interface ServerInstanceManagement {
    void createServerInstance();
    void closeServerInstance();
    void recordCoverage();
}
