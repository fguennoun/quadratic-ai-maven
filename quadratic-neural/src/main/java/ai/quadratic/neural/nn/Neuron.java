package ai.quadratic.neural.nn;

import java.util.Random;

/**
 * Neurone artificiel avec support des optimiseurs SGD et Adam.
 *
 * <p>Un neurone calcule : output = activation(Σ(wᵢ·xᵢ) + b)</p>
 *
 * <p><b>États Adam :</b> chaque neurone conserve ses propres vecteurs
 * de premier et second moment (m, v) pour l'optimiseur Adam.
 * C'est la même stratégie que PyTorch ({@code optim.Adam}) :
 * chaque paramètre a son propre historique de gradient.</p>
 */
public class Neuron {

    // ── Paramètres apprenables ────────────────────────────────────────────────
    public double[] weights;
    public double   bias;

    // ── Sorties pour forward/backward ────────────────────────────────────────
    public double output;     // Sortie après activation : a = f(z)
    public double rawOutput;  // Somme pondérée avant activation : z = Wx + b
    public double delta;      // Gradient local δ (calculé en backprop)

    // ── États de l'optimiseur Adam (un par poids + un pour le biais) ─────────
    public double[] mW;   // Premier moment des poids  (moyenne exponentielle des gradients)
    public double[] vW;   // Second moment des poids   (variance exponentielle)
    public double   mB;   // Premier moment du biais
    public double   vB;   // Second moment du biais
    public int      t;    // Compteur de pas (step counter)

    private final ActivationFunction activation;

    /**
     * Construit un neurone avec initialisation Xavier/Glorot.
     *
     * <p>L'initialisation Xavier règle les poids à une échelle √(2/n),
     * ce qui évite l'explosion ou la disparition du gradient au début
     * de l'entraînement.</p>
     *
     * @param inputCount  nombre d'entrées (poids)
     * @param activation  fonction d'activation à utiliser
     * @param rng         générateur aléatoire (seed fixe pour reproductibilité)
     */
    public Neuron(int inputCount, ActivationFunction activation, Random rng) {
        this.activation = activation;
        this.weights = new double[inputCount];
        this.mW = new double[inputCount];
        this.vW = new double[inputCount];

        // Xavier initialization
        double scale = Math.sqrt(2.0 / inputCount);
        for (int i = 0; i < inputCount; i++) {
            weights[i] = rng.nextGaussian() * scale;
        }
        this.bias = 0.01; // Petit biais positif pour éviter les neurones morts au départ
    }

    /**
     * Forward pass : calcule z = Wx + b, puis output = activation(z).
     *
     * @param inputs vecteur d'entrées de la couche précédente
     * @return la sortie du neurone après activation
     */
    public double forward(double[] inputs) {
        rawOutput = bias;
        for (int i = 0; i < weights.length; i++) {
            rawOutput += weights[i] * inputs[i];
        }
        output = activation.apply(rawOutput);
        return output;
    }

    /**
     * Dérivée de la fonction d'activation en rawOutput.
     * Utilisée lors du calcul des deltas en backpropagation.
     */
    public double activationDerivative() {
        return activation.derivative(rawOutput, output);
    }

    // ── Fonctions d'activation ────────────────────────────────────────────────

    /**
     * Fonctions d'activation disponibles.
     *
     * <p>Chacune implémente f(z) et f'(z) — la dérivée est nécessaire
     * pour la rétropropagation (règle de la chaîne).</p>
     */
    public enum ActivationFunction {

        /**
         * ReLU : max(0, z) — Rectified Linear Unit.
         * Avantages : simple, rapide, pas de saturation pour z > 0.
         * Inconvénient : "neurones morts" si z reste négatif.
         */
        RELU {
            @Override public double apply(double z)                  { return Math.max(0, z); }
            @Override public double derivative(double z, double out) { return z > 0 ? 1.0 : 0.0; }
        },

        /**
         * Linéaire (identité) : f(z) = z.
         * Utilisé en couche de sortie pour la régression (prédire des valeurs continues).
         */
        LINEAR {
            @Override public double apply(double z)                  { return z; }
            @Override public double derivative(double z, double out) { return 1.0; }
        },

        /**
         * Tanh : sortie dans [-1, 1], centrée en 0.
         * Alternative à ReLU pour les couches cachées.
         */
        TANH {
            @Override public double apply(double z)                  { return Math.tanh(z); }
            @Override public double derivative(double z, double out) { return 1.0 - out * out; }
        };

        public abstract double apply(double z);
        public abstract double derivative(double z, double out);
    }
}
