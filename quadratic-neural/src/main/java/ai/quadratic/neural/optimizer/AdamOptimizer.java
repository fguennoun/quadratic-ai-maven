package ai.quadratic.neural.optimizer;

import ai.quadratic.neural.nn.Layer;
import ai.quadratic.neural.nn.Neuron;

/**
 * Optimiseur Adam (Adaptive Moment Estimation).
 *
 * <p><b>Pourquoi Adam plutôt que SGD ?</b></p>
 * <ul>
 *   <li>SGD utilise un learning rate global identique pour tous les poids.</li>
 *   <li>Adam adapte le learning rate de chaque poids individuellement,
 *       en maintenant une moyenne mobile du gradient (m) et de sa variance (v).</li>
 *   <li>Résultat : convergence 3-5× plus rapide, moins sensible au choix du LR.</li>
 *   <li>Adam est l'optimiseur utilisé par tous les LLMs modernes (GPT, BERT, Claude...).</li>
 * </ul>
 *
 * <p><b>Formules :</b></p>
 * <pre>
 *   m_t = β₁·m_{t-1} + (1-β₁)·g_t          // Premier moment (moyenne)
 *   v_t = β₂·v_{t-1} + (1-β₂)·g_t²         // Second moment (variance)
 *
 *   m̂_t = m_t / (1 - β₁^t)                  // Correction du biais
 *   v̂_t = v_t / (1 - β₂^t)
 *
 *   w_t = w_{t-1} - η · m̂_t / (√v̂_t + ε)   // Mise à jour
 * </pre>
 *
 * <p>Référence : Kingma & Ba, "Adam: A Method for Stochastic Optimization" (2015).</p>
 */
public class AdamOptimizer {

    // ── Hyperparamètres par défaut (valeurs recommandées dans le paper original) ──
    private final double learningRate;
    private final double beta1;   // Décroissance du premier moment  (défaut : 0.9)
    private final double beta2;   // Décroissance du second moment   (défaut : 0.999)
    private final double epsilon; // Stabilité numérique (évite la division par 0)

    /**
     * Crée un optimiseur Adam avec les hyperparamètres par défaut.
     *
     * @param learningRate taux d'apprentissage (η), ex : 0.001
     */
    public AdamOptimizer(double learningRate) {
        this(learningRate, 0.9, 0.999, 1e-8);
    }

    /**
     * Crée un optimiseur Adam avec des hyperparamètres personnalisés.
     */
    public AdamOptimizer(double learningRate, double beta1, double beta2, double epsilon) {
        this.learningRate = learningRate;
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.epsilon = epsilon;
    }

    /**
     * Met à jour les poids de toutes les couches du réseau.
     *
     * <p>Appelé après chaque exemple (ou mini-batch) pendant l'entraînement.
     * Les gradients (deltas) doivent avoir été calculés par la backpropagation avant.</p>
     *
     * @param layers      couches du réseau (dans l'ordre input→output)
     * @param layerInputs entrées de chaque couche (sauvegardées pendant le forward pass)
     */
    public void update(Layer[] layers, double[][] layerInputs) {
        for (int l = 0; l < layers.length; l++) {
            double[] inputs = layerInputs[l];

            for (Neuron n : layers[l].neurons) {
                n.t++; // Incrémenter le compteur de pas global du neurone

                double beta1t = Math.pow(beta1, n.t);
                double beta2t = Math.pow(beta2, n.t);

                // ── Mise à jour de chaque poids ───────────────────────────
                for (int j = 0; j < n.weights.length; j++) {
                    double g = n.delta * inputs[j]; // Gradient du poids w_j

                    // Mise à jour des moments
                    n.mW[j] = beta1 * n.mW[j] + (1.0 - beta1) * g;
                    n.vW[j] = beta2 * n.vW[j] + (1.0 - beta2) * g * g;

                    // Correction du biais (importance surtout en début d'entraînement)
                    double mHat = n.mW[j] / (1.0 - beta1t);
                    double vHat = n.vW[j] / (1.0 - beta2t);

                    // Mise à jour du poids
                    n.weights[j] -= learningRate * mHat / (Math.sqrt(vHat) + epsilon);
                }

                // ── Mise à jour du biais ──────────────────────────────────
                n.mB = beta1 * n.mB + (1.0 - beta1) * n.delta;
                n.vB = beta2 * n.vB + (1.0 - beta2) * n.delta * n.delta;
                double mHatB = n.mB / (1.0 - beta1t);
                double vHatB = n.vB / (1.0 - beta2t);
                n.bias -= learningRate * mHatB / (Math.sqrt(vHatB) + epsilon);
            }
        }
    }

    public double getLearningRate() { return learningRate; }
}
