package org.JAVA_RAG;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;


@Service
public class IngestionService {

    private final EmbeddingStoreIngestor ingestor; // ✅ Built ONCE

    public IngestionService(
            @Value("${pinecone_api_key}") String pineconeKey,
            @Value("${cohere_api_key}") String cohereKey) {

        var embeddingModel = CohereEmbeddingModel.builder()
                .apiKey(cohereKey)
                .modelName("embed-english-v3.0")
                .inputType("search_document")
                .build();

        var embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(pineconeKey)
                .index("rag-index")
                .build();

        this.ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 100))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    public void ingestMultipartFile(MultipartFile multipartFile) throws Exception {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
            Document document = parser.parse(inputStream);
            document.metadata().add("file_name", multipartFile.getOriginalFilename());
            ingestor.ingest(document); // ✅ Reuses shared ingestor
        }
    }
}