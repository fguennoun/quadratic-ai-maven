package ai.quadratic.api.dto;
public record NeuralStatusResponse(
    boolean modelLoaded, String modelPath, String architecture, String message
) {}