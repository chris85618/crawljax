package com.crawljax.core.plugin;

/**
 * Plugin type that is called every time a state was found by Crawljax.
 */
public interface OnNewFoundStatePlugin extends Plugin {

    /**
     * This will call every time when the Crawljax found new State. 
     * Different form {@link OnNewStatePlugin}, this will call when the Crawljax create a {@link com.crawljax.core.state.StateVertex}
     *
     * @param dom
     *          The dom capture from current browser
     * @return
     *          The dom which you already make some change
     */
    String onNewFoundState(String dom);
}
