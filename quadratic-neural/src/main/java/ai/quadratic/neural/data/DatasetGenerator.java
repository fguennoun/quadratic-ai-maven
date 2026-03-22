package ai.quadratic.neural.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Générateur de dataset synthétique pour l'entraînement du réseau.
 *
 * <p><b>Stratégie :</b> générer les racines x₁, x₂ d'abord, puis calculer
 * les coefficients a, b, c via a(x-x₁)(x-x₂) = ax² - a(x₁+x₂)x + ax₁x₂.
 * Cette approche garantit des équations avec solution connue exacte.</p>
 *
 * <p><b>Distribution du dataset :</b></p>
 * <ul>
 *   <li>60 % → Δ &gt; 0 (deux racines réelles)</li>
 *   <li>20 % → Δ = 0 (racine double)</li>
 *   <li>20 % → Δ &lt; 0 (racines complexes)</li>
 * </ul>
 *
 * <p><b>Normalisation :</b> toutes les valeurs sont normalisées dans [-1, 1]
 * avant d'être présentées au réseau. Cela stabilise l'entraînement et
 * accélère la convergence (même principe que le Layer Normalization
 * utilisé dans les Transformers).</p>
 */
public class DatasetGenerator {

    // ── Plages de normalisation ──────────────────────────────────────────────
    public static final double COEFF_RANGE = 10.0;  // Coefficients dans [-10, 10]
    public static final double ROOT_RANGE  = 20.0;  // Racines dans [-20, 20]
    public static final double DELTA_RANGE = 400.0; // Δ dans [-400, 400]

    /**
     * Exemple d'entraînement : paire (entrée normalisée, cible normalisée).
     *
     * @param input   [a_norm, b_norm, c_norm]
     * @param target  [x1_norm, x2_norm, delta_norm]
     * @param a       valeur réelle de a (pour affichage/debug)
     * @param b       valeur réelle de b
     * @param c       valeur réelle de c
     * @param x1      racine 1 réelle (ou partie réelle si complexe)
     * @param x2      racine 2 réelle (ou partie imaginaire si complexe)
     * @param delta   discriminant réel
     */
    public record Sample(
        double[] input, double[] target,
        double a, double b, double c,
        double x1, double x2, double delta
    ) {}

    private final Random rng;

    public DatasetGenerator(long seed) {
        this.rng = new Random(seed);
    }

    /**
     * Génère un dataset équilibré et mélangé.
     *
     * @param count nombre total d'exemples à générer
     * @return liste d'exemples mélangés aléatoirement
     */
    public List<Sample> generate(int count) {
        List<Sample> samples = new ArrayList<>(count);

        int twoRoots = (int) (count * 0.60);
        int oneRoot  = (int) (count * 0.20);
        int complex  = count - twoRoots - oneRoot;

        // Cas Δ > 0 : générer x1, x2, puis déduire a, b, c
        for (int i = 0; i < twoRoots; i++) {
            double x1 = randInRange(-ROOT_RANGE / 2, ROOT_RANGE / 2);
            double x2 = randInRange(-ROOT_RANGE / 2, ROOT_RANGE / 2);
            double a  = randNonZero();
            double b  = -a * (x1 + x2);
            double c  = a * x1 * x2;
            if (isInCoeffRange(a, b, c)) {
                samples.add(buildSample(a, b, c));
            }
        }

        // Cas Δ = 0 : x1 = x2 = x0
        for (int i = 0; i < oneRoot; i++) {
            double x0 = randInRange(-8, 8);
            double a  = randNonZero();
            double b  = -2 * a * x0;
            double c  = a * x0 * x0;
            if (isInCoeffRange(a, b, c)) {
                samples.add(buildSample(a, b, c));
            }
        }

        // Cas Δ < 0 : choisir a, c positifs, b petit pour forcer Δ < 0
        for (int i = 0; i < complex; i++) {
            double a    = Math.abs(randNonZero());
            double c    = Math.abs(randInRange(0.5, COEFF_RANGE));
            double maxB = Math.sqrt(4 * a * c) * 0.8; // 80% du seuil
            double b    = randInRange(-maxB, maxB);
            if (isInCoeffRange(a, b, c)) {
                samples.add(buildSample(a, b, c));
            }
        }

        Collections.shuffle(samples, rng);
        return samples;
    }

    // ── Construction d'un exemple ────────────────────────────────────────────

    private Sample buildSample(double a, double b, double c) {
        double delta = b * b - 4 * a * c;
        double x1, x2;

        if (delta >= 0) {
            x1 = (-b + Math.sqrt(delta)) / (2 * a);
            x2 = (-b - Math.sqrt(delta)) / (2 * a);
        } else {
            // Pour les complexes : x1 = partie réelle, x2 = partie imaginaire
            x1 = -b / (2 * a);
            x2 = Math.sqrt(-delta) / (2 * a);
        }

        double[] input  = { norm(a, COEFF_RANGE), norm(b, COEFF_RANGE), norm(c, COEFF_RANGE) };
        double[] target = { norm(x1, ROOT_RANGE),  norm(x2, ROOT_RANGE),  norm(delta, DELTA_RANGE) };

        return new Sample(input, target, a, b, c, x1, x2, delta);
    }

    // ── Normalisation ────────────────────────────────────────────────────────

    /** Normalise dans [-1, 1] avec clamp */
    public static double norm(double value, double range) {
        return Math.max(-1.0, Math.min(1.0, value / range));
    }

    /** Dénormalise vers la valeur réelle */
    public static double denorm(double value, double range) {
        return value * range;
    }

    // ── Utilitaires privés ───────────────────────────────────────────────────

    private double randInRange(double min, double max) {
        return min + rng.nextDouble() * (max - min);
    }

    private double randNonZero() {
        double v;
        do { v = randInRange(-COEFF_RANGE, COEFF_RANGE); }
        while (Math.abs(v) < 0.2);
        return v;
    }

    private boolean isInCoeffRange(double a, double b, double c) {
        return Math.abs(a) <= COEFF_RANGE
            && Math.abs(b) <= COEFF_RANGE
            && Math.abs(c) <= COEFF_RANGE;
    }
}
