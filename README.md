# QuadraticAI — Comprendre le Generative AI en Java

> Projet pédagogique multi-module Maven : construire une IA locale from scratch
> pour résoudre des équations du 2ème degré — sans aucune bibliothèque externe.
> Documenté en **français**, codé en **anglais**.

---

## Table des matières

1. [Intelligence Artificielle — Vue d'ensemble](#1-intelligence-artificielle--vue-densemble)
2. [Machine Learning — Fondements](#2-machine-learning--fondements)
3. [Deep Learning et Réseaux de Neurones](#3-deep-learning-et-réseaux-de-neurones)
4. [Generative AI — Comment ça marche vraiment](#4-generative-ai--comment-ça-marche-vraiment)
5. [LLMs — Architecture Transformer](#5-llms--architecture-transformer)
6. [Ce projet vs un vrai LLM](#6-ce-projet-vs-un-vrai-llm)
7. [Prérequis et installation (Windows)](#7-prérequis-et-installation-windows)
8. [Structure du projet Maven](#8-structure-du-projet-maven)
9. [Compilation et exécution](#9-compilation-et-exécution)
10. [Architecture technique détaillée](#10-architecture-technique-détaillée)
11. [Améliorations implémentées](#11-améliorations-implémentées)
12. [Prochaines étapes](#12-prochaines-étapes)

---

## 1. Intelligence Artificielle — Vue d'ensemble

### Définition

L'**Intelligence Artificielle (IA)** désigne l'ensemble des techniques permettant
à une machine d'accomplir des tâches qui requièrent normalement l'intelligence humaine :
comprendre le langage, reconnaître des images, prendre des décisions, résoudre des problèmes.

### Taxonomie de l'IA

```
Intelligence Artificielle
│
├── IA Symbolique (règles explicites)
│     Exemple : notre Phase 1 — QuadraticSolver applique Δ = b²-4ac
│     Forces  : précision parfaite, explicable, déterministe
│     Limites : ne s'adapte pas, requiert des règles manuelles
│
├── Machine Learning (apprentissage depuis des données)
│     Exemple : notre Phase 2 — le réseau apprend Δ depuis 10 000 équations
│     Forces  : généralisation, adaptation, pas de règles manuelles
│     Limites : nécessite des données, boîte noire, peut se tromper
│
│     ├── Supervised Learning   → on fournit (X, Y) et le modèle apprend f(X)≈Y
│     ├── Unsupervised Learning → le modèle trouve des structures cachées dans X
│     └── Reinforcement Learning → le modèle apprend par récompenses/punitions
│
└── Deep Learning (réseaux de neurones profonds)
      Sous-ensemble du ML avec des réseaux de neurones multicouches
      Exemples : CNN (images), RNN (séquences), Transformer (texte)
```

### Historique rapide

| Période | Événement clé |
|---------|---------------|
| 1956 | Conférence de Dartmouth — naissance du terme "IA" |
| 1986 | Backpropagation popularisée (Rumelhart, Hinton, Williams) |
| 2012 | AlexNet — le Deep Learning bat les humains en reconnaissance d'images |
| 2017 | "Attention is All You Need" — architecture Transformer (Google Brain) |
| 2018 | BERT (Google) — premier grand LLM pré-entraîné |
| 2020 | GPT-3 (OpenAI) — 175 milliards de paramètres |
| 2022 | ChatGPT — démocratisation de l'IA générative |
| 2023 | GPT-4, Claude, Gemini — LLMs multimodaux |
| 2024-25 | Modèles de raisonnement (o1, o3, Claude 3.5...) |

---

## 2. Machine Learning — Fondements

### Le problème général

On cherche une fonction f telle que f(X) ≈ Y, où :
- X = les entrées (features, données d'entrée)
- Y = les sorties attendues (labels, cibles)

Dans notre projet : X = {a, b, c} et Y = {x₁, x₂, Δ}

### Les 3 types d'erreurs à connaître

```
Underfitting                   Bon équilibre                  Overfitting
(sous-apprentissage)           (généralisation)               (surapprentissage)

Loss │                         Loss │                         Loss │
     │  train ≈ val               │  train ↘                      │  train ↘↘
     │  (les deux élevés)         │  val   ↘                      │  val    ↘ puis ↗
     └── Epochs                  └── Epochs                  └── Epochs

Solution : modèle              Solution : bonne               Solution : Dropout,
plus complexe                  configuration                  + de données, L2
```

### Le cycle d'apprentissage supervisé

```
1. Données   →  (X_train, Y_train) + (X_val, Y_val)
2. Modèle    →  f(X) = réseau de neurones avec paramètres θ
3. Loss      →  L(f(X), Y) = mesure de l'erreur (MSE, cross-entropy...)
4. Optimizer →  θ = θ - η · ∇L   (gradient descent)
5. Répéter   →  jusqu'à convergence ou early stopping
```

### Normalisation — pourquoi c'est critique

Sans normalisation, les gradients ont des magnitudes très différentes selon les features.
Exemple : si a ∈ [-10, 10] et b ∈ [-1000, 1000], le réseau convergera mal.

```java
// Dans DatasetGenerator.java
public static double norm(double value, double range) {
    return Math.max(-1.0, Math.min(1.0, value / range)); // clip dans [-1, 1]
}
```

---

## 3. Deep Learning et Réseaux de Neurones

### Le neurone artificiel

Inspiré du neurone biologique, le neurone artificiel calcule :

```
Entrées    Poids              Somme pondérée    Activation
  x₁ ──── w₁ ────┐
  x₂ ──── w₂ ────┼──► z = Σ(wᵢ·xᵢ) + b ──► output = f(z)
  x₃ ──── w₃ ────┘
                 biais (b)

f(z) = fonction d'activation (ReLU, Tanh, Sigmoid, Linear...)
```

**Dans notre code (`Neuron.java`) :**
```java
public double forward(double[] inputs) {
    rawOutput = bias;
    for (int i = 0; i < weights.length; i++) {
        rawOutput += weights[i] * inputs[i];  // z = Wx + b
    }
    output = activation.apply(rawOutput);      // a = f(z)
    return output;
}
```

### Architecture MLP de ce projet

```
COUCHE      NEURONES    ACTIVATION    PARAMÈTRES    RÔLE
────────────────────────────────────────────────────────────────
Entrée           3      —                  —        a, b, c normalisés
Cachée 1        32      ReLU          (3+1)×32=128  Extraction de features
Cachée 2        32      ReLU        (32+1)×32=1056  Combinaisons complexes
Cachée 3        16      ReLU        (32+1)×16=528   Affinement
Sortie           3      Linear       (16+1)×3=51    x₁, x₂, Δ normalisés
────────────────────────────────────────────────────────────────
TOTAL                                     1 763     paramètres apprenables
```

### Backpropagation — l'algorithme fondamental

La backpropagation calcule ∂L/∂w pour chaque poids via la règle de la chaîne :

```
Forward :  x → [L1] → [L2] → [L3] → ŷ → Loss = MSE(ŷ, y)

Backward : ∂L/∂w₃ = ∂L/∂ŷ · ∂ŷ/∂z₃ · ∂z₃/∂w₃
           ∂L/∂w₂ = ∂L/∂a₃ · ∂a₃/∂z₂ · ∂z₂/∂w₂
           ∂L/∂w₁ = ∂L/∂a₂ · ∂a₂/∂z₁ · ∂z₁/∂w₁

Update :   w = w - η · ∂L/∂w   (Gradient Descent)
```

**Dans notre code (`NeuralNetwork.java`) :**
```java
// Delta de la couche de sortie
neuron.delta = (output[i] - target[i]) * neuron.activationDerivative();

// Rétropropagation dans les couches cachées
for (Neuron nextNeuron : nextLayer.neurons) {
    errorSum += nextNeuron.delta * nextNeuron.weights[i];
}
neuron.delta = errorSum * neuron.activationDerivative() * dropoutMask;
```

### Optimiseur Adam vs SGD

| Critère | SGD | Adam |
|---------|-----|------|
| Learning rate | Global, fixe | Adaptatif par poids |
| Mémoire | 0 état par poids | 2 états par poids (m, v) |
| Convergence | Lente, instable | Rapide, stable |
| Hyperparamètres | η | η, β₁=0.9, β₂=0.999, ε=1e-8 |
| Utilisé par | Anciens modèles | GPT, BERT, Claude, tous les LLMs |

**Formules Adam (`AdamOptimizer.java`) :**
```
m_t = β₁·m_{t-1} + (1-β₁)·g         // Moyenne exponentielle du gradient
v_t = β₂·v_{t-1} + (1-β₂)·g²        // Variance exponentielle du gradient
m̂   = m_t / (1 - β₁^t)               // Correction du biais au début
v̂   = v_t / (1 - β₂^t)
w   = w - η · m̂ / (√v̂ + ε)           // Mise à jour adaptative
```

---

## 4. Generative AI — Comment ça marche vraiment

### Définition

L'**IA générative** désigne les modèles capables de *créer* de nouveaux contenus
(texte, images, code, audio) plutôt que de simplement classer ou prédire.

### Comment un LLM génère du texte

Un LLM ne "comprend" pas le texte au sens humain. Il prédit statistiquement
le token suivant le plus probable, étape par étape :

```
Prompt : "La capitale de la France est"
   ↓
Token 1 : "Paris"     (probabilité : 0.94)
Token 2 : " ."        (probabilité : 0.87)
Token 3 : " Elle"     (probabilité : 0.61)
...

Ce n'est pas de la compréhension — c'est de la prédiction statistique
optimisée sur des milliards d'exemples.
```

### Les 3 phases d'entraînement d'un LLM

```
Phase 1 — Pré-entraînement (Pre-training)
  Données     : 300+ milliards de tokens (internet, livres, code...)
  Objectif    : prédire le token suivant (Causal Language Modeling)
  Durée       : semaines sur des milliers de GPU A100
  Résultat    : un modèle de base qui "connaît" le langage

Phase 2 — Fine-tuning supervisé (SFT)
  Données     : ~100 000 exemples (question → réponse idéale)
  Objectif    : apprendre à répondre comme un assistant
  Durée       : quelques heures sur quelques GPU

Phase 3 — Alignment (RLHF / Constitutional AI)
  Données     : préférences humaines (quel texte est meilleur ?)
  Objectif    : aligner le modèle sur les valeurs humaines
  Durée       : heures à jours
```

### Tokenisation — comment le texte devient des nombres

```
Texte brut : "résoudre x²-5x+6=0"
     ↓ BPE (Byte-Pair Encoding)
Tokens     : ["rés", "oud", "re", " x", "²", "-", "5", "x", "+", "6", "=", "0"]
     ↓ Embedding (vecteur de dimension 768 à 12288)
Vecteurs   : [[0.23, -0.81, 0.44, ...], [...], ...]
     ↓ Transformer layers
Sortie     : vecteur de logits → softmax → probabilités sur le vocabulaire
```

**Notre analogie :**
```
Texte brut : "résoudre 2x²-5x+3=0"
     ↓ PromptParser.java
Coefficients : a=2.0, b=-5.0, c=3.0
     ↓ DatasetGenerator.norm()
Normalisés : [0.2, -0.5, 0.3]   (entrée du réseau)
     ↓ NeuralNetwork.forward()
Sortie brute : [0.075, 0.05, 0.0025]
     ↓ DatasetGenerator.denorm()
Résultat : x₁=1.5, x₂=1.0, Δ=1.0
```

---

## 5. LLMs — Architecture Transformer

### Le mécanisme d'attention (Self-Attention)

L'innovation centrale du Transformer (2017) : chaque token "regarde" tous les
autres tokens pour décider de son représentation contextuelle.

```
Phrase : "La banque est sur la rive"

"banque" attend "rive" → sens géographique  ✓
"banque" attend "argent" (absent) → sens financier  ✗

Le modèle apprend automatiquement ces associations pendant l'entraînement.
```

**Formule :**
```
Attention(Q, K, V) = softmax(QKᵀ / √d_k) · V

Q = matrice de requêtes (Query)
K = matrice de clés (Key)
V = matrice de valeurs (Value)
d_k = dimension des clés (facteur d'échelle)
```

### Comparaison des architectures

```
Notre MLP                     GPT-4 Transformer
──────────────────────────    ────────────────────────────────────
3 entrées scalaires           50 000+ tokens contexte
Couches Dense (ReLU)          Couches Transformer (Attention+FFN)
1 763 paramètres              ~1 760 milliards de paramètres
MSE loss                      Cross-entropy loss
Adam optimizer                Adam + LR warmup + cosine decay
Entraînement : secondes       Entraînement : semaines (milliers GPU)
Inférence : ms                Inférence : ~100 tokens/sec
```

### Pourquoi les LLMs "hallucinent"

Un LLM prédit le texte le plus *probable* d'après son entraînement.
Si une information n'est pas dans ses données, il génère ce qui "semble probable"
statistiquement — même si c'est faux. Ce n'est pas un bug, c'est une conséquence
du principe de fonctionnement par prédiction.

**Notre modèle n'hallucine pas** pour les équations du 2ème degré car :
- Phase 1 : règles mathématiques exactes
- Phase 2 : les données d'entraînement couvrent tous les cas possibles

---

## 6. Ce projet vs un vrai LLM

| Concept | Ce projet | GPT-4 / Claude |
|---------|-----------|----------------|
| Entrée | 3 réels (a, b, c) | Texte libre (tokens) |
| Tokenisation | PromptParser (regex) | BPE (50 000 tokens vocab) |
| Embedding | Normalisation [-1,1] | Vecteurs 12288 dimensions |
| Architecture | MLP 4 couches | Transformer 96 couches |
| Attention | Absent | Multi-head Attention |
| Paramètres | 1 763 | 1 760 000 000 000 |
| Loss | MSE | Cross-Entropy |
| Optimizer | Adam | Adam + warmup |
| Dataset | 10 000 équations | 300 milliards de tokens |
| Entraînement | ~5 secondes CPU | ~3 mois, 25 000 GPU |
| Sortie | 3 réels (x1, x2, Δ) | Texte libre (tokens) |
| Dialogue | DialogueManager (FSM) | Context window + RLHF |
| Sauvegarde | JSON (1 Ko) | Checkpoint (700 Go) |

**Le principe fondamental est identique. L'échelle est différente de 10¹².**

---

## 7. Prérequis et installation (Windows)

### Java 21 JDK

1. Télécharger depuis : https://adoptium.net/
   - Choisir : **Windows x64** → **JDK 21** → **.msi**
2. Installer (cocher "Add to PATH" et "Set JAVA_HOME")
3. Vérifier dans **cmd** ou **PowerShell** :

```cmd
java --version
javac --version
```

Attendu : `openjdk 21.0.x ...`

### Maven 3.9+

1. Télécharger depuis : https://maven.apache.org/download.cgi
   - Choisir : **Binary zip archive** (apache-maven-3.9.x-bin.zip)
2. Extraire vers `C:\tools\maven\`
3. Ajouter `C:\tools\maven\bin` au PATH système
4. Vérifier :

```cmd
mvn --version
```

Attendu : `Apache Maven 3.9.x ...`

### Configurer PATH manuellement (si nécessaire)

```
Panneau de configuration
  → Système → Paramètres système avancés
  → Variables d'environnement → Variables système
  → Path → Modifier → Nouveau
  → Ajouter : C:\Program Files\Eclipse Adoptium\jdk-21.x.x.x-hotspot\bin
  → Ajouter : C:\tools\maven\bin
  → OK partout → Redémarrer le terminal
```

### IDE recommandé

- **IntelliJ IDEA Community** (gratuit) : https://www.jetbrains.com/idea/
  - Import : File → Open → sélectionner le dossier racine du projet
  - IntelliJ détecte automatiquement le POM Maven parent
- **VS Code** + "Extension Pack for Java" : https://code.visualstudio.com/

---

## 8. Structure du projet Maven

```
quadratic-ai/                          ← Racine Maven (POM parent)
│   pom.xml                            ← Configuration partagée, versions
│   quadratic-ai-all.jar               ← JAR exécutable (après mvn package)
│
├── quadratic-core/                    ← Module partagé (aucune dépendance interne)
│   │   pom.xml
│   └── src/
│       ├── main/java/ai/quadratic/core/
│       │   ├── model/
│       │   │   ├── ParsedEquation.java  ← Record : a, b, c + validation
│       │   │   └── Solution.java        ← Sealed interface : 3 cas
│       │   ├── solver/
│       │   │   └── QuadraticSolver.java ← Formule discriminant
│       │   └── validator/
│       │       └── InputValidator.java  ← Validation numérique complète
│       └── test/java/ai/quadratic/core/
│           └── CoreTest.java           ← Tests JUnit 5
│
├── quadratic-symbolic/                ← Phase 1 : IA Symbolique
│   │   pom.xml
│   └── src/main/java/ai/quadratic/symbolic/
│       ├── dialogue/
│       │   ├── DialogueState.java       ← Enum des états (FSM)
│       │   └── DialogueManager.java     ← Machine à états conversationnelle
│       ├── parser/
│       │   └── PromptParser.java        ← Extraction équation depuis texte
│       └── engine/
│           └── ResponseGenerator.java   ← Génération réponse pédagogique
│
├── quadratic-neural/                  ← Phase 2 : Réseau Neuronal
│   │   pom.xml
│   └── src/main/java/ai/quadratic/neural/
│       ├── nn/
│       │   ├── Neuron.java             ← Neurone + états Adam
│       │   ├── Layer.java              ← Couche + dropout
│       │   └── NeuralNetwork.java      ← MLP + backprop
│       ├── optimizer/
│       │   └── AdamOptimizer.java      ← Optimiseur Adam complet
│       ├── data/
│       │   └── DatasetGenerator.java   ← Génération + normalisation
│       ├── training/
│       │   └── Trainer.java            ← Boucle + early stopping + CSV
│       └── persistence/
│           └── ModelSerializer.java    ← Save/Load JSON sans dépendance
│
└── quadratic-app/                     ← Point d'entrée global
    │   pom.xml
    └── src/main/java/ai/quadratic/app/
        └── Main.java                  ← Lance phase1, phase2 ou train
```

### Dépendances inter-modules

```
quadratic-core          (aucune dépendance interne)
       ↑
       ├── quadratic-symbolic
       ├── quadratic-neural
       └── quadratic-app ← (dépend de tous)
```

---

## 9. Compilation et exécution

### Compilation complète (depuis la racine)

```cmd
cd C:\projets\quadratic-ai
mvn clean package -DskipTests
```

Ce qui se passe :
1. Maven compile les 4 modules dans l'ordre des dépendances
2. Lance les tests JUnit 5 du module core
3. Crée `quadratic-ai-all.jar` (JAR exécutable unique)

### Exécution

```cmd
REM Phase 1 — IA Symbolique avec dialogue guidé
java --enable-preview -jar quadratic-ai-all.jar phase1

REM Phase 2 — Réseau neuronal (charge model.json si disponible)
java --enable-preview -jar quadratic-ai-all.jar phase2

REM Forcer le réentraînement
java --enable-preview -jar quadratic-ai-all.jar train
```

### Exécution module par module (développement)

```cmd
REM Compiler seulement le core
mvn -pl quadratic-core compile

REM Lancer les tests du core
mvn -pl quadratic-core test

REM Compiler les modules symbolic et ses dépendances
mvn -pl quadratic-symbolic -am compile
```

### Avec IntelliJ IDEA

1. File → Open → sélectionner le dossier `quadratic-ai`
2. IntelliJ importe automatiquement tous les modules Maven
3. Clic droit sur `Main.java` → Run 'Main.main()'
4. Ajouter l'argument `phase1` ou `phase2` dans Run/Debug Configurations

---

## 10. Architecture technique détaillée

### DialogueManager — Machine à états (Phase 1)

```
S0: GREETING ──────────────────────────────────────────────┐
     │  "go"              │  équation directe              │
     ▼                    ▼                                │
S1: ASK_A  ←──────── CONFIRM ─── "corriger a"              │
     │ valide             │                                │
     ▼  ↑invalide (×3)   ▼ "oui"                          │
S2: ASK_B    MAX_ATTEMPTS → S0: GREETING                   │
     │                                                     │
     ▼                                                     │
S3: ASK_C                                                  │
     │                                                     │
     ▼                                                     │
S4: CONFIRM ─── "non" / "annuler" ────────────────────────►┘
     │  "oui"
     ▼
S5: SOLVING → POST_SOLVE ─── "oui" → S1: ASK_A
                           └── "non" → EXIT
```

### Validation numérique (InputValidator)

```
Entrée brute (String)
     │
     ├── Normalisation : trim, "," → ".", "−" → "-"
     │
     ├── Détection commande ? ("quitter", "aide"...) → signal __COMMAND__
     │
     ├── Caractères illégaux ? (lettres, @, #...) → message spécifique
     │
     ├── Fraction ? (1/2, -3/4) → calcul p/q
     │
     ├── Pattern numérique ? ([+-]?\d*\.?\d+) → Double.parseDouble()
     │
     ├── NaN ou Infini ? → message limite dépassée
     ├── |valeur| > 1 000 000 ? → message trop grande
     └── isA && valeur == 0 ? → message spécifique a≠0
```

---

## 11. Améliorations implémentées

| Amélioration | Module | Description |
|--------------|--------|-------------|
| DialogueManager | symbolic | Machine à états conversationnelle (6 états) |
| InputValidator | core | Validation complète avec messages contextuels |
| Optimiseur Adam | neural | Convergence 3-5× plus rapide que SGD |
| Dropout | neural | Régularisation, évite le surapprentissage |
| ModelSerializer | neural | Save/Load JSON sans dépendance externe |
| Export CSV loss | neural | Courbe ouvrable dans Excel Windows |
| Graphe ASCII | neural | Visualisation loss dans le terminal |
| Tests JUnit 5 | core | 12 tests unitaires (solveur + validateur) |
| Maven multi-module | tous | 4 modules avec dépendances claires |
| Fractions | core | "1/2" accepté comme coefficient |

---

## 12. Prochaines étapes

### Niveau débutant
- Ajouter d'autres tests JUnit dans `CoreTest.java`
- Exporter l'historique de la session en CSV (Phase 1)
- Ajouter un mode "expliquer le discriminant" détaillé

### Niveau intermédiaire
- Implémenter l'Adam avec weight decay (AdamW — utilisé par GPT)
- Ajouter Batch Normalization entre les couches cachées
- Visualiser les poids du réseau après entraînement

### Niveau avancé
- Étendre à l'équation cubique (4 entrées, architecture plus large)
- Implémenter un mécanisme d'attention simplifié (mini-Transformer)
- Ajouter un tokenizer BPE pour des prompts en langage naturel plus riches

### Ressources pour aller plus loin

- **Livre** : *Deep Learning* — Goodfellow, Bengio, Courville (gratuit en ligne)
- **Cours** : Stanford CS231n — Convolutional Neural Networks (YouTube)
- **Cours** : Stanford CS224N — NLP avec Deep Learning (YouTube)
- **Blog**  : *The Illustrated Transformer* — Jay Alammar (excellent visuellement)
- **Code**  : nanoGPT de Karpathy — GPT minimal en ~300 lignes Python

---

*Projet pédagogique — Java 21, Maven 3.9+, aucune dépendance externe.*
*Documentation en français, code en anglais.*
