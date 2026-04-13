package org.JAVA_RAG;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.Path;

@Service
public class IngestionService {

    private final String pineconeKey;

    public IngestionService(@Value("${pinecone_api_key}") String pineconeKey) {
        this.pineconeKey = pineconeKey;
    }

    public void ingestFile(String filePath) {
        // 1. Load the document
        Document document = FileSystemDocumentLoader.loadDocument(
                Path.of(filePath),
                new ApacheTikaDocumentParser()
        );

        // 2. Local Embedding
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // 3. Setup Pinecone (our permanent database)
        var embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(pineconeKey)
                .environment("us-east-1") // e.g., "us-east-1"
                .index("rag-index")
                .build();

        // 4. Create the Ingestor (The "Machine" that chunks and saves)
        var ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 100)) // 500 chars, 100 overlap
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 5. Run it!
        ingestor.ingest(document);
        System.out.println("Ingestion complete! Your data is now in Pinecone.");
    }
}