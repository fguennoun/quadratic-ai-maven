package ai.quadratic.symbolic.dialogue;

import ai.quadratic.core.model.ParsedEquation;
import ai.quadratic.core.model.Solution;
import ai.quadratic.core.solver.QuadraticSolver;
import ai.quadratic.core.validator.InputValidator;
import ai.quadratic.core.validator.InputValidator.ValidationResult;
import ai.quadratic.symbolic.engine.ResponseGenerator;
import ai.quadratic.symbolic.parser.PromptParser;

/**
 * Gestionnaire de dialogue conversationnel pour la Phase 1.
 *
 * <p>Implémente une machine à états finis (FSM — Finite State Machine).
 * Chaque appel à {@link #handle(String)} fait potentiellement transiter
 * l'état courant vers un nouvel état, selon l'entrée reçue.</p>
 *
 * <p><b>Analogie LLM :</b> le DialogueManager joue le rôle du
 * "context manager" dans un chatbot — il maintient l'état de la
 * conversation (mémoire courte) et décide quoi générer ensuite.</p>
 *
 * <p><b>Capacités :</b></p>
 * <ul>
 *   <li>Guidage pas à pas : demande a, puis b, puis c</li>
 *   <li>Validation à chaque étape (max 3 tentatives)</li>
 *   <li>Correction possible après la confirmation</li>
 *   <li>Détection de prompts directs ("résoudre x²-5x+6=0")</li>
 *   <li>Commandes : aide, recommencer, corriger, quitter</li>
 * </ul>
 */
public class DialogueManager {

    // ── Collaborateurs ───────────────────────────────────────────────────────
    private final InputValidator    validator  = new InputValidator();
    private final QuadraticSolver   solver     = new QuadraticSolver();
    private final ResponseGenerator generator  = new ResponseGenerator();
    private final PromptParser      parser     = new PromptParser();

    // ── État conversationnel ─────────────────────────────────────────────────
    private DialogueState state    = DialogueState.GREETING;
    private double        a, b, c;
    private int           attempts = 0;

    private static final int MAX_ATTEMPTS = 3;

    // ── Point d'entrée principal ─────────────────────────────────────────────

    /**
     * Traite une entrée utilisateur et retourne la réponse de l'IA.
     *
     * @param input la saisie brute de l'utilisateur
     * @return la réponse textuelle à afficher
     */
    public String handle(String input) {
        if (input == null) input = "";
        String trimmed = input.trim();

        // Commandes globales (disponibles depuis n'importe quel état)
        String globalResponse = handleGlobalCommands(trimmed);
        if (globalResponse != null) return globalResponse;

        return switch (state) {
            case GREETING  -> handleGreeting(trimmed);
            case ASK_A     -> handleCoefficient(trimmed, 'a');
            case ASK_B     -> handleCoefficient(trimmed, 'b');
            case ASK_C     -> handleCoefficient(trimmed, 'c');
            case CONFIRM   -> handleConfirm(trimmed);
            case SOLVING   -> handlePostSolve(trimmed);
            case POST_SOLVE -> handlePostSolve(trimmed);
            case EXIT      -> "Au revoir !";
        };
    }

    /** Retourne true si la session est terminée */
    public boolean isFinished() {
        return state == DialogueState.EXIT;
    }

    /** Retourne l'état courant de la FSM (utile pour le backend REST) */
    public DialogueState getCurrentState() {
        return state;
    }

    // ── Handlers d'état ──────────────────────────────────────────────────────

    private String handleGreeting(String input) {
        if (input.isBlank()) {
            return welcomeMessage();
        }

        String lower = input.toLowerCase();

        // L'utilisateur veut être guidé
        if (lower.equals("go") || lower.equals("oui") || lower.equals("commencer")
            || lower.equals("démarrer") || lower.equals("start")) {
            state = DialogueState.ASK_A;
            attempts = 0;
            return askCoeffMessage('a');
        }

        // L'utilisateur entre directement une équation
        var parsed = parser.tryParse(input);
        if (parsed.isPresent()) {
            return solveDirectly(parsed.get());
        }

        // Entrée non reconnue → guider
        return "Je n'ai pas compris. Tapez go pour être guidé, " +
               "ou entrez directement une équation (ex : x²-5x+6=0).\n" + welcomeMessage();
    }

    private String handleCoefficient(String input, char coeff) {
        attempts++;
        boolean isA = (coeff == 'a');

        ValidationResult result = validator.validate(input, isA, attempts);

        // Commande détectée dans la validation
        if (!result.valid() && result.errorMessage() != null
            && result.errorMessage().startsWith("__COMMAND__:")) {
            return handleInlineCommand(result.errorMessage().substring("__COMMAND__:".length()));
        }

        if (!result.valid()) {
            if (attempts >= MAX_ATTEMPTS) {
                state = DialogueState.GREETING;
                attempts = 0;
                return String.format(
                    "Trop de tentatives invalides pour '%c'. Recommençons depuis le début.\n\n%s",
                    coeff, welcomeMessage()
                );
            }
            return String.format(
                "Tentative %d/%d — %s\n\nRe-saisissez le coefficient '%c' :",
                attempts, MAX_ATTEMPTS, result.errorMessage(), coeff
            );
        }

        // Valeur valide — enregistrer et passer à l'état suivant
        attempts = 0;
        switch (coeff) {
            case 'a' -> { a = result.value(); state = DialogueState.ASK_B; return askCoeffMessage('b'); }
            case 'b' -> { b = result.value(); state = DialogueState.ASK_C; return askCoeffMessage('c'); }
            case 'c' -> { c = result.value(); state = DialogueState.CONFIRM; return confirmMessage(); }
        }
        return "Erreur interne.";
    }

