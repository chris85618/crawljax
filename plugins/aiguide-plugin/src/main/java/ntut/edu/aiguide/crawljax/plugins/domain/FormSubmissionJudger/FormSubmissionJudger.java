package ntut.edu.aiguide.crawljax.plugins.domain.FormSubmissionJudger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormSubmissionJudger implements IFormSubmissionJudger {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormSubmissionJudger.class);
	final FormSubmissionJudgerByPageSimilarity formSubmissionJudgerByPageSimilarity;
	final FormSubmissionJudgerByLLM formSubmissionJudgerByLLM;

    public FormSubmissionJudger() {
        formSubmissionJudgerByPageSimilarity = new FormSubmissionJudgerByPageSimilarity();
        formSubmissionJudgerByLLM = new FormSubmissionJudgerByLLM();
    }

	/**
	 * Check if the form submission is successful by comparing the before DOM and the after DOM.
	 * 
	 * @param beforeDomString the before DOM as a string
	 * @param afterDomString the after DOM as a string
	 * 
	 * @return submitting successfully or not
	 */
	@Override
	public boolean judge(final String beforeDomString, final String afterDomString) {
		final boolean formSubmissionResultByPageSimilarity = this.formSubmissionJudgerByPageSimilarity.judge(beforeDomString, afterDomString);
		final boolean formSubmissionResultByLLM = this.formSubmissionJudgerByLLM.judge(beforeDomString, afterDomString);
		final boolean result = formSubmissionResultByPageSimilarity || formSubmissionResultByLLM;
		LOGGER.info("FormSubmissionJudger judges that this form submission result is: {}", Boolean.toString(result));
        return result;
	}
}
