package ai.quadratic.api.controller;
import ai.quadratic.api.dto.EquationRequest;
import ai.quadratic.api.dto.SolveResponse;
import ai.quadratic.api.service.SolverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class EquationController {
    private final SolverService solverService;
    public EquationController(SolverService solverService) { this.solverService = solverService; }
    @PostMapping("/solve")
    public ResponseEntity<SolveResponse> solve(@Valid @RequestBody EquationRequest request) {
        try { return ResponseEntity.ok(solverService.solve(request)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }
    }
}