package com.crawljax.core.plugin;

public interface OnHtmlAttributeFilteringPlugin extends Plugin {
    String filterDom(String dom, String url);
}
