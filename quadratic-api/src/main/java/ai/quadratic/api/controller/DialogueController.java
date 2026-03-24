package ai.quadratic.api.controller;
import ai.quadratic.api.dto.DialogueRequest;
import ai.quadratic.api.dto.DialogueResponse;
import ai.quadratic.api.service.DialogueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/dialogue")
public class DialogueController {
    private final DialogueService dialogueService;
    public DialogueController(DialogueService dialogueService) { this.dialogueService = dialogueService; }
    @PostMapping
    public ResponseEntity<DialogueResponse> chat(@Valid @RequestBody DialogueRequest request) {
        return ResponseEntity.ok(dialogueService.handle(request));
    }
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> reset(@PathVariable String sessionId) {
        dialogueService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}