package ai.quadratic.symbolic.parser;

import ai.quadratic.core.model.ParsedEquation;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parseur de prompts en langage naturel vers une {@link ParsedEquation}.
 *
 * <p>Extrait les coefficients a, b, c depuis une expression textuelle.
 * Analogue à la couche de tokenisation d'un LLM : transforme un texte brut
 * en représentation structurée.</p>
 *
 * <p>Supporte les notations : x², x^2, x*x, coefficients implicites (x² = 1·x²).</p>
 */
public class PromptParser {

    /** Mots-clés d'intention (résoudre, solve, calculer...) */
    private static final Pattern INTENT_PATTERN = Pattern.compile(
        "(?i)(r[eé]soud(?:re|s)?|solve|calculer?|trouver?|find|compute)"
    );

    /** Pattern général pour ax² + bx + c = 0 */
    private static final Pattern GENERAL_PATTERN = Pattern.compile(
        "([+-]?\\d*\\.?\\d+)?\\s*[xX]\\s*(?:\\^2|²|\\*[xX])" +
        "\\s*([+-]\\s*\\d*\\.?\\d*)?\\s*[xX]?" +
        "\\s*([+-]\\s*\\d+\\.?\\d*)?\\s*=\\s*0"
    );

    /**
     * Tente de parser un prompt en équation.
     *
     * @param prompt la saisie brute de l'utilisateur
     * @return une {@link ParsedEquation} si le parsing réussit, sinon empty
     */
    public Optional<ParsedEquation> tryParse(String prompt) {
        if (prompt == null || prompt.isBlank()) return Optional.empty();

        String normalized = normalize(prompt);
        return extractFromNormalized(normalized);
    }

    // ── Implémentation ────────────────────────────────────────────────────────

    private String normalize(String input) {
        return input.toLowerCase()
                    .replace("²", "^2")
                    .replace("×", "*")
                    .replace("−", "-")
                    .replaceAll("\\s+", " ")
                    .trim();
    }

    private Optional<ParsedEquation> extractFromNormalized(String text) {
        // Extraire la partie avant "= 0"
        int eqIdx = text.indexOf("=");
        if (eqIdx < 0) return Optional.empty();

        String lhs = text.substring(0, eqIdx).trim();

        // Terme x² obligatoire
        Pattern aPattern = Pattern.compile("([+-]?\\d*\\.?\\d*)\\s*[xX]\\^?2");
        Matcher aMatcher = aPattern.matcher(lhs);
        if (!aMatcher.find()) return Optional.empty();

        double a = parseCoeff(aMatcher.group(1), true);
        String remaining = lhs.substring(0, aMatcher.start()) + lhs.substring(aMatcher.end());

        // Terme x
        double b = 0;
        Pattern bPattern = Pattern.compile("([+-]?\\s*\\d*\\.?\\d*)\\s*[xX](?!\\^?2)");
        Matcher bMatcher = bPattern.matcher(remaining);
        if (bMatcher.find()) {
            b = parseCoeff(bMatcher.group(1), false);
            remaining = remaining.substring(0, bMatcher.start()) + remaining.substring(bMatcher.end());
        }

        // Terme constant
        double c = 0;
        Pattern cPattern = Pattern.compile("([+-]?\\s*\\d+\\.?\\d*)\\s*$");
        Matcher cMatcher = cPattern.matcher(remaining.trim());
        if (cMatcher.find()) {
            try { c = Double.parseDouble(cMatcher.group(1).replace(" ", "")); }
            catch (NumberFormatException ignored) {}
        }

        try {
            return Optional.of(new ParsedEquation(a, b, c, lhs + " = 0"));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // a = 0
        }
    }

    private double parseCoeff(String s, boolean isLeading) {
        if (s == null || s.isBlank()) return isLeading ? 1.0 : 0.0;
        String cleaned = s.replace(" ", "");
        if (cleaned.equals("+") || cleaned.isEmpty()) return 1.0;
        if (cleaned.equals("-")) return -1.0;
        try { return Double.parseDouble(cleaned); }
        catch (NumberFormatException e) { return isLeading ? 1.0 : 0.0; }
    }
}
