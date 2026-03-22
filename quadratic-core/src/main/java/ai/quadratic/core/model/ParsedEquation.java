package ai.quadratic.core.model;

/**
 * Représentation immuable d'une équation du 2ème degré : ax² + bx + c = 0.
 *
 * <p>Utilise un {@code record} Java 21 pour garantir l'immutabilité
 * et générer automatiquement equals(), hashCode() et toString().</p>
 *
 * @param a          coefficient du terme x² (doit être ≠ 0)
 * @param b          coefficient du terme x
 * @param c          terme constant
 * @param rawInput   chaîne originale saisie par l'utilisateur (pour traçabilité)
 */
public record ParsedEquation(double a, double b, double c, String rawInput) {

    /** Validation à la construction : a ne peut pas être nul */
    public ParsedEquation {
        if (Math.abs(a) < 1e-12) {
            throw new IllegalArgumentException(
                "Le coefficient 'a' ne peut pas être 0. " +
                "L'équation ne serait plus du 2ème degré."
            );
        }
    }

    /**
     * Retourne la représentation en forme standard : ax² + bx + c = 0.
     * Gère les cas particuliers (a=1, b=0, c=0, signes négatifs).
     */
    public String toStandardForm() {
        StringBuilder sb = new StringBuilder();

        // Terme x²
        if (a == 1.0)       sb.append("x²");
        else if (a == -1.0) sb.append("-x²");
        else                sb.append(fmt(a)).append("x²");

        // Terme x
        if (b > 0)       sb.append(" + ").append(b == 1.0 ? "" : fmt(b)).append("x");
        else if (b < 0)  sb.append(" - ").append(b == -1.0 ? "" : fmt(Math.abs(b))).append("x");

        // Terme constant
        if (c > 0)       sb.append(" + ").append(fmt(c));
        else if (c < 0)  sb.append(" - ").append(fmt(Math.abs(c)));

        sb.append(" = 0");
        return sb.toString();
    }

    /** Formate un double : entier si possible, sinon 2 décimales */
    private String fmt(double d) {
        return (d == Math.floor(d) && !Double.isInfinite(d))
            ? String.valueOf((long) d)
            : String.format("%.2f", d);
    }
}
