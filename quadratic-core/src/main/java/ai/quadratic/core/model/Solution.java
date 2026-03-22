package ai.quadratic.core.model;

/**
 * Résultat de la résolution d'une équation du 2ème degré.
 *
 * <p>Interface scellée (sealed) : seuls 3 cas sont possibles,
 * ce qui permet au compilateur de vérifier l'exhaustivité des switch.</p>
 *
 * <ul>
 *   <li>{@link TwoRealRoots}  — Δ &gt; 0 : deux racines réelles distinctes</li>
 *   <li>{@link OneDoubleRoot} — Δ = 0 : une racine double</li>
 *   <li>{@link ComplexRoots}  — Δ &lt; 0 : racines complexes conjuguées</li>
 * </ul>
 */
public sealed interface Solution permits
        Solution.TwoRealRoots,
        Solution.OneDoubleRoot,
        Solution.ComplexRoots {

    /** Retourne le discriminant associé à la solution */
    double delta();

    // ── Implémentations ────────────────────────────────────────────────────────

    /**
     * Δ &gt; 0 : deux racines réelles distinctes.
     * x₁ = (-b + √Δ) / 2a  et  x₂ = (-b - √Δ) / 2a
     */
    record TwoRealRoots(double x1, double x2, double delta) implements Solution {}

    /**
     * Δ = 0 : une racine double (le parabole touche l'axe des x en un seul point).
     * x₀ = -b / 2a
     */
    record OneDoubleRoot(double x0, double delta) implements Solution {}

    /**
     * Δ &lt; 0 : pas de racine réelle, mais deux racines complexes conjuguées.
     * z₁ = realPart + i·imagPart  et  z₂ = realPart - i·imagPart
     */
    record ComplexRoots(double realPart, double imagPart, double delta) implements Solution {

        /** Représentation de z₁ sous forme a + bi */
        public String z1AsString() {
            return String.format("%.4f + %.4fi", realPart, imagPart);
        }

        /** Représentation de z₂ sous forme a - bi */
        public String z2AsString() {
            return String.format("%.4f - %.4fi", realPart, imagPart);
        }
    }
}
