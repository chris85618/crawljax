package com.crawljax.core.plugin;

import com.crawljax.core.state.Eventable;

/**
 * Plugin type that is called every time a state was found by Crawljax.
 */
public interface OnNewFoundStatePlugin extends Plugin {

    /**
     * This will call every time when the Crawljax found new State. 
     * Different form {@link OnNewStatePlugin}, this will call when the Crawljax create a {@link StateVertex}
     */
    String onNewFoundState(String dom, Eventable event);
}
