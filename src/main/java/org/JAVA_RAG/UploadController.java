package org.JAVA_RAG;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    private final IngestionService ingestionService;

    public UploadController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            ingestionService.ingestMultipartFile(file);
            return "File " + file.getOriginalFilename() + " indexed successfully!";
        } catch (Exception e) {
            return "Security Error: " + e.getMessage();
        }
    }
}