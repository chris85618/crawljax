package ntut.edu.aiguide.crawljax.plugins.domain.FormSubmissionJudger;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import com.crawljax.util.DomUtils;

import ntut.edu.aiguide.crawljax.plugins.domain.SequenceMatcher.ListSimilarity;

public class FormSubmissionJudgerByPageSimilarity implements IFormSubmissionJudger {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormSubmissionJudgerByPageSimilarity.class);
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
		final List<String> beforeDomFeatureStateList = DomUtils.traverseElementFeatureStateList(beforeDom);
		final List<String> afterDomFeatureStateList = DomUtils.traverseElementFeatureStateList(afterDom);
		return this.judge(beforeDomFeatureStateList, afterDomFeatureStateList);
	}

	public boolean judge(final List<String> beforeDom, final List<String> afterDom) {
		// LOGGER.debug("Size of (List<String>) beforeDom: {}", beforeDom.size());
		// LOGGER.debug("Size of (List<String>) afterDom: {}", afterDom.size());
		final double similarity = this.getSimilarity(beforeDom, afterDom);
		return this.judge(similarity);
	}

	public boolean judge(final double similarity) {
		final boolean result = similarity < this.threshold;
		// LOGGER.info("similarity is: {}", similarity);
		LOGGER.info("FormSubmissionJudgerByPageSimilarity judges that this form submission result is: {}", Boolean.toString(result));
		return result;
	}

	public double getSimilarity(List<String> beforeSequence, List<String> afterSequence) {
		return this.matcher.get(beforeSequence, afterSequence);
	}
}
