package com.crawljax.util.llm;

// import dev.langchain4j.model.openai.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public interface ILlmService {
    public String getResponse(final String prompt);
    // public ChatModel getModel();
    public OpenAiChatModel getModel();
}
