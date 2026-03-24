package ai.quadratic.api.dto;
public record NeuralResponse(
    String type, double x1, double x2, double delta,
    double confidence, boolean modelLoaded
) {}