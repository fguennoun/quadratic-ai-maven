package ai.quadratic.api.service;
import ai.quadratic.api.dto.DialogueRequest;
import ai.quadratic.api.dto.DialogueResponse;
import ai.quadratic.symbolic.dialogue.DialogueManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class DialogueService {
    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private record SessionEntry(DialogueManager manager, Instant lastAccess) {}
    public DialogueResponse handle(DialogueRequest req) {
        String sessionId = req.sessionId();
        String message   = req.message() != null ? req.message() : "";
        SessionEntry entry = sessions.compute(sessionId, (id, ex) -> {
            if (ex == null || ex.manager().isFinished())
                return new SessionEntry(new DialogueManager(), Instant.now());
            return new SessionEntry(ex.manager(), Instant.now());
        });
        DialogueManager mgr = entry.manager();
        String response = mgr.handle(message);
        String state    = mgr.getCurrentState().name();
        return new DialogueResponse(sessionId, response, state, mgr.isFinished());
    }
    public void deleteSession(String sessionId) { sessions.remove(sessionId); }
    @Scheduled(fixedDelay = 600_000)
    public void cleanExpiredSessions() {
        Instant threshold = Instant.now().minusSeconds(1800);
        sessions.entrySet().removeIf(e -> e.getValue().lastAccess().isBefore(threshold));
    }
}