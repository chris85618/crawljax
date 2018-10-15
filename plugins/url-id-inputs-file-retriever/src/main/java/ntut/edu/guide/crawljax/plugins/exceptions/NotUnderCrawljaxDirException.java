package ntut.edu.guide.crawljax.plugins.exceptions;

/**
 * Created by vodalok on 2017/2/28.
 */
public class NotUnderCrawljaxDirException extends Exception{
    public NotUnderCrawljaxDirException(){
        super("Can't find crawljax root folder. The plugin may not under the crawljax directory. Please check the crawljax dir name and ensure the plugin is under the crawljax dir.");
    }
}
