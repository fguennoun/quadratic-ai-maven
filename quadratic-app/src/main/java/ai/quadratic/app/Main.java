package ai.quadratic.app;

import ai.quadratic.neural.data.DatasetGenerator;
import ai.quadratic.neural.nn.NeuralNetwork;
import ai.quadratic.neural.persistence.ModelSerializer;
import ai.quadratic.neural.training.Trainer;
import ai.quadratic.symbolic.dialogue.DialogueManager;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Point d'entrée principal de QuadraticAI.
 *
 * <p>Lance la Phase 1 (IA symbolique avec dialogue manager)
 * ou la Phase 2 (réseau de neurones) selon l'argument passé.</p>
 *
 * <p><b>Utilisation :</b></p>
 * <pre>
 *   java -jar quadratic-ai-all.jar          → Phase 1 (défaut)
 *   java -jar quadratic-ai-all.jar phase1   → Phase 1 explicite
 *   java -jar quadratic-ai-all.jar phase2   → Phase 2 (réseau neuronal)
 *   java -jar quadratic-ai-all.jar train    → (Re)entraîner le réseau
 * </pre>
 */
public class Main {

    // Hyperparamètres du réseau — modifiez ici pour expérimenter
    private static final int[]   ARCHITECTURE   = {3, 32, 32, 16, 3};
    private static final double  LEARNING_RATE  = 0.001;
    private static final double  DROPOUT_RATE   = 0.1;
    private static final int     DATASET_SIZE   = 10_000;
    private static final String  MODEL_PATH     = "model.json";

    public static void main(String[] args) throws IOException {
        String mode = (args.length > 0) ? args[0].toLowerCase() : "phase1";

        switch (mode) {
            case "phase1"           -> runPhase1();
            case "phase2"           -> runPhase2(false);
            case "train"            -> runPhase2(true);
            default -> {
                System.out.println("Mode inconnu : " + mode);
                System.out.println("Modes disponibles : phase1, phase2, train");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHASE 1 — IA Symbolique avec Dialogue Manager
    // ══════════════════════════════════════════════════════════════════════════

    private static void runPhase1() {
        DialogueManager dialogue = new DialogueManager();
        Scanner scanner = new Scanner(System.in);

        // Message d'accueil initial
        System.out.println(dialogue.handle(""));

        while (!dialogue.isFinished()) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;

            String input    = scanner.nextLine();
            String response = dialogue.handle(input);
            System.out.println();
            System.out.println(response);
            System.out.println();
        }

        scanner.close();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHASE 2 — Réseau de Neurones
    // ══════════════════════════════════════════════════════════════════════════

    private static void runPhase2(boolean forceTrain) throws IOException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   QuadraticAI — Phase 2 : Réseau Neuronal    ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        // Créer le réseau
        NeuralNetwork network = new NeuralNetwork(ARCHITECTURE, LEARNING_RATE, DROPOUT_RATE);
        System.out.println(network.summary(ARCHITECTURE));

        // Charger ou entraîner
        if (!forceTrain && ModelSerializer.modelExists(MODEL_PATH)) {
            System.out.println("Modèle existant trouvé. Chargement...");
            ModelSerializer.load(network, MODEL_PATH);
            System.out.println("Chargement terminé. Prêt pour l'inférence.\n");
        } else {
            System.out.println("Génération du dataset (" + DATASET_SIZE + " exemples)...");
            DatasetGenerator gen     = new DatasetGenerator(42);
            List<DatasetGenerator.Sample> all = gen.generate(DATASET_SIZE);

            int splitIdx = (int) (all.size() * 0.8);
            var train = all.subList(0, splitIdx);
            var val   = all.subList(splitIdx, all.size());
            System.out.printf("Split : %d train / %d validation%n%n", train.size(), val.size());

            // Entraîner
            Trainer trainer = new Trainer(network, Trainer.TrainingConfig.defaults());
            trainer.train(train, val);

            // Sauvegarder
            ModelSerializer.save(network, ARCHITECTURE, MODEL_PATH);
        }

        // Mode inférence interactif
        runInference(network);
    }

    private static void runInference(NeuralNetwork network) {
        System.out.println("\n" + "═".repeat(56));
        System.out.println("  Test interactif — entrez a, b, c pour prédire");
        System.out.println("  (tapez 'q' pour quitter)");
        System.out.println("═".repeat(56) + "\n");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("a > ");
            String line = scanner.nextLine().trim();
            if (line.equals("q") || line.equals("quitter")) break;

            try {
                double a = Double.parseDouble(line);
                System.out.print("b > ");
                double b = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("c > ");
                double c = Double.parseDouble(scanner.nextLine().trim());

                predictAndCompare(network, a, b, c);
            } catch (NumberFormatException e) {
                System.out.println("Valeur invalide. Entrez un nombre décimal.");
            }
        }

        scanner.close();
    }

    private static void predictAndCompare(NeuralNetwork network, double a, double b, double c) {
        // Prédiction du réseau
        double[] input = {
            DatasetGenerator.norm(a, DatasetGenerator.COEFF_RANGE),
            DatasetGenerator.norm(b, DatasetGenerator.COEFF_RANGE),
            DatasetGenerator.norm(c, DatasetGenerator.COEFF_RANGE)
        };

        network.setTrainingMode(false);
        double[] pred = network.forward(input);

        double predX1    = DatasetGenerator.denorm(pred[0], DatasetGenerator.ROOT_RANGE);
        double predX2    = DatasetGenerator.denorm(pred[1], DatasetGenerator.ROOT_RANGE);
        double predDelta = DatasetGenerator.denorm(pred[2], DatasetGenerator.DELTA_RANGE);

        // Solution exacte pour comparaison
        double exactDelta = b * b - 4 * a * c;
        double exactX1, exactX2;
        if (exactDelta >= 0) {
            exactX1 = (-b + Math.sqrt(exactDelta)) / (2 * a);
            exactX2 = (-b - Math.sqrt(exactDelta)) / (2 * a);
        } else {
            exactX1 = -b / (2 * a);
            exactX2 = Math.sqrt(-exactDelta) / (2 * a);
        }

        // Affichage comparatif
        System.out.println("\n┌─────────────────────────────────────────────────┐");
        System.out.printf( "│  Équation : %.2fx² + (%.2f)x + (%.2f) = 0%n", a, b, c);
        System.out.println("├──────────────┬───────────────┬──────────────────┤");
        System.out.println("│              │  Réseau NN    │  Formule exacte  │");
        System.out.println("├──────────────┼───────────────┼──────────────────┤");
        System.out.printf( "│  Δ           │ %13.4f │ %16.4f │%n", predDelta, exactDelta);
        System.out.printf( "│  x₁ (ou Re) │ %13.4f │ %16.4f │%n", predX1, exactX1);
        System.out.printf( "│  x₂ (ou Im) │ %13.4f │ %16.4f │%n", predX2, exactX2);
        System.out.println("└──────────────┴───────────────┴──────────────────┘");
        System.out.printf( "  Erreur |Δ_prédit - Δ_exact| = %.4f%n%n",
            Math.abs(predDelta - exactDelta));
    }
}