    private String handleConfirm(String input) {
        String lower = input.toLowerCase().trim();

        if (lower.equals("oui") || lower.equals("yes") || lower.equals("o") || lower.equals("y")) {
            return solveAndDisplay();
        }

        if (lower.equals("non") || lower.equals("no") || lower.equals("n") || lower.equals("annuler")) {
            state = DialogueState.GREETING;
            return "Annulé. Recommençons.\n\n" + welcomeMessage();
        }

        if (lower.startsWith("corriger a")) { state = DialogueState.ASK_A; attempts = 0;
            return "Nouvelle valeur pour a (actuel : " + fmt(a) + ") :"; }
        if (lower.startsWith("corriger b")) { state = DialogueState.ASK_B; attempts = 0;
            return "Nouvelle valeur pour b (actuel : " + fmt(b) + ") :"; }
        if (lower.startsWith("corriger c")) { state = DialogueState.ASK_C; attempts = 0;
            return "Nouvelle valeur pour c (actuel : " + fmt(c) + ") :"; }

        return "Répondez par oui / non, ou : corriger a, corriger b, corriger c\n" + confirmMessage();
    }

    private String handlePostSolve(String input) {
        String lower = input.toLowerCase().trim();

        if (lower.equals("oui") || lower.equals("go") || lower.equals("encore")
            || lower.equals("yes") || lower.equals("o")) {
            state = DialogueState.ASK_A;
            attempts = 0;
            return "Nouvelle équation !\n\n" + askCoeffMessage('a');
        }

        if (lower.equals("non") || lower.equals("no") || lower.equals("quitter")) {
            state = DialogueState.EXIT;
            return "Merci d'avoir utilisé QuadraticAI. Au revoir !";
        }

        // Question sur le discriminant ou les concepts
        if (lower.contains("discriminant") || lower.contains("delta") || lower.contains("delta")) {
            return explainDiscriminant();
        }

        if (lower.contains("complexe") || lower.contains("imaginaire")) {
            return explainComplex();
        }

        return "Tapez oui pour résoudre une autre équation, " +
               "ou non pour quitter.\n" +
               "Vous pouvez aussi poser une question : 'Que signifie le discriminant ?'";
    }

    // ── Commandes globales ───────────────────────────────────────────────────

    private String handleGlobalCommands(String input) {
        String lower = input.toLowerCase();

        if (lower.equals("quitter") || lower.equals("exit") || lower.equals("quit") || lower.equals("bye")) {
            state = DialogueState.EXIT;
            return "Au revoir !";
        }
        if (lower.equals("recommencer") || lower.equals("restart") || lower.equals("reset")) {
            state = DialogueState.GREETING;
            attempts = 0;
            return "Recommencons depuis le debut.\n\n" + welcomeMessage();
        }
        if (lower.equals("aide") || lower.equals("help") || lower.equals("?")) {
            return helpMessage();
        }
        return null; // Pas une commande globale
    }

    private String handleInlineCommand(String command) {
        if (command.equals("quitter") || command.equals("exit")) {
            state = DialogueState.EXIT;
            return "Au revoir !";
        }
        if (command.equals("recommencer")) {
            state = DialogueState.GREETING;
            attempts = 0;
            return "Recommencons.\n\n" + welcomeMessage();
        }
        if (command.equals("aide") || command.equals("help")) {
            return helpMessage();
        }
        return "Commande non reconnue : " + command;
    }

    // ── Résolution ───────────────────────────────────────────────────────────

    private String solveAndDisplay() {
        try {
            var equation = new ParsedEquation(a, b, c, "dialogue");
            Solution solution = solver.solve(equation);
            state = DialogueState.POST_SOLVE;
            return generator.generate(equation, solution) +
                   "\n\n─────────────────────────────\n" +
                   "Voulez-vous résoudre une autre équation ? (oui / non)\n" +
                   "Ou posez une question : 'Que signifie le discriminant ?'";
        } catch (IllegalArgumentException e) {
            state = DialogueState.GREETING;
            return "Erreur : " + e.getMessage() + "\n\n" + welcomeMessage();
        }
    }

    private String solveDirectly(ParsedEquation equation) {
        try {
            Solution solution = solver.solve(equation);
            state = DialogueState.POST_SOLVE;
            return "Équation détectée : " + equation.toStandardForm() + "\n\n" +
                   generator.generate(equation, solution) +
                   "\n\n─────────────────────────────\n" +
                   "Voulez-vous résoudre une autre équation ? (oui / non)";
        } catch (Exception e) {
            return "Impossible de résoudre : " + e.getMessage();
        }
    }

