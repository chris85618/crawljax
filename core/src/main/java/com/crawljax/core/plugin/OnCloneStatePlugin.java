package com.crawljax.core.plugin;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;


/**
 * When Crwaling State is a `Clone State` or `Same State`, will run this plugin
 */

public interface OnCloneStatePlugin extends Plugin {

	/**
	 *  This will gave you all the action on the page, you can reconstruct the order
	 * 		because the cache does not refresh the order, so I (TonyLin) add this plugin
	 * 	
	 * 	It's just like the {@link PreStateCrawlingPlugin}
	 * 
	 * @param context
	 * 			the current session data.
	 * @param candidateElements
	 * 			the candidates for the current state.
	 * @param newState
	 * 			The state being crawled
	 */

	void onCloneState(CrawlerContext context, ImmutableList<CandidateElement> candidateElements, StateVertex newState);
}
