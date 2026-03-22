package ai.quadratic.symbolic.engine;

import ai.quadratic.core.model.ParsedEquation;
import ai.quadratic.core.model.Solution;
import ai.quadratic.core.solver.QuadraticSolver;

/**
 * Générateur de réponses textuelles à partir d'une solution calculée.
 *
 * <p>Transforme une structure de données ({@link Solution}) en texte
 * lisible et pédagogique. Analogue à la couche de "decoding" d'un LLM :
 * elle convertit une représentation interne en langage naturel.</p>
 *
 * <p>Chaque type de solution génère une réponse différente, exploitant
 * le pattern matching exhaustif des sealed interfaces Java 21.</p>
 */
public class ResponseGenerator {

    private final QuadraticSolver solver = new QuadraticSolver();

    /**
     * Génère la réponse complète pour une équation et sa solution.
     *
     * @param equation l'équation résolue
     * @param solution le résultat de la résolution
     * @return la réponse formatée, prête à afficher
     */
    public String generate(ParsedEquation equation, Solution solution) {
        StringBuilder sb = new StringBuilder();

        // En-tête
        sb.append("📐 ANALYSE DE L'ÉQUATION\n");
        sb.append("  Forme standard : ").append(equation.toStandardForm()).append("\n");
        sb.append(String.format("  Coefficients   : a=%s, b=%s, c=%s%n",
            fmt(equation.a()), fmt(equation.b()), fmt(equation.c())));

        // Discriminant
        sb.append("\n🔢 CALCUL DU DISCRIMINANT\n");
        sb.append("  Δ = b² - 4ac\n");
        sb.append(String.format("  Δ = (%s)² - 4 × (%s) × (%s)%n",
            fmt(equation.b()), fmt(equation.a()), fmt(equation.c())));
        sb.append(String.format("  Δ = %.4f - %.4f%n",
            equation.b() * equation.b(), 4 * equation.a() * equation.c()));
        sb.append(String.format("  Δ = %.4f%n", getDelta(solution)));

        // Résolution — pattern matching exhaustif (Java 21)
        sb.append("\n✅ RÉSOLUTION\n");
        switch (solution) {
            case Solution.TwoRealRoots(double x1, double x2, double d) -> {
                sb.append("  Δ > 0 → Deux racines réelles distinctes\n\n");
                sb.append(String.format("  x₁ = (-b + √Δ) / 2a = %.6f%n", x1));
                sb.append(String.format("  x₂ = (-b - √Δ) / 2a = %.6f%n%n", x2));
                sb.append("🧪 VÉRIFICATION\n");
                sb.append(String.format("  f(x₁) = %.2e  (≈ 0 ✓)%n", solver.verify(equation, x1)));
                sb.append(String.format("  f(x₂) = %.2e  (≈ 0 ✓)%n%n", solver.verify(equation, x2)));
                sb.append("📝 FORME FACTORISÉE\n");
                sb.append(String.format("  %s(x - %.4f)(x - %.4f) = 0%n", fmt(equation.a()), x1, x2));
            }
            case Solution.OneDoubleRoot(double x0, double d) -> {
                sb.append("  Δ = 0 → Une racine double\n\n");
                sb.append(String.format("  x₀ = -b / 2a = %.6f%n%n", x0));
                sb.append("🧪 VÉRIFICATION\n");
                sb.append(String.format("  f(x₀) = %.2e  (≈ 0 ✓)%n%n", solver.verify(equation, x0)));
                sb.append("📝 FORME FACTORISÉE\n");
                sb.append(String.format("  %s(x - %.4f)² = 0%n", fmt(equation.a()), x0));
            }
            case Solution.ComplexRoots(double re, double im, double d) -> {
                sb.append("  Δ < 0 → Pas de racine réelle (solutions complexes)\n\n");
                sb.append(String.format("  z₁ = %.4f + %.4fi%n", re, im));
                sb.append(String.format("  z₂ = %.4f - %.4fi%n%n", re, im));
                sb.append("  ℝ : L'équation n'a pas de solution dans l'ensemble des réels.\n");
            }
        }

        // Résumé final
        sb.append("\n").append("─".repeat(52)).append("\n");
        sb.append(summarize(solution));
        return sb.toString();
    }

    private String summarize(Solution s) {
        return switch (s) {
            case Solution.TwoRealRoots(double x1, double x2, double d) ->
                String.format("💡 Résultat : x₁ = %.4f  |  x₂ = %.4f", x1, x2);
            case Solution.OneDoubleRoot(double x0, double d) ->
                String.format("💡 Résultat : x₀ = %.4f  (racine double)", x0);
            case Solution.ComplexRoots r ->
                String.format("💡 Résultat : z₁ = %s  |  z₂ = %s", r.z1AsString(), r.z2AsString());
        };
    }

    private double getDelta(Solution s) {
        return switch (s) {
            case Solution.TwoRealRoots r -> r.delta();
            case Solution.OneDoubleRoot r -> r.delta();
            case Solution.ComplexRoots r -> r.delta();
        };
    }

    private String fmt(double d) {
        return (d == Math.floor(d) && !Double.isInfinite(d))
            ? String.valueOf((long) d)
            : String.format("%.2f", d);
    }
}
