package ai.quadratic.api.controller;
import ai.quadratic.api.dto.EquationRequest;
import ai.quadratic.api.dto.NeuralResponse;
import ai.quadratic.api.dto.NeuralStatusResponse;
import ai.quadratic.api.dto.TrainingResponse;
import ai.quadratic.api.service.NeuralService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/neural")
public class NeuralController {
    private final NeuralService neuralService;
    public NeuralController(NeuralService neuralService) {
        this.neuralService = neuralService;
    }
    @GetMapping("/status")
    public ResponseEntity<NeuralStatusResponse> status() {
        return ResponseEntity.ok(neuralService.getStatus());
    }
    @PostMapping("/predict")
    public ResponseEntity<NeuralResponse> predict(@Valid @RequestBody EquationRequest request) {
        try { return ResponseEntity.ok(neuralService.predict(request)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }
    }
    @PostMapping("/train")
    public ResponseEntity<TrainingResponse> train(
            @RequestParam(name = "epochs",      defaultValue = "200")   int epochs,
            @RequestParam(name = "datasetSize", defaultValue = "10000") int datasetSize) {
        return ResponseEntity.ok(neuralService.train(epochs, datasetSize));
    }
}