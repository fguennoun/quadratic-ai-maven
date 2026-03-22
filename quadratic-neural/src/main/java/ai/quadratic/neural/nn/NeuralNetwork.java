package ai.quadratic.neural.nn;

import ai.quadratic.neural.optimizer.AdamOptimizer;

import java.util.Random;

/**
 * Réseau de neurones MLP (Multi-Layer Perceptron) avec backpropagation et Adam.
 *
 * <p><b>Architecture typique pour les équations du 2ème degré :</b></p>
 * <pre>
 *   [3] → [32] → [32] → [16] → [3]
 *    ↑      ↑      ↑      ↑      ↑
 *   a,b,c  ReLU  ReLU  ReLU  x1,x2,Δ
 * </pre>
 *
 * <p><b>Pipeline complet :</b></p>
 * <ol>
 *   <li>Forward pass  : propagation des entrées couche par couche</li>
 *   <li>Loss          : MSE = (prédiction - cible)²</li>
 *   <li>Backward pass : calcul des deltas via la règle de la chaîne</li>
 *   <li>Update        : mise à jour des poids via Adam</li>
 * </ol>
 *
 * <p><b>Analogie LLM :</b> ce réseau est un Transformer simplifié.
 * GPT-4 empile 96 couches Transformer avec attention multi-têtes.
 * Notre MLP empile 3-4 couches denses — même principe, échelle différente.</p>
 */
public class NeuralNetwork {

    private final Layer[]         layers;
    private final int             inputSize;
    private final AdamOptimizer   optimizer;

    /**
     * Construit le réseau selon les tailles de couches spécifiées.
     *
     * @param layerSizes   ex: {3, 32, 32, 16, 3} → entrée=3, 3 couches cachées, sortie=3
     * @param learningRate taux d'apprentissage Adam (recommandé : 0.001)
     * @param dropoutRate  taux de dropout sur les couches cachées (0 = désactivé)
     */
    public NeuralNetwork(int[] layerSizes, double learningRate, double dropoutRate) {
        this.inputSize = layerSizes[0];
        this.optimizer = new AdamOptimizer(learningRate);
        this.layers    = new Layer[layerSizes.length - 1];

        Random rng = new Random(42); // Seed fixe → reproductibilité

        for (int i = 0; i < layers.length; i++) {
            boolean isOutput = (i == layers.length - 1);
            Neuron.ActivationFunction activation = isOutput
                ? Neuron.ActivationFunction.LINEAR  // Régression → sortie linéaire
                : Neuron.ActivationFunction.RELU;

            double rate = isOutput ? 0.0 : dropoutRate; // Pas de dropout sur la sortie
            layers[i] = new Layer(layerSizes[i + 1], layerSizes[i], activation, rate, rng);
        }
    }

