package ntut.edu.aiguide.crawljax.plugins.domain.FormSubmissionJudger;


public class FormSubmissionJudgeResultBuilder {
	private String beforeDomString;
	private String afterDomString;
	private IFormSubmissionJudger formSubmissionJudger;

    public FormSubmissionJudgeResultBuilder() {
        this.formSubmissionJudger = new FormSubmissionJudger();
    }

    public FormSubmissionJudgeResultBuilder(IFormSubmissionJudger formSubmissionJudger) {
        this.formSubmissionJudger = formSubmissionJudger;
    }

    public FormSubmissionJudgeResultBuilder setBeforeDom(String beforeDomString) {
        this.beforeDomString = beforeDomString;
        return this;
    }

    public FormSubmissionJudgeResultBuilder setAfterDom(String afterDomString) {
        this.afterDomString = afterDomString;
        return this;
    }
    public boolean build() {
        return this.formSubmissionJudger.judge(beforeDomString, afterDomString);
    }
}
