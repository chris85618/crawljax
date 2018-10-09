package com.crawljax.core.plugin;

import com.crawljax.core.CrawlerContext;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateVertex;
//ch-sh begin
public interface OnCloneStatePlugin extends Plugin {
	void onCloneState(CrawlerContext context, StateVertex newState);
}
//ch-sh end
