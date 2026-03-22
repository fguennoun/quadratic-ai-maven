package ai.quadratic.core.validator;

import java.util.regex.Pattern;

/**
 * Validateur d'entrées utilisateur pour les coefficients d'une équation.
 *
 * <p>Centralise toutes les règles de validation dans une seule classe,
 * ce qui facilite les tests unitaires et évite la duplication de logique.</p>
 *
 * <p><b>Règles appliquées :</b></p>
 * <ul>
 *   <li>Accepte : entiers, décimaux, négatifs, fractions simples (1/2)</li>
 *   <li>Convertit : virgule → point (2,5 → 2.5), +2 → 2.0</li>
 *   <li>Rejette : texte, caractères spéciaux, NaN, infini, dépassement</li>
 *   <li>Rejette : a = 0 avec message spécifique</li>
 * </ul>
 */
public class InputValidator {

    /** Pattern : chiffres optionnels, signe optionnel, décimale optionnelle */
    private static final Pattern NUMERIC_PATTERN =
        Pattern.compile("^[+-]?\\d*\\.?\\d+$");

    /** Pattern pour fractions simples : ex. 1/2, -3/4 */
    private static final Pattern FRACTION_PATTERN =
        Pattern.compile("^([+-]?\\d+)/([+-]?\\d+)$");

    /** Résultat de validation — record immuable */
    public record ValidationResult(boolean valid, double value, String errorMessage) {

        /** Crée un résultat valide */
        public static ValidationResult ok(double v) {
            return new ValidationResult(true, v, null);
        }

        /** Crée un résultat invalide avec message d'erreur */
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, 0.0, message);
        }
    }

    /**
     * Valide et parse une entrée utilisateur en tant que coefficient numérique.
     *
     * @param input   la chaîne brute saisie par l'utilisateur
     * @param isA     true si c'est le coefficient 'a' (doit être ≠ 0)
     * @param attempt numéro de tentative actuelle (pour personnaliser le message)
     * @return un {@link ValidationResult} contenant la valeur ou le message d'erreur
     */
    public ValidationResult validate(String input, boolean isA, int attempt) {

        if (input == null || input.isBlank()) {
            return ValidationResult.fail(
                "Entrée vide. Veuillez saisir un nombre (ex: 2, -3.5, 0)."
            );
        }

        // Normalisation : supprimer les espaces, remplacer virgule par point
        String cleaned = input.trim()
                              .replace(" ", "")
                              .replace(",", ".")
                              .replace("−", "-"); // tiret long Unicode

        // Détecter les commandes conversationnelles pour ne pas les valider comme erreur
        if (isConversationalCommand(cleaned)) {
            return ValidationResult.fail("__COMMAND__:" + cleaned.toLowerCase());
        }

        // Détecter les caractères non autorisés (lettres, caractères spéciaux)
        if (containsIllegalChars(cleaned)) {
            String illegal = extractIllegalChars(cleaned);
            return ValidationResult.fail(String.format(
                "Caractère(s) non autorisé(s) détecté(s) : '%s'%n" +
                "Saisissez uniquement des chiffres, un signe (-) ou un point décimal.",
                illegal
            ));
        }

        // Tenter de parser une fraction (ex: 1/2, -3/4)
        var fractionResult = tryParseFraction(cleaned);
        if (fractionResult != null) {
            return validateFinalValue(fractionResult, isA, attempt);
        }

        // Parsing numérique standard
        if (!NUMERIC_PATTERN.matcher(cleaned).matches()) {
            return ValidationResult.fail(buildFormatError(input, attempt));
        }

        double value;
        try {
            value = Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return ValidationResult.fail(buildFormatError(input, attempt));
        }

        return validateFinalValue(value, isA, attempt);
    }

    // ── Méthodes privées ────────────────────────────────────────────────────────

    /** Vérifie si la valeur parsée est acceptable (pas NaN, pas infini, a≠0) */
    private ValidationResult validateFinalValue(double value, boolean isA, int attempt) {

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return ValidationResult.fail(
                "La valeur est hors des limites autorisées. " +
                "Utilisez un nombre entre -1 000 000 et 1 000 000."
            );
        }

        if (Math.abs(value) > 1_000_000) {
            return ValidationResult.fail(
                "Valeur trop grande. Utilisez un coefficient entre -1 000 000 et 1 000 000."
            );
        }

        if (isA && Math.abs(value) < 1e-12) {
            return ValidationResult.fail(
                "Le coefficient 'a' ne peut pas être 0.\n" +
                "Si a = 0, l'équation devient linéaire (bx + c = 0), non quadratique.\n" +
                "Entrez une valeur différente de 0."
            );
        }

        return ValidationResult.ok(value);
    }

    /** Tente de parser une fraction de la forme p/q. Retourne null si non applicable. */
    private Double tryParseFraction(String input) {
        var matcher = FRACTION_PATTERN.matcher(input);
        if (!matcher.matches()) return null;

        double numerator   = Double.parseDouble(matcher.group(1));
        double denominator = Double.parseDouble(matcher.group(2));

        if (Math.abs(denominator) < 1e-12) return null; // division par zéro
        return numerator / denominator;
    }

    /** Vérifie si l'entrée contient des caractères illégaux */
    private boolean containsIllegalChars(String input) {
        return input.chars().anyMatch(c ->
            !Character.isDigit(c) && c != '.' && c != '-' && c != '+' && c != '/'
        );
    }

    /** Extrait les caractères illégaux pour le message d'erreur */
    private String extractIllegalChars(String input) {
        StringBuilder found = new StringBuilder();
        input.chars()
             .filter(c -> !Character.isDigit(c) && c != '.' && c != '-' && c != '+' && c != '/')
             .distinct()
             .forEach(c -> found.append((char) c).append(' '));
        return found.toString().trim();
    }

    /** Vérifie si l'entrée est une commande conversationnelle connue */
    private boolean isConversationalCommand(String input) {
        String lower = input.toLowerCase();
        return lower.equals("quitter") || lower.equals("exit") || lower.equals("quit")
            || lower.equals("aide") || lower.equals("help") || lower.equals("?")
            || lower.equals("recommencer") || lower.equals("restart")
            || lower.startsWith("corriger");
    }

    /** Construit un message d'erreur formaté selon le numéro de tentative */
    private String buildFormatError(String input, int attempt) {
        String base = String.format(
            "'%s' n'est pas un nombre valide. Exemples acceptés : 2, -3.5, 0, 1/2.",
            input
        );
        if (attempt == 2) {
            base += "\n  Conseil : n'utilisez pas de lettres ni de symboles mathématiques.";
        }
        return base;
    }
}
