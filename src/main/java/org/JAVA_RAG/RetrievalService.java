package org.JAVA_RAG;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.time.Duration;
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

    public RetrievalService(@Value("${groq_api_key}") String groq_api_key,@Value("${pinecone_api_key}") String pineconeKey,@Value("${cohere_api_key}") String cohereKey) {

        // 1. Embedding Model
        System.out.println("--- STARTING RAG ENGINE ---");
        System.out.println("COHERE KEY: " + cohereKey.substring(0, 5) + "...");

        EmbeddingModel embeddingModel = CohereEmbeddingModel.builder()
                .apiKey(cohereKey)
                .modelName("embed-english-v3.0") // Keep it at 3.0 for V1 API compatibility
                .inputType("search_query")
                .build();

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
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(groq_api_key) // Use your Groq API Key here
                .baseUrl("https://api.groq.com/openai/v1") // <--- THIS REDIRECTS TO GROQ
                .modelName("llama-3.3-70b-versatile") // Groq's fastest model
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
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