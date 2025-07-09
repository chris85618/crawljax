package com.crawljax.core.plugin;

import com.crawljax.core.CrawlerContext;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;

/**
 * Plugin type that is called after a state is crawled. You can use this to plugin to programatically
 * inspect the DOM of a state and to assert certain properties of the state.
 */
public interface PostStateCrawlingPlugin extends Plugin {

	/**
	 * Method that is called after a state is crawled.
	 * <p>
	 * This method can be called multiple times for the same state.
	 *
	 * @param context      The Crawler context.
	 * @param currentState the state that is being crawled. This is the state that is just crawled
	 * and from which the candidate elements are selected.
	 * @param events       The events that were fired on the state.
	 */
	void postStateCrawling(CrawlerContext context, StateVertex currentState,
			ImmutableList<Eventable> events);

}