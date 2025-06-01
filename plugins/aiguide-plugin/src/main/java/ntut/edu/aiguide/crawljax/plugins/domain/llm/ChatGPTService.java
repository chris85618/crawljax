package ntut.edu.aiguide.crawljax.plugins.domain.llm;

// import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.data.message.UserMessage;

// TODO: 調整架構
public class ChatGPTService implements ILlmService {

    private final String modelName;
    private final String apiKey;
    private final OpenAiChatModel model;
    // private final ChatModel model;
    private final static double DEFAULT_TEMPERATURE = 0.0;
    private final static String DEFAULT_MODEL_NAME = "gpt-4o-mini";
    private final static String DEFAULT_APT_KEY = System.getenv("OPENAI_API_KEY");

    public ChatGPTService() {
        this(DEFAULT_MODEL_NAME);
    }

    public ChatGPTService(final String modelName) {
        this(modelName, DEFAULT_APT_KEY);
    }

    public ChatGPTService(final String modelName, final String apiKey) {
        this(modelName, apiKey, DEFAULT_TEMPERATURE);
    }

    public ChatGPTService(final double temperature) {
        this(DEFAULT_MODEL_NAME, DEFAULT_APT_KEY, temperature);
    }

    public ChatGPTService(final String modelName, final double temperature) {
        this(modelName, DEFAULT_APT_KEY, temperature);
    }

    public ChatGPTService(final String modelName, final String apiKey, final double temperature) {
        this.modelName = modelName; 
        this.apiKey = apiKey;
        try {
            this.model = OpenAiChatModel.builder()
                .apiKey(this.apiKey)
                .modelName(this.modelName)
                .temperature(temperature)
                .logRequests(true)
                .logResponses(true)
                .build();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.toString());
            throw e;
        }
    }
    
    public OpenAiChatModel getModel() {
    // public ChatModel getModel() {
        return this.model;
    }

    // @Override
    // public String getResponse(final String prompt) {
    //     return this.model.chat(prompt);
    // }

    @Override
    public String getResponse(final String prompt) {
        ChatRequest chatRequest = new ChatRequest.Builder()
        .messages(new UserMessage(prompt))
        .build();
        return chatRequest.toString();
    }
    
}
