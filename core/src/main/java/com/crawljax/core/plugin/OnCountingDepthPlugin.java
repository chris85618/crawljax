package com.crawljax.core.plugin;

import com.crawljax.core.state.StateVertex;

import java.util.concurrent.atomic.AtomicInteger;

public interface OnCountingDepthPlugin extends Plugin {
    void controlDepth(StateVertex currentState, AtomicInteger crawlDepth);
}
