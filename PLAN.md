# Plan d'analyse et d'amélioration du projet Quadratic AI

## 1. Objectif
- Analyser l'architecture du projet.
- Corriger les erreurs critiques (pom.xml et compilation).
- Exécuter la compilation globale avec validation.
- Fournir un rapport de fin de tâche.

## 2. Étapes exécutées
1. Correction des balises invalide `<n>` en `<name>` dans :
   - `quadratic-core/pom.xml`
   - `quadratic-neural/pom.xml`
   - `quadratic-symbolic/pom.xml`
   - `quadratic-app/pom.xml`
2. Lancement de `mvn clean compile`.
3. Détection d'erreur de compilation dans `quadratic-symbolic/src/main/java/ai/quadratic/symbolic/dialogue/DialogueManager.java` liée à du `switch` expression (ligne 210) et potentielle incohérence d'encodage.
4. Refactorisation de `handleGlobalCommands` et `handleInlineCommand` en `if/else` classiques.
5. Re-lancement de `mvn clean compile` et succès global du build.

## 3. Résultat
- Build Maven : ✅ `BUILD SUCCESS`.
- Module symbolique compilé corrigé.
- Pas de changement de spécification métier.

## 4. Recommandations suivantes
- Ajouter tests JUnit pour `quadratic-symbolic` (FSM, parser, réponse). 
- Ajouter tests JUnit pour `quadratic-neural` (Réseau, optimiseurs, entraînement). 
- Ajouter CI (`mvn test` + rapport couverture).
- Ajouter un interface abstraite `SolverBackend` pour déconnecter phase symbolique vs neuronale.

## 5. Validation rapide
- `mvn clean compile` -> échec corrigé et réussi.
- Fichier créé avec résumé du plan et état de l'exécution.
