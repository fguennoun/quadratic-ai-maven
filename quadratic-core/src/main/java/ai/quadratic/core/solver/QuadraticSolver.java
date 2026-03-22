package ai.quadratic.core.solver;

import ai.quadratic.core.model.ParsedEquation;
import ai.quadratic.core.model.Solution;

/**
 * Solveur mathématique pour les équations du 2ème degré.
 *
 * <p><b>Algorithme :</b></p>
 * <ol>
 *   <li>Calcul du discriminant : Δ = b² - 4ac</li>
 *   <li>Branchement selon le signe de Δ</li>
 *   <li>Application des formules correspondantes</li>
 * </ol>
 *
 * <p><b>Analogie LLM :</b> ce solveur joue le rôle du "forward pass" de la
 * Phase 1. Dans la Phase 2, ce rôle est tenu par {@code NeuralNetwork.forward()}.
 * Le résultat final (la Solution) est équivalent aux "logits" d'un LLM
 * avant le décodage en texte.</p>
 */
public class QuadraticSolver {

    /** Tolérance numérique pour comparer Δ à 0 */
    private static final double EPSILON = 1e-10;

    /**
     * Résout l'équation représentée par {@code equation}.
     *
     * @param equation l'équation parsée (a ≠ 0 garanti par le record)
     * @return une instance de {@link Solution} représentant le résultat
     */
    public Solution solve(ParsedEquation equation) {
        double a = equation.a();
        double b = equation.b();
        double c = equation.c();

        double delta = computeDelta(a, b, c);

        if (delta > EPSILON) {
            // Δ > 0 → deux racines réelles distinctes
            double sqrtDelta = Math.sqrt(delta);
            double x1 = (-b + sqrtDelta) / (2 * a);
            double x2 = (-b - sqrtDelta) / (2 * a);
            return new Solution.TwoRealRoots(x1, x2, delta);

        } else if (Math.abs(delta) <= EPSILON) {
            // Δ = 0 → une racine double
            double x0 = -b / (2 * a);
            return new Solution.OneDoubleRoot(x0, 0.0);

        } else {
            // Δ < 0 → racines complexes conjuguées
            double realPart = -b / (2 * a);
            double imagPart = Math.sqrt(-delta) / (2 * Math.abs(a));
            return new Solution.ComplexRoots(realPart, imagPart, delta);
        }
    }

    /**
     * Calcule le discriminant Δ = b² - 4ac.
     *
     * <p>Le discriminant est la clé de toute l'analyse :
     * son signe détermine entièrement le type de solution.</p>
     */
    public double computeDelta(double a, double b, double c) {
        return b * b - 4.0 * a * c;
    }

    /**
     * Vérifie une racine réelle en substituant x dans ax² + bx + c.
     * Le résultat devrait être ≈ 0 pour une racine exacte.
     *
     * @param equation l'équation originale
     * @param x        la racine à vérifier
     * @return la valeur de f(x) = ax² + bx + c
     */
    public double verify(ParsedEquation equation, double x) {
        return equation.a() * x * x + equation.b() * x + equation.c();
    }
}
