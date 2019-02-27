package com.crawljax.core.plugin;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;

/**
 * This Plugin will be call every time when the crawler is restart.
 */
public interface OnRestartCrawlingStatePlugin extends Plugin {

    /**
     * This will allow you to modify the state, which is a old state,
     *      so you can be do something.
     * This is similar to {@link PreStateCrawlingPlugin}, but that plugin
     *      will be call at pre "New State" crawling.
     *
     * @param context
     *            the current session data.
     * @param candidateElements
     *            the candidates for the current state.
     * @param state
     *            The state being crawled
     */
    void onRestartCrawling(CrawlerContext context,
                           ImmutableList<CandidateElement> candidateElements, StateVertex state);
}
