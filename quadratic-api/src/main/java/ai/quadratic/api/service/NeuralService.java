package ai.quadratic.api.service;
import ai.quadratic.api.dto.EquationRequest;
import ai.quadratic.api.dto.NeuralResponse;
import ai.quadratic.api.dto.NeuralStatusResponse;
import ai.quadratic.api.dto.TrainingResponse;
import ai.quadratic.neural.data.DatasetGenerator;
import ai.quadratic.neural.nn.NeuralNetwork;
import ai.quadratic.neural.persistence.ModelSerializer;
import ai.quadratic.neural.training.Trainer;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
@Service
public class NeuralService {
    private static final int[]  ARCHITECTURE  = {3, 32, 32, 16, 3};
    private static final double LEARNING_RATE = 0.001;
    private static final double DROPOUT_RATE  = 0.1;
    private static final String MODEL_PATH    = "model.json";
    private NeuralNetwork network;
    private boolean modelLoaded = false;
    public NeuralService() { loadOrCreate(); }
    private void loadOrCreate() {
        network = new NeuralNetwork(ARCHITECTURE, LEARNING_RATE, DROPOUT_RATE);
        if (Files.exists(Path.of(MODEL_PATH))) {
            try {
                ModelSerializer.load(network, MODEL_PATH);
                modelLoaded = true;
            } catch (IOException e) {
                modelLoaded = false;
            }
        }
    }
    public NeuralStatusResponse getStatus() {
        return new NeuralStatusResponse(modelLoaded, MODEL_PATH, Arrays.toString(ARCHITECTURE),
            modelLoaded
                ? "Modele charge depuis " + MODEL_PATH
                : "Modele non entraine - POST /api/neural/train");
    }
    public NeuralResponse predict(EquationRequest req) {
        double a = req.a(), b = req.b(), c = req.c();
        double[] out = network.forward(new double[]{
            a / DatasetGenerator.COEFF_RANGE,
            b / DatasetGenerator.COEFF_RANGE,
            c / DatasetGenerator.COEFF_RANGE
        });
        double x1    = out[0] * DatasetGenerator.ROOT_RANGE;
        double x2    = out[1] * DatasetGenerator.ROOT_RANGE;
        double delta = out[2] * DatasetGenerator.DELTA_RANGE;
        String type  = delta > 1e-6 ? "TWO_REAL" : delta > -1e-6 ? "ONE_DOUBLE" : "COMPLEX";
        return new NeuralResponse(type, x1, x2, delta, modelLoaded ? 0.85 : 0.40, modelLoaded);
    }
    public synchronized TrainingResponse train(int epochs, int datasetSize) {
        DatasetGenerator gen = new DatasetGenerator(42);
        List<DatasetGenerator.Sample> dataset = gen.generate(datasetSize);
        int split = (int)(dataset.size() * 0.8);
        network = new NeuralNetwork(ARCHITECTURE, LEARNING_RATE, DROPOUT_RATE);
        Trainer.TrainingConfig cfg = new Trainer.TrainingConfig(epochs, 32, 50, 20, 1e-5, null);
        Trainer trainer = new Trainer(network, cfg);
        Trainer.TrainingHistory history = trainer.train(
            dataset.subList(0, split), dataset.subList(split, dataset.size()));
        try {
            ModelSerializer.save(network, ARCHITECTURE, MODEL_PATH);
            modelLoaded = true;
        } catch (IOException e) {
            return new TrainingResponse(false, 0, -1, -1, "Erreur sauvegarde: " + e.getMessage());
        }
        int done = history.getRecords().size();
        return new TrainingResponse(true, done, history.getFinalTrainLoss(), history.getFinalValLoss(),
            "Entrainement termine (" + done + " epochs). Modele sauvegarde.");
    }
}