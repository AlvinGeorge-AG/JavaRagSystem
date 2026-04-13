package org.JAVA_RAG;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Allows your future frontend to talk to this API
public class ChatController {

    private final RetrievalService ragService;

    // Spring injects the RagService we built earlier
    public ChatController(RetrievalService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/{sessionId}") // Now the URL looks like /api/chat/user123
    public String ask(@PathVariable String sessionId, @RequestBody String question) {
        return ragService.ask(sessionId, question);
    }
}