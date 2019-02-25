package ntut.edu.tw.irobot.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class WorkDirManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkDirManager.class);

    private int currentRecordID;
    private File outputFolder;
    private File recordFolder;

    public WorkDirManager () {
        outputFolder = getOutputFolder();
        currentRecordID = getCurrentRecordID();
        recordFolder = createRecordFolder();
    }

    public File getRecordFolder() {
        LOGGER.info("Get the record file...");
        return recordFolder;
    }

    private File getOutputFolder() {
        File outputFolder = new File(System.getProperty("user.dir") + File.separatorChar + "crawler-record");
        if (!outputFolder.exists())
            outputFolder.mkdir();
        return outputFolder;
    }

    private int getCurrentRecordID() {
        LOGGER.info("Get the current record ID : {}", outputFolder.list().length + 1);
        return outputFolder.list().length + 1;
    }

    private File createRecordFolder() {
        File recordFolder = new File(outputFolder.getAbsolutePath() + File.separatorChar + currentRecordID);
        if (!recordFolder.exists())
            recordFolder.mkdir();
        return recordFolder;
    }
}
