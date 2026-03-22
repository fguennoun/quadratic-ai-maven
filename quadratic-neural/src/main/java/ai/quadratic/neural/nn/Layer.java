package ai.quadratic.neural.nn;

import java.util.Random;

/**
 * Couche de neurones du réseau MLP.
 *
 * <p>Une couche contient N neurones recevant tous les mêmes entrées.
 * Le forward pass calcule les N sorties en parallèle (séquentiellement en Java).</p>
 *
 * <p><b>Dropout :</b> pendant l'entraînement, une fraction {@code dropoutRate}
 * des neurones est mise à zéro aléatoirement. Cela force le réseau à ne pas
 * dépendre d'un seul chemin, améliorant la généralisation (même principe que
 * dans les Transformers GPT).</p>
 */
public class Layer {

    public final Neuron[] neurons;
    public double[]       outputs;
    public double[]       dropoutMask;

    private double[]      lastInputs;
    private final double  dropoutRate;
    private boolean       trainingMode = true;

    /**
     * Construit une couche de neurones.
     *
     * @param neuronCount      nombre de neurones dans cette couche
     * @param inputsPerNeuron  nombre d'entrées par neurone (= taille de la couche précédente)
     * @param activation       fonction d'activation partagée par tous les neurones
     * @param dropoutRate      taux de dropout [0, 1[ (0 = désactivé)
     * @param rng              générateur aléatoire
     */
    public Layer(int neuronCount, int inputsPerNeuron,
                 Neuron.ActivationFunction activation, double dropoutRate, Random rng) {
        this.dropoutRate = dropoutRate;
        this.neurons = new Neuron[neuronCount];
        this.outputs = new double[neuronCount];
        this.dropoutMask = new double[neuronCount];

        for (int i = 0; i < neuronCount; i++) {
            neurons[i] = new Neuron(inputsPerNeuron, activation, rng);
        }
    }

    /** Constructeur sans dropout (rate = 0.0) */
    public Layer(int neuronCount, int inputsPerNeuron,
                 Neuron.ActivationFunction activation, Random rng) {
        this(neuronCount, inputsPerNeuron, activation, 0.0, rng);
    }

    /**
     * Forward pass avec dropout optionnel.
     *
     * <p>En mode entraînement ({@code trainingMode = true}), certains neurones
     * sont masqués selon {@code dropoutRate}. En inférence, tous les neurones
     * sont actifs mais leurs sorties sont multipliées par (1 - dropoutRate)
     * pour compenser — c'est le "inverted dropout".</p>
     *
     * @param inputs vecteur d'entrées
     * @return vecteur de sorties de cette couche
     */
    public double[] forward(double[] inputs) {
        this.lastInputs = inputs.clone();

        for (int i = 0; i < neurons.length; i++) {
            outputs[i] = neurons[i].forward(inputs);

            if (dropoutRate > 0) {
                if (trainingMode) {
                    // Inverted dropout : masquer avec probabilité dropoutRate
                    // et scaler le reste pour maintenir l'espérance
                    dropoutMask[i] = (Math.random() > dropoutRate)
                        ? 1.0 / (1.0 - dropoutRate)
                        : 0.0;
                    outputs[i] *= dropoutMask[i];
                    neurons[i].output = outputs[i];
                } else {
                    dropoutMask[i] = 1.0; // Inférence : tous les neurones actifs
                }
            } else {
                dropoutMask[i] = 1.0;
            }
        }

        return outputs.clone();
    }

    public double[]  getLastInputs() { return lastInputs; }
    public void      setTrainingMode(boolean training) { this.trainingMode = training; }
    public boolean   isTrainingMode() { return trainingMode; }
}
