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


    public void ingestMultipartFile(MultipartFile multipartFile) throws Exception {
        // 1. Get the stream and filename
        try (InputStream inputStream = multipartFile.getInputStream()) {
            String fileName = multipartFile.getOriginalFilename();

            // 2. Use Tika to parse the stream directly (No saving to disk!)
            ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
            Document document = parser.parse(inputStream);

            // 3. Add metadata so we know which file this belongs to
            document.metadata().add("file_name", fileName);

            // 4. Setup Ingestor (Exactly the same as before)
            var embeddingModel = CohereEmbeddingModel.builder()
                    .apiKey(hfApiKey)
                    .modelName("embed-english-v3.0")
                    .inputType("search_document")
                    .build();

            var embeddingStore = PineconeEmbeddingStore.builder()
                    .apiKey(pineconeKey)
                    .index("rag-index")
                    .build();

            var ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(500, 100))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            // 5. Ingest the in-memory document
            ingestor.ingest(document);
        }
    }



    private final String pineconeKey;
    private final String hfApiKey;
    public IngestionService(@Value("${pinecone_api_key}") String pineconeKey,@Value("${java-rag-app}") String hfApiKey) {
        this.pineconeKey = pineconeKey;
        this.hfApiKey = hfApiKey;
    }

    public void ingestFile(String filePath) {
        // 1. Load the document
        Document document = FileSystemDocumentLoader.loadDocument(
                Path.of(filePath),
                new ApacheTikaDocumentParser()
        );

        // 2. Local Embedding
        EmbeddingModel embeddingModel = CohereEmbeddingModel.builder()
                .apiKey(hfApiKey)
                .modelName("embed-english-v3.0") // The gold standard for RAG
                .inputType("search_document")
                .build();

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