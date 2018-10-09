//ch-sh begin
package com.crawljax.core.plugin;

import java.util.List;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.state.StateVertex;

public interface AfterRetrievePathPlugin extends Plugin {
    void afterRetrievePath(CrawlerContext context, List<String[]> path, StateVertex targetState);
}
//ch-sh end