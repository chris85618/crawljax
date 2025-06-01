package ntut.edu.aiguide.crawljax.plugins.domain.FormSubmissionJudger;

public interface IFormSubmissionJudger {
	/**
	 * Check if the form submission is successful by comparing the before DOM and the after DOM.
	 * 
	 * @param beforeDomString the before DOM as a string
	 * @param afterDomString the after DOM as a string
	 * 
	 * @return submitting successfully or not
	 */
	public boolean judge(String beforeDomString, String afterDomString);
}
