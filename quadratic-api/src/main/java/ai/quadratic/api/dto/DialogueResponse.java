package ai.quadratic.api.dto;
public record DialogueResponse(
    String sessionId, String response, String state, boolean finished
) {}