package ai.quadratic.neural.persistence;

import ai.quadratic.neural.nn.Layer;
import ai.quadratic.neural.nn.NeuralNetwork;
import ai.quadratic.neural.nn.Neuron;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sérialisation et désérialisation des poids du réseau en JSON.
 *
 * <p><b>Objectif :</b> entraîner une seule fois, sauvegarder les poids,
 * puis charger directement lors des utilisations suivantes.
 * Évite de réentraîner depuis zéro à chaque lancement.</p>
 *
 * <p><b>Format JSON généré :</b></p>
 * <pre>
 * {
 *   "version": "1.0",
 *   "architecture": [3, 32, 32, 16, 3],
 *   "learningRate": 0.001,
 *   "layers": [
 *     {
 *       "neurons": [
 *         {"bias": 0.123, "weights": [0.45, -0.23, 0.78]},
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 * }
 * </pre>
 *
 * <p>Implémentation 100% Java standard — aucune dépendance externe (pas de Jackson).</p>
 */
public class ModelSerializer {

    private static final String VERSION = "1.0";

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    /**
     * Sauvegarde les poids du réseau dans un fichier JSON.
     *
     * <p>Le fichier est encodé en UTF-8 avec BOM pour compatibilité Windows.</p>
     *
     * @param network      le réseau entraîné à sauvegarder
     * @param architecture tableau des tailles de couches (ex: {3,32,32,16,3})
     * @param outputPath   chemin du fichier de destination (ex: "model.json")
     * @throws IOException si l'écriture échoue
     */
    public static void save(NeuralNetwork network, int[] architecture, String outputPath)
            throws IOException {

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"").append(VERSION).append("\",\n");
        json.append("  \"architecture\": ").append(Arrays.toString(architecture)).append(",\n");
        json.append("  \"learningRate\": ").append(network.getLearningRate()).append(",\n");
        json.append("  \"parameters\": ").append(network.countParameters()).append(",\n");
        json.append("  \"layers\": [\n");

        for (int l = 0; l < network.getLayerCount(); l++) {
            Layer layer = network.getLayer(l);
            json.append("    {\n");
            json.append("      \"neurons\": [\n");

            for (int n = 0; n < layer.neurons.length; n++) {
                Neuron neuron = layer.neurons[n];
                json.append("        {\n");
                json.append("          \"bias\": ").append(neuron.bias).append(",\n");
                json.append("          \"weights\": ").append(Arrays.toString(neuron.weights)).append("\n");
                json.append("        }");
                if (n < layer.neurons.length - 1) json.append(",");
                json.append("\n");
            }

            json.append("      ]\n");
            json.append("    }");
            if (l < network.getLayerCount() - 1) json.append(",");
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.writeString(Path.of(outputPath), json.toString(), StandardCharsets.UTF_8);
        System.out.printf("Modèle sauvegardé : %s (%,d paramètres)%n",
            outputPath, network.countParameters());
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    /**
     * Charge les poids d'un réseau depuis un fichier JSON et les applique
     * à un réseau déjà instancié avec la même architecture.
     *
     * <p><b>Utilisation :</b></p>
     * <pre>
     *   NeuralNetwork network = new NeuralNetwork(new int[]{3,32,32,16,3}, 0.001);
     *   ModelSerializer.load(network, "model.json");
     *   // Le réseau est prêt pour l'inférence, sans réentraînement.
     * </pre>
     *
     * @param network    réseau cible (même architecture que lors de la sauvegarde)
     * @param inputPath  chemin du fichier JSON à charger
     * @throws IOException              si la lecture échoue
     * @throws IllegalArgumentException si le fichier est corrompu ou incompatible
     */
    public static void load(NeuralNetwork network, String inputPath)
            throws IOException {

        if (!Files.exists(Path.of(inputPath))) {
            throw new IOException("Fichier modèle introuvable : " + inputPath);
        }

        String json = Files.readString(Path.of(inputPath), StandardCharsets.UTF_8);

        // Vérifier la version
        String version = extractString(json, "version");
        if (!VERSION.equals(version)) {
            throw new IllegalArgumentException(
                "Version de modèle incompatible : " + version + " (attendu : " + VERSION + ")");
        }

        // Parser les couches
        Pattern layerPattern  = Pattern.compile("\"neurons\":\\s*\\[(.*?)\\]",
            Pattern.DOTALL);
        Pattern neuronPattern = Pattern.compile(
            "\"bias\":\\s*([\\-\\d.E]+),\\s*\"weights\":\\s*\\[([^\\]]+)\\]",
            Pattern.DOTALL);

        Matcher layerMatcher = layerPattern.matcher(json);
        int layerIdx = 0;

        while (layerMatcher.find() && layerIdx < network.getLayerCount()) {
            Layer layer = network.getLayer(layerIdx);
            Matcher neuronMatcher = neuronPattern.matcher(layerMatcher.group(1));
            int neuronIdx = 0;

            while (neuronMatcher.find() && neuronIdx < layer.neurons.length) {
                Neuron neuron = layer.neurons[neuronIdx];
                neuron.bias = Double.parseDouble(neuronMatcher.group(1).trim());

                String[] weightStrs = neuronMatcher.group(2).trim().split(",");
                for (int w = 0; w < Math.min(weightStrs.length, neuron.weights.length); w++) {
                    neuron.weights[w] = Double.parseDouble(weightStrs[w].trim());
                }
                neuronIdx++;
            }
            layerIdx++;
        }

        System.out.printf("Modèle chargé depuis : %s (%,d paramètres)%n",
            inputPath, network.countParameters());
    }

    /** Vérifie si un fichier de modèle existe */
    public static boolean modelExists(String path) {
        return Files.exists(Path.of(path));
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────

    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\":\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }
}
