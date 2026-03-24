package ai.quadratic.api.dto;
import java.util.List;
public record SolveResponse(
    String type, double delta, Double x1, Double x2,
    String equation, List<String> steps
) {}