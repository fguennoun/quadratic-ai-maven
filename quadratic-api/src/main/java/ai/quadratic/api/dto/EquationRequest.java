package ai.quadratic.api.dto;
import jakarta.validation.constraints.NotNull;
public record EquationRequest(
    @NotNull(message = "Le coefficient a est obligatoire") Double a,
    @NotNull(message = "Le coefficient b est obligatoire") Double b,
    @NotNull(message = "Le coefficient c est obligatoire") Double c
) {}