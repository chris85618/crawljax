package com.crawljax.core.plugin;

import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;

public interface OnAlertPresentedPlugin extends Plugin {
	void onAlertPresented(StateVertex state, Eventable event, String alertText);
}