    /** Constructeur sans dropout */
    public NeuralNetwork(int[] layerSizes, double learningRate) {
        this(layerSizes, learningRate, 0.0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORWARD PASS
    //  Propage l'entrée de couche en couche jusqu'à la sortie.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Calcule la prédiction du réseau pour une entrée donnée.
     *
     * @param input vecteur d'entrée (normalisé dans [-1, 1])
     * @return vecteur de sortie (à dénormaliser après)
     */
    public double[] forward(double[] input) {
        double[] current = input;
        for (Layer layer : layers) {
            current = layer.forward(current);
        }
        return current;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BACKWARD PASS — Backpropagation
    //
    //  Algorithme fondamental du Deep Learning (Rumelhart et al., 1986).
    //  Calcule ∂Loss/∂w pour chaque poids via la règle de la chaîne.
    //
    //  Étape 1 : delta couche sortie = (output - target) × f'(z)
    //  Étape 2 : delta couche l = (Σ delta_l+1 × w_l+1) × f'(z_l)
    //  Étape 3 : Adam met à jour les poids avec ces deltas
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Effectue un pas d'entraînement complet sur un exemple.
     *
     * @param input  vecteur d'entrée normalisé
     * @param target vecteur cible normalisé
     * @return la loss MSE pour cet exemple
     */
    public double trainStep(double[] input, double[] target) {
        // 1. Forward pass
        double[] output = forward(input);

        // 2. Calcul de la loss (MSE)
        double loss = computeMSE(output, target);

        // 3. Deltas de la couche de sortie : δ = (ŷ - y) × f'(z)
        Layer outputLayer = layers[layers.length - 1];
        for (int i = 0; i < outputLayer.neurons.length; i++) {
            Neuron n = outputLayer.neurons[i];
            n.delta = (output[i] - target[i]) * n.activationDerivative();
        }

        // 4. Rétropropagation dans les couches cachées
        //    δ_l = (Σ_k δ_{l+1,k} × w_{l+1,k,i}) × f'(z_{l,i})
        for (int l = layers.length - 2; l >= 0; l--) {
            Layer currentLayer = layers[l];
            Layer nextLayer    = layers[l + 1];

            for (int i = 0; i < currentLayer.neurons.length; i++) {
                double errorSum = 0.0;
                for (Neuron nextNeuron : nextLayer.neurons) {
                    // Contribution du neurone i de la couche l au neurone nextNeuron
                    errorSum += nextNeuron.delta * nextNeuron.weights[i];
                }
                // Appliquer le masque dropout dans le backward pass
                double dropoutFactor = currentLayer.dropoutMask[i];
                currentLayer.neurons[i].delta =
                    errorSum * currentLayer.neurons[i].activationDerivative() * dropoutFactor;
            }
        }

        // 5. Mise à jour des poids via Adam
        double[][] layerInputs = gatherLayerInputs(input);
        optimizer.update(layers, layerInputs);

        return loss;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ÉVALUATION (sans mise à jour des poids)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Évalue la loss sur un exemple sans modifier les poids.
     * Utilisé pour le calcul de la validation loss.
     */
    public double evaluate(double[] input, double[] target) {
        setTrainingMode(false);
        double[] output = forward(input);
        setTrainingMode(true);
        return computeMSE(output, target);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════════

    private double computeMSE(double[] output, double[] target) {
        double sum = 0.0;
        for (int i = 0; i < output.length; i++) {
            double diff = output[i] - target[i];
            sum += diff * diff;
        }
        return sum / output.length;
    }

    private double[][] gatherLayerInputs(double[] networkInput) {
        double[][] inputs = new double[layers.length][];
        inputs[0] = networkInput;
        for (int l = 1; l < layers.length; l++) {
            inputs[l] = layers[l - 1].outputs;
        }
        return inputs;
    }

    public void setTrainingMode(boolean training) {
        for (Layer layer : layers) {
            layer.setTrainingMode(training);
        }
    }

    public int     getLayerCount()  { return layers.length; }
    public Layer   getLayer(int i)  { return layers[i]; }
    public int     getInputSize()   { return inputSize; }
    public double  getLearningRate(){ return optimizer.getLearningRate(); }

    /** Compte le nombre total de paramètres apprenables (poids + biais) */
    public int countParameters() {
        int total = 0;
        for (Layer layer : layers) {
            for (Neuron n : layer.neurons) {
                total += n.weights.length + 1; // +1 pour le biais
            }
        }
        return total;
    }

    /** Résumé de l'architecture (comme Keras model.summary()) */
    public String summary(int[] layerSizes) {
        StringBuilder sb = new StringBuilder();
        sb.append("═".repeat(52)).append("\n");
        sb.append(String.format(" %-20s %-15s %s%n", "Couche", "Neurones", "Activation"));
        sb.append("─".repeat(52)).append("\n");
        sb.append(String.format(" %-20s %-15s %s%n", "Entrée", layerSizes[0], "—"));
        for (int i = 0; i < layers.length; i++) {
            boolean isOut = (i == layers.length - 1);
            sb.append(String.format(" %-20s %-15s %s%n",
                isOut ? "Sortie" : "Cachée " + (i + 1),
                layers[i].neurons.length,
                isOut ? "Linear" : "ReLU"));
        }
        sb.append("─".repeat(52)).append("\n");
        sb.append(String.format(" Paramètres totaux : %,d%n", countParameters()));
        sb.append("═".repeat(52)).append("\n");
        return sb.toString();
    }
}
