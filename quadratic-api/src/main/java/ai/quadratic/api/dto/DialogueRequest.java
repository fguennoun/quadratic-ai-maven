package ai.quadratic.api.dto;
import jakarta.validation.constraints.NotNull;
public record DialogueRequest(
    @NotNull(message = "Le sessionId est obligatoire") String sessionId,
    String message
) {}