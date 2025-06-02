package com.crawljax.util.FormSubmissionJudger;

import java.io.IOException;
import java.util.List;
import org.w3c.dom.Document;
import com.crawljax.util.DomUtils;
import com.crawljax.util.ListSimilarity;

public class FormSubmissionJudgerByPageSimilarity implements IFormSubmissionJudger {
	final double threshold = 0.95;
	final ListSimilarity<String> matcher = new ListSimilarity<>();

	/**
	 * Check if the form submission is successful by comparing the before DOM and the after DOM.
	 * 
	 * @param beforeDom the before DOM as a string
	 * @param afterDom the after DOM as a string
	 * 
	 * @return submitting successfully or not
	 */
	@Override
	public boolean judge(final String beforeDom, final String afterDom) {
		try {
			final Document beforeDomDocument = DomUtils.asDocument(beforeDom);
			final Document afterDomDocument = DomUtils.asDocument(afterDom);
			return this.judge(beforeDomDocument, afterDomDocument);
		} catch (IOException e) {
			return false;
		}
	}

	public boolean judge(final Document beforeDom, final Document afterDom) {
		final List<String> beforeDomFeatureStateList = DomUtils.getElementFeatureStateList(beforeDom);
		final List<String> afterDomFeatureStateList = DomUtils.getElementFeatureStateList(afterDom);
		return this.judge(beforeDomFeatureStateList, afterDomFeatureStateList);
	}

	public boolean judge(final List<String> beforeDom, final List<String> afterDom) {
		final double similarity = this.getSimilarity(beforeDom, afterDom);
		return this.judge(similarity);
	}

	public boolean judge(final double similarity) {
		return similarity < this.threshold;
	}

	public double getSimilarity(List<String> beforeSequence, List<String> afterSequence) {
		return this.matcher.get(beforeSequence, afterSequence);
	}
}
