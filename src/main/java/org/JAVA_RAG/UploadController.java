package org.JAVA_RAG;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            // 1. Get the real project root
            String projectRoot = System.getProperty("user.dir");
            Path baseDir = Paths.get(projectRoot, "data").toAbsolutePath().normalize();

            // 2. Sanitize: Use only the filename, ignore any path info provided by user
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.contains("..")) {
                return "Invalid file name!";
            }

            // 3. Resolve path and VERIFY it stays inside baseDir
            Path targetLocation = baseDir.resolve(fileName).normalize();
            if (!targetLocation.startsWith(baseDir)) {
                throw new SecurityException("Cannot store file outside current directory!");
            }

            // 4. Save the file
            file.transferTo(targetLocation.toFile());
            ingestionService.ingestFile(targetLocation.toString());

            return "File uploaded safely!";
        } catch (Exception e) {
            return "Security Error: " + e.getMessage();
        }
    }
}