package ai.quadratic.symbolic.dialogue;

/**
 * États de la machine à états conversationnelle (Dialogue Manager).
 *
 * <p>Le {@link DialogueManager} passe d'un état à l'autre selon
 * les entrées utilisateur. Ce pattern est identique aux "decoder states"
 * d'un modèle seq2seq ou aux "conversation states" des chatbots industriels.</p>
 *
 * <pre>
 *  GREETING ──► ASK_A ──► ASK_B ──► ASK_C ──► CONFIRM ──► SOLVING ──► GREETING
 *                 ▲          ▲          ▲           │
 *                 │          │          │           └── corriger a/b/c
 *                 └──────────┴──────────┴────── ERROR (max 3 tentatives)
 * </pre>
 */
public enum DialogueState {

    /** État initial : accueil, présentation, détection d'intention */
    GREETING,

    /** Demande du coefficient a (terme x²) */
    ASK_A,

    /** Demande du coefficient b (terme x) */
    ASK_B,

    /** Demande du coefficient c (terme constant) */
    ASK_C,

    /** Récapitulatif et demande de confirmation avant résolution */
    CONFIRM,

    /** Résolution en cours + affichage du résultat */
    SOLVING,

    /** État post-résolution : proposer une nouvelle équation ou des explications */
    POST_SOLVE,

    /** État terminal : l'utilisateur a demandé à quitter */
    EXIT
}
