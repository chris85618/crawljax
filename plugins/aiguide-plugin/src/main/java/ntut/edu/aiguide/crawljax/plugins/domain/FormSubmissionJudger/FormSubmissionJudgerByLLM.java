package ntut.edu.aiguide.crawljax.plugins.domain.FormSubmissionJudger;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import ntut.edu.aiguide.crawljax.plugins.domain.SequenceMatcher.SequenceMatcher;
import ntut.edu.aiguide.crawljax.plugins.domain.SequenceMatcher.SequenceMatcher.Opcode;
import ntut.edu.aiguide.crawljax.plugins.domain.llm.ChatGPTService;
import ntut.edu.aiguide.crawljax.plugins.domain.llm.ILlmService;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.crawljax.util.DomUtils;

// TODO: 調整架構
public class FormSubmissionJudgerByLLM implements IFormSubmissionJudger {
	public interface IFormSubmissionJudgerByLLM {
		@SystemMessage("You are an AI web crawler assistant. Follow the user requirements carefully and The user will prompt you for the DOM differences before and after submitting the web form. Please answer whether the form was submitted successfully. ")
		public boolean judge(final String diffDom);
	}

    private static final Logger LOGGER = LoggerFactory.getLogger(FormSubmissionJudgerByLLM.class);
	final ILlmService llm;
	final SequenceMatcher<String> matcher = new SequenceMatcher<>();

	FormSubmissionJudgerByLLM() {
		llm = new ChatGPTService();
	}

	/**
	 * Check if the form submission is successful by comparing the before DOM and
	 * the after DOM.
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
		final String diffStr = this.getDiffElements(beforeDom, afterDom);
		return this.judge(diffStr);
	}

	private boolean judge(final String diffStr) {
		if (diffStr.isEmpty()) {
			return false; // No changes detected, assume submission failed
		} else {
			// LOGGER.debug("diffStr: {}", diffStr);
			final IFormSubmissionJudgerByLLM formSubmissionJudgerByLLM = AiServices.create(IFormSubmissionJudgerByLLM.class, this.llm.getModel());
			final boolean result = formSubmissionJudgerByLLM.judge(diffStr);
			LOGGER.info("FormSubmissionJudgerByLLM judges that this form submission result is: {}", Boolean.toString(result));
			return result;
		}
	}

	private String getDiffElements(List<String> beforeDomFeatureStateList, List<String> afterDomFeatureStateList) {
		String diffStr = "";
		this.matcher.setSeqs(beforeDomFeatureStateList, afterDomFeatureStateList);
		for (Opcode opcode : this.matcher.getOpcodes()) {
			if(opcode.tag == Opcode.Tag.INSERT) {
				// TODO: 產出與Python版差了字串的''符號，若會影響準確度可調整
                diffStr += afterDomFeatureStateList.subList(opcode.j1, opcode.j2).toString() + "\n";
			}
		}
		return diffStr;
	}
}
