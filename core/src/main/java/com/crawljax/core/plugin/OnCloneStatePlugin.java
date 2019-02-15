package com.crawljax.core.plugin;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;


/**
 * When Crwaling State is a `Clone State`, will run this plugin
 * 		in {@link StateMachine.java}
 */

public interface OnCloneStatePlugin extends Plugin {

	/**
	 *  
	 * @param context
	 * 			the current session data.
	 * @param oldState
	 * 			The state being crawled
	 */

	void onCloneState(CrawlerContext context, StateVertex oldState);
}
