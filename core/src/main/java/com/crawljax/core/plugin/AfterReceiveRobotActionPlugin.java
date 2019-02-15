package com.crawljax.core.plugin;

/**
 * This Plugin will get the signal that the robot want to restart the crawling
 *      ex. When Crawljax is crawling state6 and get the robot signal,
 *               Crawljax will restart to index.
 */

public interface AfterReceiveRobotActionPlugin extends Plugin {

    /**
     * @return boolean 
     *          This will get the signal that robot want to restart crawling
     */

    boolean isRestartOrNot();

}