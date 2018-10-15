package ntut.edu.guide.crawljax.plugins;

import ntut.edu.guide.crawljax.plugins.exceptions.NotUnderCrawljaxDirException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by vodalok on 2017/5/11.
 */
public class URLIDFileRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(URLIDFileRetriever.class);

    private final static String CRAWLJAX_ROOT_FILE_NAME = "crawljax";
    private final static String CRAWLJAX_ROOT_TO_INPUTS_DIR_PATH = "out/url-id-inputs/";
    private static Path cachedURLIDInputDirPath = null;

    private static Path getURLIDInputsDirPath() throws NotUnderCrawljaxDirException {
        //GET CWD
        Path path = Paths.get("").toAbsolutePath();
        LOGGER.info("Plugin ABS CWD Path: {}", path.toAbsolutePath().toString());
        LOGGER.info("Current path file name: {}", path.getFileName().toString());
        LOGGER.info("Compare: {}, {} : {}", path.getFileName().toString(), CRAWLJAX_ROOT_FILE_NAME, path.getFileName().toString().equals(CRAWLJAX_ROOT_FILE_NAME));
        while(!path.getFileName().toString().equals(CRAWLJAX_ROOT_FILE_NAME)){
            LOGGER.info("Plugin ABS CWD Path: {}", path.toAbsolutePath().toString());
            path = path.getParent();
            //No patent
            if(path == null){
                throw new NotUnderCrawljaxDirException();
            }
        }
        LOGGER.info("CRAWLJAX Path: {}", path.toAbsolutePath().toString());

        return path.resolve(CRAWLJAX_ROOT_TO_INPUTS_DIR_PATH);
    }

    public static Path getURLIDFilePath(String urlIdInputFileName) throws NotUnderCrawljaxDirException{
        Path urlIdInputsDirPath;

        //If cached the url id dir path, use cached value
        if(cachedURLIDInputDirPath == null){
            urlIdInputsDirPath = getURLIDInputsDirPath();
            cachedURLIDInputDirPath = urlIdInputsDirPath;
            return urlIdInputsDirPath.resolve(urlIdInputFileName);
        }else{
            return cachedURLIDInputDirPath.resolve(urlIdInputFileName);
        }
    }
}
