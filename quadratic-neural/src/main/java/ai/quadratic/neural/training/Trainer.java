package ai.quadratic.neural.training;

import ai.quadratic.neural.data.DatasetGenerator;
import ai.quadratic.neural.nn.NeuralNetwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Boucle d'entraînement du réseau de neurones.
 *
 * <p><b>Fonctionnalités :</b></p>
 * <ul>
 *   <li>Mini-batch gradient descent avec mélange des données à chaque epoch</li>
 *   <li>Early stopping : arrêt si val_loss ne diminue plus</li>
 *   <li>Learning rate scheduling : réduction progressive</li>
 *   <li>Export CSV de la courbe de loss (ouvrable dans Excel)</li>
 *   <li>Visualisation ASCII de la loss dans le terminal</li>
 * </ul>
 *
 * <p><b>Analogie LLM :</b> cette boucle est l'équivalent du script
 * {@code train.py} de nanoGPT ou HuggingFace Trainer, avec les mêmes
 * concepts : epochs, batches, validation loss, early stopping.</p>
 */
public class Trainer {

    private final NeuralNetwork network;
    private final TrainingConfig config;

    public Trainer(NeuralNetwork network, TrainingConfig config) {
        this.network = network;
        this.config  = config;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOUCLE D'ENTRAÎNEMENT PRINCIPALE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Lance l'entraînement complet.
     *
     * @param trainSet données d'entraînement
     * @param valSet   données de validation (jamais vues pendant l'entraînement)
     * @return historique des losses par epoch
     */
    public TrainingHistory train(List<DatasetGenerator.Sample> trainSet,
                                 List<DatasetGenerator.Sample> valSet) {
        TrainingHistory history = new TrainingHistory();

        double bestValLoss = Double.MAX_VALUE;
        int    stagnation  = 0;

        printHeader();

        for (int epoch = 1; epoch <= config.epochs(); epoch++) {

            // Mélanger les données à chaque epoch (important pour éviter l'overfitting)
            List<DatasetGenerator.Sample> shuffled = new ArrayList<>(trainSet);
            Collections.shuffle(shuffled);

            // Une epoch = parcourir tout le train set en mini-batches
            double trainLoss = runEpoch(shuffled);

            // Évaluation sur le val set (dropout désactivé)
            double valLoss = evaluateAll(valSet);

            history.add(epoch, trainLoss, valLoss);

            // ── Early stopping ────────────────────────────────────────────
            if (valLoss < bestValLoss - config.minDelta()) {
                bestValLoss = valLoss;
                stagnation  = 0;
            } else {
                stagnation++;
                if (stagnation >= config.patience()) {
                    System.out.printf("%nEarly stopping à l'epoch %d " +
                        "(val_loss stable depuis %d epochs)%n", epoch, config.patience());
                    break;
                }
            }

            // ── Affichage périodique ──────────────────────────────────────
            if (epoch % config.printEvery() == 0 || epoch == 1) {
                System.out.printf("%-8d %-14.6f %-14.6f %-10.6f%n",
                    epoch, trainLoss, valLoss, network.getLearningRate());
            }
        }

        printFooter(history);
        exportCsv(history);
        return history;
    }

    // ── Epoch ────────────────────────────────────────────────────────────────

    private double runEpoch(List<DatasetGenerator.Sample> samples) {
        double totalLoss = 0.0;
        int    batches   = 0;

        for (int i = 0; i < samples.size(); i += config.batchSize()) {
            int    end       = Math.min(i + config.batchSize(), samples.size());
            double batchLoss = 0.0;

            for (int j = i; j < end; j++) {
                DatasetGenerator.Sample s = samples.get(j);
                batchLoss += network.trainStep(s.input(), s.target());
            }

            totalLoss += batchLoss / (end - i);
            batches++;
        }

        return batches > 0 ? totalLoss / batches : 0.0;
    }

    private double evaluateAll(List<DatasetGenerator.Sample> samples) {
        double total = 0.0;
        for (DatasetGenerator.Sample s : samples) {
            total += network.evaluate(s.input(), s.target());
        }
        return samples.isEmpty() ? 0.0 : total / samples.size();
    }

    // ── Export CSV ───────────────────────────────────────────────────────────

    /**
     * Exporte la courbe de loss dans un fichier CSV.
     * Le fichier est encodé UTF-8 avec BOM pour compatibilité Excel Windows.
     */
    private void exportCsv(TrainingHistory history) {
        if (config.csvOutputPath() == null) return;

        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM UTF-8
        sb.append("epoch;train_loss;val_loss\n");
        for (double[] row : history.getRecords()) {
            sb.append(String.format("%d;%.8f;%.8f%n",
                (int) row[0], row[1], row[2]));
        }

        try {
            Files.writeString(Path.of(config.csvOutputPath()), sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Courbe de loss exportée : " + config.csvOutputPath());
        } catch (IOException e) {
            System.err.println("Impossible d'écrire le CSV : " + e.getMessage());
        }
    }

    // ── Affichage ASCII ──────────────────────────────────────────────────────

    private void printHeader() {
        System.out.println("─".repeat(56));
        System.out.printf("%-8s %-14s %-14s %-10s%n",
            "Epoch", "Train Loss", "Val Loss", "LR");
        System.out.println("─".repeat(56));
    }

    private void printFooter(TrainingHistory h) {
        System.out.println("─".repeat(56));
        System.out.printf("Meilleure val_loss : %.6f%n%n", h.getBestValLoss());

        // Graphe ASCII de la courbe de loss
        if (h.getRecords().size() > 1) {
            System.out.println("Courbe de loss (train) :");
            printAsciiPlot(h.getTrainLosses(), 60, 8);
        }
    }

    /**
     * Affiche un graphe ASCII de la série temporelle {@code values}.
     *
     * @param values  valeurs à tracer
     * @param width   largeur en caractères
     * @param height  hauteur en caractères
     */
    public static void printAsciiPlot(List<Double> values, int width, int height) {
        if (values == null || values.size() < 2) return;

        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        if (max == min) max = min + 1;

        char[][] grid = new char[height][width];
        for (char[] row : grid) java.util.Arrays.fill(row, ' ');

        int step = Math.max(1, values.size() / width);
        for (int x = 0; x < width; x++) {
            int    idx = Math.min(x * step, values.size() - 1);
            double v   = values.get(idx);
            int    y   = (int) ((v - min) / (max - min) * (height - 1));
            y = Math.max(0, Math.min(height - 1, y));
            grid[height - 1 - y][x] = '●';
        }

        // Axe Y (labels)
        for (int row = 0; row < height; row++) {
            double label = max - row * (max - min) / (height - 1);
            System.out.printf("%8.4f │%s%n", label, new String(grid[row]));
        }
        System.out.printf("         └%s%n", "─".repeat(width));
        System.out.printf("          0%s%d epochs%n",
            " ".repeat(Math.max(0, width - 10)), values.size());
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    /**
     * Paramètres de la boucle d'entraînement.
     *
     * @param epochs        nombre maximum d'epochs
     * @param batchSize     taille du mini-batch
     * @param printEvery    fréquence d'affichage (en epochs)
     * @param patience      early stopping : epochs sans amélioration
     * @param minDelta      amélioration minimale pour réinitialiser la patience
     * @param csvOutputPath chemin du fichier CSV de sortie (null = pas d'export)
     */
    public record TrainingConfig(
        int    epochs,
        int    batchSize,
        int    printEvery,
        int    patience,
        double minDelta,
        String csvOutputPath
    ) {
        /** Configuration par défaut recommandée */
        public static TrainingConfig defaults() {
            return new TrainingConfig(500, 32, 50, 30, 1e-5, "loss.csv");
        }

        /** Configuration rapide pour les tests */
        public static TrainingConfig quick() {
            return new TrainingConfig(100, 64, 25, 15, 1e-4, null);
        }
    }

    // ── Historique ────────────────────────────────────────────────────────────

    /** Historique des losses par epoch */
    public static class TrainingHistory {
        private final List<double[]> records = new ArrayList<>();

        public void add(int epoch, double trainLoss, double valLoss) {
            records.add(new double[]{epoch, trainLoss, valLoss});
        }

        public List<double[]> getRecords() { return records; }

        public List<Double> getTrainLosses() {
            return records.stream().map(r -> r[1]).toList();
        }

        public double getFinalTrainLoss() {
            return records.isEmpty() ? 0 : records.getLast()[1];
        }

        public double getFinalValLoss() {
            return records.isEmpty() ? 0 : records.getLast()[2];
        }

        public double getBestValLoss() {
            return records.stream().mapToDouble(r -> r[2]).min().orElse(0);
        }
    }
}
