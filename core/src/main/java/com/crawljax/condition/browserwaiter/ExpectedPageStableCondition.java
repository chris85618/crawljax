package com.crawljax.condition.browserwaiter;

import net.jcip.annotations.ThreadSafe;

import com.crawljax.browser.EmbeddedBrowser;

/**
 * Checks whether an elements exists.
 * 
 * @author dannyroest@gmail.com (Danny Roest)
 */
@ThreadSafe
public class ExpectedPageStableCondition implements ExpectedCondition {
    final long totalWaitMiliseconds;

	/**
	 * Constructor.
	 * 
	 * @param totalWaitMiliseconds
	 *            the maximum muliseconds to wait the page stable.
	 */
	public ExpectedPageStableCondition(final long totalWaitMiliseconds) {
		this.totalWaitMiliseconds = totalWaitMiliseconds;
	}

	@Override
	public boolean isSatisfied(EmbeddedBrowser browser) {
		browser.waitForPageToBeStable(this.totalWaitMiliseconds);
        return true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
