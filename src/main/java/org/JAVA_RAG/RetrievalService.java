package org.JAVA_RAG;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RetrievalService {

    // Define the interface for the AI
    interface Assistant {
        String ask(@MemoryId Object memoryId,@UserMessage String question);
    }

    private final Assistant assistant;
    private final Map<Object, ChatMemory> chatMemories = new ConcurrentHashMap<>();

    public RetrievalService(@Value("${gemini_api_key}") String gemini_api_key,@Value("${pinecone_api_key}") String pineconeKey) {

        // 1. Embedding Model
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // 2. Connect to the same Pinecone index
        var embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(pineconeKey)
                .environment("us-east-1")
                .index("rag-index")
                .build();

        // 3. Create the Retriever (The "Search Engine")
        var contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // Get the top 3 most similar chunks
                .build();

        // 4. Connect to Gemini for the final answer
        ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(gemini_api_key)
                .modelName("gemini-2.5-flash-lite")
                .build();

        // 5. Create the "Assistant" (The glue that holds everything together)
        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

    }
    // This is the clean method the Controller will call
    public String ask(String memoryId,String question) {
        return assistant.ask(memoryId,question);
    }
}