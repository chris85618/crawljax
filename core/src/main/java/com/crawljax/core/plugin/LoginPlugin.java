package com.crawljax.core.plugin;

import com.crawljax.browser.EmbeddedBrowser;

/**
 * Created by vodalok on 2017/6/11.
 */
public interface LoginPlugin extends Plugin{
    void login(EmbeddedBrowser browser);
}
