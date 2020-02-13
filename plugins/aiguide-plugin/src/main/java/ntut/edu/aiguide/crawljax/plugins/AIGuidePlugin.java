package ntut.edu.aiguide.crawljax.plugins;


import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.plugin.*;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class AIGuidePlugin implements OnFireEventFailedPlugin, AfterReceiveRobotActionPlugin,
        OnNewFoundStatePlugin, OnRestartCrawlingStatePlugin, OnHtmlAttributeFilteringPlugin {
    @Override
    public boolean isRestartOrNot() {
        return false;
    }

    @Override
    public void onFireEventFailed(CrawlerContext context, Eventable eventable, List<Eventable> pathToFailure) {

    }

    @Override
    public String filterDom(String dom, String url) {
        return null;
    }

    @Override
    public String onNewFoundState(String dom) {
        return null;
    }

    @Override
    public void onRestartCrawling(CrawlerContext context, ImmutableList<CandidateElement> candidateElements, StateVertex state) {

    }
}
