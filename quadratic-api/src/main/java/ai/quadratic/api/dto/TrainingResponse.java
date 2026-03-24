package ai.quadratic.api.dto;
public record TrainingResponse(
    boolean success, int epochs, double finalLoss, double finalValLoss, String message
) {}