    // ── Messages ─────────────────────────────────────────────────────────────

    private String welcomeMessage() {
        return """
               ╔══════════════════════════════════════════════╗
               ║   QuadraticAI — Phase 1 : IA Symbolique      ║
               ╚══════════════════════════════════════════════╝

               Je résous les équations du 2ème degré : ax² + bx + c = 0

               Options :
                 go              → je vous guide pas à pas
                 x²-5x+6=0       → entrez directement l'équation
                 aide            → afficher l'aide complète
                 quitter         → terminer le programme
               """;
    }

    private String askCoeffMessage(char coeff) {
        return switch (coeff) {
            case 'a' -> """
                        ┌─ Étape 1/3 ──────────────────────────────┐
                        │  Entrez le coefficient a (terme x²)       │
                        │  Rappel : a ≠ 0 (sinon équation linéaire) │
                        └───────────────────────────────────────────┘
                        Formats acceptés : entier (2), décimal (-3.5), fraction (1/2)
                        
                        a = """;
            case 'b' -> String.format("""
                        ┌─ Étape 2/3 ──────────────────────────────┐
                        │  Entrez le coefficient b (terme x)        │
                        │  Équation en cours : %sx² + __ x + __ = 0 │
                        └───────────────────────────────────────────┘
                        (Entrez 0 si le terme x est absent)
                        
                        b = """, fmt(a));
            case 'c' -> String.format("""
                        ┌─ Étape 3/3 ──────────────────────────────┐
                        │  Entrez la constante c                    │
                        │  Équation en cours : %sx² %s x + __ = 0   │
                        └───────────────────────────────────────────┘
                        (Entrez 0 si le terme constant est absent)
                        
                        c = """, fmt(a), fmtSigned(b));
            default -> "Coefficient inconnu.";
        };
    }

    private String confirmMessage() {
        var preview = new ParsedEquation(a, b, c, "preview");
        return String.format("""
               ┌─ Confirmation ───────────────────────────────┐
               │  Équation : %-32s  │
               │  a = %-10s b = %-10s c = %-10s │
               └──────────────────────────────────────────────┘
               
               Répondez : oui  |  non  |  corriger a  |  corriger b  |  corriger c
               
               > """,
               preview.toStandardForm(), fmt(a), fmt(b), fmt(c));
    }

    private String helpMessage() {
        return """
               ┌─ Aide ──────────────────────────────────────────────────────┐
               │  COMMANDES                                                  │
               │  go            Démarrer la saisie guidée pas à pas          │
               │  aide / ?      Afficher cette aide                          │
               │  recommencer   Repartir depuis le début                     │
               │  quitter       Terminer le programme                        │
               │                                                             │
               │  SAISIE DES COEFFICIENTS                                    │
               │  Formats valides : 2  -3.5  +1.0  0.001  1/2  -3/4         │
               │  Virgule acceptée : 2,5 → converti en 2.5                  │
               │  Lettres et symboles (@#$) → rejetés avec explication       │
               │  a = 0 → message spécifique (équation linéaire)             │
               │                                                             │
               │  CONFIRMATION                                               │
               │  oui / non / corriger a / corriger b / corriger c           │
               └─────────────────────────────────────────────────────────────┘
               """;
    }

    private String explainDiscriminant() {
        return """
               ─────────────────────────────────────────────────────
                Le discriminant Δ = b² - 4ac
               ─────────────────────────────────────────────────────
               
               C'est un nombre qui résume tout sur les solutions :
               
                 Δ > 0  →  2 racines réelles distinctes
                           x₁ = (-b + √Δ) / 2a
                           x₂ = (-b - √Δ) / 2a
               
                 Δ = 0  →  1 racine double (le parabole touche l'axe
                           en un seul point)
                           x₀ = -b / 2a
               
                 Δ < 0  →  Pas de racine réelle. Les solutions sont
                           complexes (nombres imaginaires).
                           z = -b/(2a) ± i·√(-Δ)/(2a)
               
               Voulez-vous résoudre une équation ? (oui / non)
               """;
    }

    private String explainComplex() {
        return """
               ─────────────────────────────────────────────────────
                Les nombres complexes
               ─────────────────────────────────────────────────────
               
               Quand Δ < 0, la racine carrée d'un nombre négatif
               n'existe pas dans les réels. On introduit i = √(-1).
               
               Exemple : x² + 1 = 0 → x = ±i
               
               Les solutions sont toujours conjuguées :
                 z₁ = a + bi   et   z₂ = a - bi
               
               Voulez-vous résoudre une équation ? (oui / non)
               """;
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private String fmt(double d) {
        return (d == Math.floor(d) && !Double.isInfinite(d))
            ? String.valueOf((long) d)
            : String.format("%.2f", d);
    }

    private String fmtSigned(double d) {
        return d >= 0 ? "+ " + fmt(d) : "- " + fmt(Math.abs(d));
    }
}
