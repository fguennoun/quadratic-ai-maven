# IMPROVEMENT.md — Propositions d'Amélioration pour QuadraticAI

**Date :** 23 Mars 2026  
**Version actuelle :** 1.0.0  
**Version cible :** 2.0.0 (POC IA/GenAI pédagogique avancé)

---

## Table des matières

1. [Objectifs des Améliorations](#1-objectifs-des-améliorations)
2. [Améliorations Prioritaires](#2-améliorations-prioritaires)
3. [Améliorations Fonctionnelles](#3-améliorations-fonctionnelles)
4. [Améliorations Techniques](#4-améliorations-techniques)
5. [Améliorations Pédagogiques](#5-améliorations-pédagogiques)
6. [Améliorations Avancées](#6-améliorations-avancées)
7. [Roadmap d'Implémentation](#7-roadmap-dimplémentation)
8. [Spécifications Détaillées](#8-spécifications-détaillées)

---

## 1. Objectifs des Améliorations

### Vision Globale

Transformer QuadraticAI en un **POC (Proof of Concept) complet** pour l'apprentissage de l'IA et du Generative AI en Java, en :

1. ✅ **Maintenant l'approche pédagogique** (zero-dependency, code explicite)
2. ✅ **Ajoutant des fonctionnalités avancées** (Transformer lite, attention mechanism)
3. ✅ **Améliorant la qualité** (tests, CI/CD, visualisation)
4. ✅ **Facilitant l'extensibilité** (abstractions, plugins)

### Principes Directeurs

- 🎯 **Pédagogie d'abord** : chaque ajout doit avoir une valeur éducative
- 🚀 **Pas de sur-engineering** : garder le code compréhensible
- 📦 **Modularité** : chaque feature peut être activée/désactivée
- 🔬 **Expérimentation** : permettre de tweaker les hyperparamètres facilement

---

## 2. Améliorations Prioritaires

### 🔴 Priorité CRITIQUE (à implémenter en premier)

#### PRIO-1 : Tests Unitaires Complets

**Problème** : 0% de couverture sur `symbolic` et `neural`, seulement `core` testé

**Solution** :
```
quadratic-symbolic/src/test/java/
├── DialogueManagerTest.java     (15 tests)
│   ├── testGreetingState()
│   ├── testAskAValidInput()
│   ├── testAskAInvalidMaxAttempts()
│   ├── testConfirmYes()
│   ├── testConfirmNo()
│   ├── testCorrectCommand()
│   └── testDirectPromptParsing()
│
└── PromptParserTest.java        (10 tests)
    ├── testStandardForm()       → "x²-5x+6=0"
    ├── testImplicitCoefficients() → "x²+x+1=0"
    ├── testNegativeCoefficients() → "-2x²+3x-1=0"
    └── testInvalidFormats()     → "2x+3=0" (pas quadratique)

quadratic-neural/src/test/java/
├── NeuralNetworkTest.java       (12 tests)
│   ├── testForwardPass()
│   ├── testBackwardPass()
│   ├── testGradientChecking()   ← numérique vs analytique
│   └── testDropoutMasking()
│
├── AdamOptimizerTest.java       (8 tests)
│   ├── testMomentumUpdate()
│   ├── testBiasCorrection()
│   └── testConvergenceSimpleFunction()
│
└── DatasetGeneratorTest.java    (6 tests)
    ├── testNormalizationInRange()
    ├── testDenormalizationInverse()
    └── testDistribution60_20_20()
```

**Estimation** : 3-5 jours  
**Impact** : ⭐⭐⭐⭐⭐ (garantit la non-régression)

---

#### PRIO-2 : Configuration Externalisée

**Problème** : Hyperparamètres codés en dur dans `Main.java`

**Solution** : Fichier `config.properties` ou `config.yaml`

```properties
# config.properties
# === Neural Network ===
network.architecture=3,32,32,16,3
network.learningRate=0.001
network.dropoutRate=0.1

# === Training ===
training.epochs=100
training.batchSize=32
training.patience=5
training.datasetSize=10000

# === Paths ===
model.savePath=model.json
training.csvExport=loss_curve.csv

# === Logging ===
logging.level=INFO
logging.printEvery=5
```

**Implémentation** :
```java
// Nouvelle classe : ai.quadratic.app.config.ConfigLoader
public class ConfigLoader {
    private final Properties props;
    
    public ConfigLoader(String path) throws IOException {
        props = new Properties();
        try (var input = Files.newInputStream(Path.of(path))) {
            props.load(input);
        }
    }
    
    public int[] getArchitecture() {
        String arch = props.getProperty("network.architecture", "3,32,32,16,3");
        return Arrays.stream(arch.split(","))
                     .mapToInt(Integer::parseInt)
                     .toArray();
    }
    
    public double getLearningRate() {
        return Double.parseDouble(props.getProperty("network.learningRate", "0.001"));
    }
    
    // ... autres getters
}

// Dans Main.java
ConfigLoader config = new ConfigLoader("config.properties");
int[] architecture = config.getArchitecture();
NeuralNetwork network = new NeuralNetwork(architecture, config.getLearningRate(), ...);
```

**Estimation** : 1 jour  
**Impact** : ⭐⭐⭐⭐ (facilite l'expérimentation)

---

#### PRIO-3 : CI/CD avec GitHub Actions

**Problème** : Pas de validation automatique des commits

**Solution** : `.github/workflows/maven.yml`

```yaml
name: Build & Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean package
      
      - name: Run tests
        run: mvn test
      
      - name: Generate coverage report
        run: mvn jacoco:report
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
```

**Ajout JaCoCo dans `pom.xml` parent** :
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Estimation** : 1 jour  
**Impact** : ⭐⭐⭐⭐ (qualité garantie)

---

### 🟠 Priorité HAUTE (à implémenter rapidement)

#### PRIO-4 : Abstractions Solver et Optimizer

**Problème** : Impossible de brancher un solveur alternatif ou un autre optimiseur

**Solution** : Interfaces abstraites

```java
// ai.quadratic.core.solver.Solver (interface)
public interface Solver {
    Solution solve(ParsedEquation equation);
    double computeDelta(double a, double b, double c);
}

// Implémentations
public class AnalyticSolver implements Solver { ... }  ← actuel QuadraticSolver
public class NewtonRaphsonSolver implements Solver { ... }  ← méthode numérique
public class NeuralSolver implements Solver {  ← réseau comme solveur
    private final NeuralNetwork network;
    @Override
    public Solution solve(ParsedEquation eq) {
        double[] pred = network.forward(new double[]{eq.a(), eq.b(), eq.c()});
        return new Solution.TwoRealRoots(pred[0], pred[1], pred[2]);
    }
}

// ai.quadratic.neural.optimizer.Optimizer (interface)
public interface Optimizer {
    void update(Layer[] layers, double[][] layerInputs);
    void setLearningRate(double lr);
    double getLearningRate();
}

// Implémentations
public class AdamOptimizer implements Optimizer { ... }  ← existant
public class SGDOptimizer implements Optimizer { ... }  ← baseline
public class AdamWOptimizer implements Optimizer { ... }  ← Adam + weight decay
public class RMSPropOptimizer implements Optimizer { ... }  ← alternative
```

**Modification dans `NeuralNetwork.java`** :
```java
public NeuralNetwork(int[] layerSizes, Optimizer optimizer, double dropoutRate) {
    this.optimizer = optimizer;
    // ...
}
```

**Usage** :
```java
// Changer d'optimiseur facilement
Optimizer adam = new AdamOptimizer(0.001);
Optimizer sgd = new SGDOptimizer(0.01, 0.9);  // lr, momentum

NeuralNetwork netAdam = new NeuralNetwork(arch, adam, dropout);
NeuralNetwork netSGD = new NeuralNetwork(arch, sgd, dropout);
```

**Estimation** : 2 jours  
**Impact** : ⭐⭐⭐⭐ (extensibilité majeure)

---

#### PRIO-5 : Mode Batch (traiter fichier CSV)

**Problème** : Impossible de résoudre 100 équations d'un coup

**Solution** : Nouveau mode `java -jar quadratic-ai-all.jar batch input.csv output.csv`

**Format CSV d'entrée (`equations.csv`)** :
```csv
a,b,c
1,-5,6
2,-7,3
1,0,1
0.5,-1.5,1
```

**Format CSV de sortie (`results.csv`)** :
```csv
a,b,c,delta,x1,x2,type,error
1.0,-5.0,6.0,1.0,3.0,2.0,TwoRealRoots,0.0
2.0,-7.0,3.0,25.0,3.0,0.5,TwoRealRoots,0.0
1.0,0.0,1.0,-4.0,0.0,1.0,ComplexRoots,N/A
0.5,-1.5,1.0,0.25,2.0,1.0,TwoRealRoots,0.0
```

**Implémentation** :
```java
// ai.quadratic.app.batch.BatchProcessor
public class BatchProcessor {
    private final Solver solver;
    
    public void processCsv(Path input, Path output) throws IOException {
        var equations = parseCsv(input);
        var results = new ArrayList<Result>();
        
        for (var eq : equations) {
            try {
                var solution = solver.solve(eq);
                results.add(new Result(eq, solution, null));
            } catch (Exception e) {
                results.add(new Result(eq, null, e.getMessage()));
            }
        }
        
        writeCsv(output, results);
    }
}

// Dans Main.java
case "batch" -> {
    if (args.length < 3) {
        System.out.println("Usage: batch <input.csv> <output.csv>");
        return;
    }
    BatchProcessor processor = new BatchProcessor(new AnalyticSolver());
    processor.processCsv(Path.of(args[1]), Path.of(args[2]));
    System.out.println("Traitement terminé : " + args[2]);
}
```

**Estimation** : 2 jours  
**Impact** : ⭐⭐⭐⭐ (usage pratique avancé)

---

## 3. Améliorations Fonctionnelles

### FUNC-1 : Persistance de l'Historique des Conversations

**Objectif** : Sauvegarder toutes les interactions de la Phase 1 pour analyse

**Implémentation** :
```java
// ai.quadratic.symbolic.persistence.ConversationHistory
public class ConversationHistory {
    private final List<Interaction> interactions = new ArrayList<>();
    private final String sessionId = UUID.randomUUID().toString();
    
    public record Interaction(
        Instant timestamp,
        String userInput,
        String systemResponse,
        DialogueState stateBefore,
        DialogueState stateAfter
    ) {}
    
    public void log(String user, String system, DialogueState before, DialogueState after) {
        interactions.add(new Interaction(Instant.now(), user, system, before, after));
    }
    
    public void exportJson(Path output) throws IOException {
        // Sérialiser en JSON avec contexte complet
    }
    
    public void exportMarkdown(Path output) throws IOException {
        // Format lisible pour humains
        StringBuilder sb = new StringBuilder();
        sb.append("# Session ").append(sessionId).append("\n\n");
        for (var i : interactions) {
            sb.append("## ").append(i.timestamp()).append("\n");
            sb.append("**User:** ").append(i.userInput()).append("\n\n");
            sb.append("**System:** ").append(i.systemResponse()).append("\n\n");
        }
        Files.writeString(output, sb.toString());
    }
}
```

**Usage dans DialogueManager** :
```java
private final ConversationHistory history = new ConversationHistory();

public String handle(String input) {
    DialogueState before = this.state;
    String response = handleInternal(input);
    history.log(input, response, before, this.state);
    return response;
}

public void saveHistory() throws IOException {
    history.exportJson(Path.of("session_" + history.sessionId + ".json"));
    history.exportMarkdown(Path.of("session_" + history.sessionId + ".md"));
}
```

**Estimation** : 1 jour  
**Impact** : ⭐⭐⭐ (analyse utilisateur, debug)

---

### FUNC-2 : Visualisation Graphique (courbe loss, histogramme)

**Objectif** : Remplacer le graphe ASCII par de vrais graphiques

**Approche 1 : JFreeChart (dépendance externe)** ❌ Éviter (va à l'encontre du zero-dependency)

**Approche 2 : Export HTML + JavaScript** ✅ Recommandé

```java
// ai.quadratic.neural.visualization.ChartExporter
public class ChartExporter {
    
    public void exportLossCurve(TrainingHistory history, Path output) throws IOException {
        String html = generateHtmlTemplate(history);
        Files.writeString(output, html);
    }
    
    private String generateHtmlTemplate(TrainingHistory history) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
        <!DOCTYPE html>
        <html>
        <head>
            <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
        </head>
        <body>
            <canvas id="lossChart"></canvas>
            <script>
                const ctx = document.getElementById('lossChart');
                new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: %s,
                        datasets: [
                            {
                                label: 'Train Loss',
                                data: %s,
                                borderColor: 'rgb(75, 192, 192)'
                            },
                            {
                                label: 'Val Loss',
                                data: %s,
                                borderColor: 'rgb(255, 99, 132)'
                            }
                        ]
                    }
                });
            </script>
        </body>
        </html>
        """.formatted(
            toJsonArray(history.getEpochs()),
            toJsonArray(history.getTrainLosses()),
            toJsonArray(history.getValLosses())
        ));
        return sb.toString();
    }
}
```

**Usage** :
```java
// Après entraînement
ChartExporter exporter = new ChartExporter();
exporter.exportLossCurve(history, Path.of("loss_curve.html"));
System.out.println("Ouvrez loss_curve.html dans un navigateur pour voir la courbe.");
```

**Estimation** : 2 jours  
**Impact** : ⭐⭐⭐⭐ (pédagogique fort)

---

### FUNC-3 : Mode Comparaison (Symbolic vs Neural)

**Objectif** : Comparer les résultats des deux approches sur une même équation

**Implémentation** :
```java
// Nouveau mode : java -jar quadratic-ai-all.jar compare

public static void runComparison() {
    Scanner scanner = new Scanner(System.in);
    QuadraticSolver symbolic = new QuadraticSolver();
    NeuralNetwork neural = loadOrTrainNetwork();
    
    System.out.println("Mode Comparaison : Symbolic vs Neural");
    
    while (true) {
        System.out.print("Équation (a b c) ou 'q' : ");
        String line = scanner.nextLine();
        if (line.equals("q")) break;
        
        String[] parts = line.split(" ");
        double a = Double.parseDouble(parts[0]);
        double b = Double.parseDouble(parts[1]);
        double c = Double.parseDouble(parts[2]);
        
        ParsedEquation eq = new ParsedEquation(a, b, c, line);
        
        // Symbolic
        long t1 = System.nanoTime();
        Solution symSol = symbolic.solve(eq);
        long t2 = System.nanoTime();
        
        // Neural
        double[] input = normalize(a, b, c);
        long t3 = System.nanoTime();
        double[] pred = neural.forward(input);
        long t4 = System.nanoTime();
        
        // Affichage comparatif
        System.out.println("\n📊 COMPARAISON");
        System.out.println("─".repeat(60));
        System.out.printf("Symbolic : %s (temps: %.2f µs)%n", formatSolution(symSol), (t2-t1)/1000.0);
        System.out.printf("Neural   : x1=%.4f x2=%.4f Δ=%.4f (temps: %.2f µs)%n",
            denorm(pred[0]), denorm(pred[1]), denorm(pred[2]), (t4-t3)/1000.0);
        
        // Calcul de l'erreur
        double[] symTarget = extractTarget(symSol);
        double mse = computeMSE(pred, symTarget);
        System.out.printf("Erreur (MSE) : %.6f%n", mse);
    }
}
```

**Estimation** : 1 jour  
**Impact** : ⭐⭐⭐⭐⭐ (pédagogique maximal)

---

## 4. Améliorations Techniques

### TECH-1 : Gradient Checking (validation backpropagation)

**Objectif** : Vérifier que le backprop calcule correctement les gradients

**Principe** : Comparer gradient analytique (backprop) vs gradient numérique

```java
// ai.quadratic.neural.testing.GradientChecker
public class GradientChecker {
    
    private static final double EPSILON = 1e-4;
    
    public boolean check(NeuralNetwork network, double[] input, double[] target) {
        // 1. Forward + backward pour obtenir gradients analytiques
        network.trainStep(input, target);
        
        // 2. Pour chaque poids, calculer gradient numérique
        for (int l = 0; l < network.getLayerCount(); l++) {
            Layer layer = network.getLayer(l);
            for (int n = 0; n < layer.neurons.length; n++) {
                Neuron neuron = layer.neurons[n];
                for (int w = 0; w < neuron.weights.length; w++) {
                    
                    double original = neuron.weights[w];
                    
                    // f(w + ε)
                    neuron.weights[w] = original + EPSILON;
                    double lossPlus = network.evaluate(input, target);
                    
                    // f(w - ε)
                    neuron.weights[w] = original - EPSILON;
                    double lossMinus = network.evaluate(input, target);
                    
                    // Gradient numérique
                    double numGrad = (lossPlus - lossMinus) / (2 * EPSILON);
                    
                    // Gradient analytique (backprop)
                    double analyticGrad = neuron.delta * input[w];
                    
                    // Comparaison
                    double relativeError = Math.abs(numGrad - analyticGrad) / 
                                           (Math.abs(numGrad) + Math.abs(analyticGrad) + 1e-8);
                    
                    if (relativeError > 1e-5) {
                        System.err.printf("ERREUR Gradient: L%d N%d W%d → num=%.6f analytic=%.6f%n",
                            l, n, w, numGrad, analyticGrad);
                        return false;
                    }
                    
                    neuron.weights[w] = original;  // Restaurer
                }
            }
        }
        return true;
    }
}
```

**Usage dans les tests** :
```java
@Test
void testGradientCheckingPasses() {
    NeuralNetwork net = new NeuralNetwork(new int[]{3, 8, 3}, 0.001);
    GradientChecker checker = new GradientChecker();
    
    double[] input = {0.5, -0.3, 0.2};
    double[] target = {0.8, -0.6, 0.1};
    
    assertTrue(checker.check(net, input, target), "Gradients doivent être corrects");
}
```

**Estimation** : 1 jour  
**Impact** : ⭐⭐⭐⭐ (fiabilité du réseau)

---

### TECH-2 : Parallélisation de l'Entraînement (Virtual Threads)

**Objectif** : Accélérer l'entraînement en utilisant les Virtual Threads Java 21

**Approche** : Paralléliser le traitement des mini-batches

```java
// Dans Trainer.java
private double runEpochParallel(List<Sample> samples) throws InterruptedException {
    int batchCount = (samples.size() + config.batchSize() - 1) / config.batchSize();
    double[] batchLosses = new double[batchCount];
    
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < batchCount; i++) {
            final int batchIdx = i;
            futures.add(executor.submit(() -> {
                int start = batchIdx * config.batchSize();
                int end = Math.min(start + config.batchSize(), samples.size());
                double loss = 0.0;
                
                // Synchronisation sur le réseau pour éviter les data races
                synchronized (network) {
                    for (int j = start; j < end; j++) {
                        Sample s = samples.get(j);
                        loss += network.trainStep(s.input(), s.target());
                    }
                }
                
                batchLosses[batchIdx] = loss / (end - start);
            }));
        }
        
        // Attendre tous les batches
        for (var future : futures) {
            future.get();
        }
    }
    
    return Arrays.stream(batchLosses).average().orElse(0.0);
}
```

**Avertissement** : La synchronisation peut annuler le gain de performance pour de petits réseaux. À tester !

**Estimation** : 2 jours  
**Impact** : ⭐⭐⭐ (gain de vitesse variable)

---

### TECH-3 : Métriques Avancées (MAE, R², MAPE)

**Objectif** : Aller au-delà de MSE pour évaluer le réseau

```java
// ai.quadratic.neural.metrics.RegressionMetrics
public class RegressionMetrics {
    
    // Mean Absolute Error
    public static double mae(double[] predicted, double[] target) {
        double sum = 0;
        for (int i = 0; i < predicted.length; i++) {
            sum += Math.abs(predicted[i] - target[i]);
        }
        return sum / predicted.length;
    }
    
    // R² (coefficient of determination)
    public static double r2Score(List<Sample> samples, NeuralNetwork network) {
        double ssTot = 0, ssRes = 0;
        double[] means = computeMeans(samples);
        
        for (Sample s : samples) {
            double[] pred = network.forward(s.input());
            for (int i = 0; i < s.target().length; i++) {
                ssRes += Math.pow(s.target()[i] - pred[i], 2);
                ssTot += Math.pow(s.target()[i] - means[i], 2);
            }
        }
        
        return 1.0 - (ssRes / ssTot);
    }
    
    // Mean Absolute Percentage Error
    public static double mape(double[] predicted, double[] target) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < predicted.length; i++) {
            if (Math.abs(target[i]) > 1e-8) {  // Éviter division par 0
                sum += Math.abs((target[i] - predicted[i]) / target[i]);
                count++;
            }
        }
        return count > 0 ? (sum / count) * 100 : 0;
    }
}
```

**Affichage dans Trainer** :
```java
System.out.println("\n📊 MÉTRIQUES FINALES");
System.out.printf("MSE  : %.6f%n", avgMSE);
System.out.printf("MAE  : %.6f%n", RegressionMetrics.mae(...));
System.out.printf("R²   : %.6f%n", RegressionMetrics.r2Score(valSet, network));
System.out.printf("MAPE : %.2f%%%n", RegressionMetrics.mape(...));
```

**Estimation** : 0.5 jour  
**Impact** : ⭐⭐⭐ (évaluation plus complète)

---

## 5. Améliorations Pédagogiques

### PEDA-1 : Glossaire Interactif

**Objectif** : Commande `aide [terme]` dans la Phase 1 pour expliquer les concepts

```java
// ai.quadratic.symbolic.help.GlossaryManager
public class GlossaryManager {
    private static final Map<String, String> GLOSSARY = Map.ofEntries(
        Map.entry("discriminant", """
            Le DISCRIMINANT (Δ) est la quantité b² - 4ac.
            
            • Δ > 0 : deux racines réelles distinctes (parabole coupe l'axe x deux fois)
            • Δ = 0 : une racine double (parabole touche l'axe x en un point)
            • Δ < 0 : pas de racine réelle (parabole ne coupe pas l'axe x)
            
            Exemple : x² - 5x + 6 = 0
            Δ = (-5)² - 4(1)(6) = 25 - 24 = 1 > 0 → deux racines
            """),
        
        Map.entry("activation", """
            Fonction d'ACTIVATION : transforme la sortie d'un neurone.
            
            • ReLU : max(0, z) → évite les gradients qui disparaissent
            • Sigmoid : 1/(1+e^-z) → sortie entre 0 et 1
            • Tanh : (e^z - e^-z)/(e^z + e^-z) → sortie entre -1 et 1
            • Linear : z → utilisée en sortie pour la régression
            """),
        
        Map.entry("backpropagation", """
            RÉTROPROPAGATION (backpropagation) : algorithme pour calculer
            les gradients (∂Loss/∂w) dans un réseau de neurones.
            
            Étapes :
            1. Forward : calculer la sortie du réseau
            2. Loss : mesurer l'erreur entre prédiction et cible
            3. Backward : propager l'erreur de la sortie vers l'entrée
            4. Update : ajuster les poids avec l'optimiseur (Adam, SGD...)
            
            Inventé par Rumelhart, Hinton & Williams (1986).
            """)
        // ... 20+ termes
    );
    
    public String explain(String term) {
        String normalized = term.toLowerCase().trim();
        return GLOSSARY.getOrDefault(normalized,
            "Terme inconnu. Tapez 'aide glossaire' pour voir la liste complète.");
    }
    
    public String listAll() {
        return "Termes disponibles : " + String.join(", ", GLOSSARY.keySet());
    }
}
```

**Intégration dans DialogueManager** :
```java
private String handleGlobalCommands(String input) {
    if (input.startsWith("aide ")) {
        String term = input.substring(5);
        return glossary.explain(term);
    }
    // ...
}
```

**Estimation** : 2 jours (rédaction du contenu)  
**Impact** : ⭐⭐⭐⭐⭐ (pédagogie self-service)

---

### PEDA-2 : Mode "Explain" (étape par étape)

**Objectif** : Afficher chaque étape du calcul avec pauses

```java
// Nouveau mode : java -jar quadratic-ai-all.jar explain

public static void runExplainMode() {
    Scanner scanner = new Scanner(System.in);
    System.out.println("Mode EXPLICATION : chaque étape sera détaillée\n");
    
    System.out.print("Entrez l'équation (a b c) : ");
    String[] parts = scanner.nextLine().split(" ");
    double a = Double.parseDouble(parts[0]);
    double b = Double.parseDouble(parts[1]);
    double c = Double.parseDouble(parts[2]);
    
    pause("Équation reçue : " + a + "x² + " + b + "x + " + c + " = 0");
    
    pause("Étape 1 : Calcul du discriminant Δ = b² - 4ac");
    double bSquared = b * b;
    pause("  b² = (" + b + ")² = " + bSquared);
    double fourAC = 4 * a * c;
    pause("  4ac = 4 × " + a + " × " + c + " = " + fourAC);
    double delta = bSquared - fourAC;
    pause("  Δ = " + bSquared + " - " + fourAC + " = " + delta);
    
    if (delta > 0) {
        pause("Étape 2 : Δ > 0 → deux racines réelles");
        double sqrtDelta = Math.sqrt(delta);
        pause("  √Δ = √" + delta + " = " + sqrtDelta);
        pause("Étape 3 : Formule x₁ = (-b + √Δ) / 2a");
        double x1Num = -b + sqrtDelta;
        pause("  Numérateur : -b + √Δ = " + (-b) + " + " + sqrtDelta + " = " + x1Num);
        double denom = 2 * a;
        pause("  Dénominateur : 2a = 2 × " + a + " = " + denom);
        double x1 = x1Num / denom;
        pause("  x₁ = " + x1Num + " / " + denom + " = " + x1);
        
        // Idem pour x2...
    }
    // ... autres cas
}

private static void pause(String message) {
    System.out.println(message);
    System.out.print("  [Appuyez sur Entrée pour continuer] ");
    new Scanner(System.in).nextLine();
}
```

**Estimation** : 1 jour  
**Impact** : ⭐⭐⭐⭐ (débutants absol)

---

### PEDA-3 : Visualisation des Poids du Réseau

**Objectif** : Afficher les poids sous forme de heatmap ASCII

```java
// ai.quadratic.neural.visualization.WeightVisualizer
public class WeightVisualizer {
    
    public void printLayerWeights(Layer layer, int layerIndex) {
        System.out.println("\n🔬 POIDS DE LA COUCHE " + layerIndex);
        System.out.println("─".repeat(60));
        
        for (int n = 0; n < Math.min(5, layer.neurons.length); n++) {  // 5 premiers neurones
            Neuron neuron = layer.neurons[n];
            System.out.printf("Neurone %d (bias=%.3f) :%n", n, neuron.bias);
            System.out.print("  Poids : ");
            for (double w : neuron.weights) {
                System.out.print(toBar(w) + " ");
            }
            System.out.println();
        }
        if (layer.neurons.length > 5) {
            System.out.println("  ... (" + (layer.neurons.length - 5) + " neurones supplémentaires)");
        }
    }
    
    private String toBar(double weight) {
        // Convertir poids en barre ASCII
        int length = (int) (Math.abs(weight) * 10);
        length = Math.min(length, 20);
        String bar = "█".repeat(length);
        return weight >= 0 ? ("+" + bar) : ("-" + bar);
    }
    
    public void printWeightHistogram(NeuralNetwork network) {
        List<Double> allWeights = new ArrayList<>();
        for (int l = 0; l < network.getLayerCount(); l++) {
            Layer layer = network.getLayer(l);
            for (Neuron n : layer.neurons) {
                for (double w : n.weights) {
                    allWeights.add(w);
                }
            }
        }
        
        // Histogramme ASCII
        int[] bins = new int[10];
        for (double w : allWeights) {
            int bin = (int) ((w + 1) / 0.2);  // Bins de -1 à 1
            bin = Math.max(0, Math.min(9, bin));
            bins[bin]++;
        }
        
        System.out.println("\n📊 DISTRIBUTION DES POIDS");
        for (int i = 0; i < bins.length; i++) {
            double rangeStart = -1 + i * 0.2;
            System.out.printf("[%5.2f,%5.2f[ : %s (%d)%n",
                rangeStart, rangeStart + 0.2, "█".repeat(bins[i] / 10), bins[i]);
        }
    }
}
```

**Usage** :
```java
// Après entraînement
WeightVisualizer viz = new WeightVisualizer();
viz.printWeightHistogram(network);
viz.printLayerWeights(network.getLayer(0), 0);
```

**Estimation** : 1 jour  
**Impact** : ⭐⭐⭐⭐ (compréhension interne)

---

## 6. Améliorations Avancées

### ADV-1 : Mini-Transformer avec Mécanisme d'Attention

**Objectif** : Implémenter une version simplifiée du mécanisme d'attention

**Contexte pédagogique** : Comprendre le cœur des LLMs (GPT, BERT...)

**Architecture** :
```
Entrée : [a, b, c]  (3 tokens)
   ↓
Embedding : 3 × 64  (dimension cachée)
   ↓
Self-Attention :
   Q = X·W_Q  (3×64)
   K = X·W_K  (3×64)
   V = X·W_V  (3×64)
   Attention(Q,K,V) = softmax(QK^T / √d_k) · V
   ↓
Feed-Forward Network :
   FFN(x) = ReLU(x·W1 + b1)·W2 + b2
   ↓
Output : [x1, x2, Δ]  (3 sorties)
```

**Implémentation** :
```java
// ai.quadratic.neural.transformer.AttentionLayer
public class AttentionLayer {
    private final int dModel;    // Dimension du modèle (ex: 64)
    private final int dK;        // Dimension des clés (ex: 16)
    private final double[][] wQ, wK, wV, wO;  // Matrices de projection
    
    public AttentionLayer(int dModel, int dK, Random rng) {
        this.dModel = dModel;
        this.dK = dK;
        this.wQ = initMatrix(dModel, dK, rng);
        this.wK = initMatrix(dModel, dK, rng);
        this.wV = initMatrix(dModel, dK, rng);
        this.wO = initMatrix(dK, dModel, rng);
    }
    
    public double[][] forward(double[][] input) {
        // input: [seq_len=3, d_model=64]
        int seqLen = input.length;
        
        // Projections Q, K, V
        double[][] Q = matmul(input, wQ);  // [3, 16]
        double[][] K = matmul(input, wK);  // [3, 16]
        double[][] V = matmul(input, wV);  // [3, 16]
        
        // Scores d'attention : QK^T / √d_k
        double[][] scores = matmul(Q, transpose(K));  // [3, 3]
        for (int i = 0; i < seqLen; i++) {
            for (int j = 0; j < seqLen; j++) {
                scores[i][j] /= Math.sqrt(dK);
            }
        }
        
        // Softmax sur chaque ligne
        double[][] attnWeights = softmax2D(scores);
        
        // Contexte : Attention · V
        double[][] context = matmul(attnWeights, V);  // [3, 16]
        
        // Projection de sortie
        return matmul(context, wO);  // [3, 64]
    }
    
    private double[][] softmax2D(double[][] matrix) {
        double[][] result = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            double max = Arrays.stream(matrix[i]).max().orElse(0);
            double sum = 0;
            for (int j = 0; j < matrix[i].length; j++) {
                result[i][j] = Math.exp(matrix[i][j] - max);
                sum += result[i][j];
            }
            for (int j = 0; j < matrix[i].length; j++) {
                result[i][j] /= sum;
            }
        }
        return result;
    }
    
    // matmul, transpose, initMatrix...
}

// ai.quadratic.neural.transformer.TransformerBlock
public class TransformerBlock {
    private final AttentionLayer attention;
    private final Layer ffn1, ffn2;
    
    public double[][] forward(double[][] input) {
        // 1. Self-attention + residual connection
        double[][] attnOut = attention.forward(input);
        double[][] residual1 = add(input, attnOut);  // Residual connection
        
        // 2. Layer Normalization (simplifiée)
        double[][] norm1 = layerNorm(residual1);
        
        // 3. Feed-Forward Network + residual
        double[][] ffnOut = ffn2.forward(ffn1.forward(flatten(norm1)));
        double[][] residual2 = add(norm1, reshape(ffnOut));
        
        // 4. Layer Normalization finale
        return layerNorm(residual2);
    }
}
```

**Module Maven dédié** : `quadratic-transformer/`

**Estimation** : 5-7 jours (complexe)  
**Impact** : ⭐⭐⭐⭐⭐ (pédagogique maximal, comprendre les LLMs)

---

### ADV-2 : Extension aux Équations Cubiques

**Objectif** : Généraliser à ax³ + bx² + cx + d = 0

**Changements nécessaires** :

1. **ParsedEquation** → `ParsedCubicEquation(a, b, c, d)`
2. **Solver** → Méthode de Cardan ou Newton-Raphson
3. **Réseau** → Architecture [4, 64, 64, 32, 4] (4 entrées, jusqu'à 3 racines + type)
4. **DatasetGenerator** → Génération équations cubiques

**Complexité** : Équations cubiques ont 1 à 3 racines réelles (formule de Cardan complexe)

**Estimation** : 4-5 jours  
**Impact** : ⭐⭐⭐⭐ (généralisation du concept)

---

### ADV-3 : API REST + Frontend Web

**Objectif** : Interface web pour résoudre des équations

**Stack** :
- Backend : Javalin (micro-framework REST, ~50 Ko)
- Frontend : HTML/CSS/JS vanilla (pas de React pour rester simple)

**Endpoints** :
```
POST /api/solve
{
  "a": 1.0,
  "b": -5.0,
  "c": 6.0,
  "solver": "symbolic"  // ou "neural"
}
→ 
{
  "solution": {
    "type": "TwoRealRoots",
    "x1": 3.0,
    "x2": 2.0,
    "delta": 1.0
  },
  "computation_time_ms": 0.5
}

POST /api/train
{
  "epochs": 50,
  "dataset_size": 10000
}
→
{
  "status": "training_started",
  "job_id": "abc123"
}

GET /api/train/status/abc123
→
{
  "status": "running",
  "progress": 0.45,
  "current_epoch": 23,
  "train_loss": 0.00012
}
```

**Frontend** : SPA simple avec fetch API

**Estimation** : 5-7 jours  
**Impact** : ⭐⭐⭐⭐ (accessibilité, démo)

---

## 7. Roadmap d'Implémentation

### Phase 1 : Fondations (2-3 semaines)

| Semaine | Tâches | Estimation |
|---------|--------|------------|
| **S1** | PRIO-1 (Tests), PRIO-2 (Config), PRIO-3 (CI/CD) | 5 jours |
| **S2** | PRIO-4 (Abstractions), PRIO-5 (Batch), FUNC-1 (History) | 5 jours |
| **S3** | FUNC-2 (Visualisation), TECH-1 (Gradient Check), TECH-3 (Métriques) | 4 jours |

**Livrable S3** : Version 1.5.0 — Base solide pour extensions

---

### Phase 2 : Pédagogie Avancée (2 semaines)

| Semaine | Tâches | Estimation |
|---------|--------|------------|
| **S4** | PEDA-1 (Glossaire), PEDA-2 (Explain Mode), PEDA-3 (Poids viz) | 4 jours |
| **S5** | FUNC-3 (Compare Mode), Documentation mise à jour | 3 jours |

**Livrable S5** : Version 1.8.0 — POC pédagogique complet

---

### Phase 3 : Features Avancées (3-4 semaines)

| Semaine | Tâches | Estimation |
|---------|--------|------------|
| **S6-S7** | ADV-1 (Mini-Transformer) | 7 jours |
| **S8** | ADV-2 (Équations cubiques) | 5 jours |
| **S9** | ADV-3 (API REST + Frontend) (optionnel) | 7 jours |

**Livrable S9** : Version 2.0.0 — POC IA/GenAI avancé

---

## 8. Spécifications Détaillées

### 8.1. Structure Maven Cible (v2.0)

```
quadratic-ai/
├── quadratic-core/          (inchangé)
├── quadratic-symbolic/      (+ tests, + glossaire)
├── quadratic-neural/        (+ tests, + métriques, + viz)
├── quadratic-transformer/   ← NOUVEAU (mini-Transformer)
├── quadratic-web/           ← NOUVEAU (API REST Javalin)
├── quadratic-cli/           ← NOUVEAU (CLI avancée avec Picocli)
└── quadratic-app/           (orchestration globale)
```

### 8.2. Tests Cible (Couverture > 80%)

```
Total tests : ~100
├── quadratic-core (20 tests)
│   ├── SolverTest (12)
│   └── ValidatorTest (8)
├── quadratic-symbolic (25 tests)
│   ├── DialogueManagerTest (15)
│   └── PromptParserTest (10)
├── quadratic-neural (35 tests)
│   ├── NeuralNetworkTest (15)
│   ├── AdamOptimizerTest (8)
│   ├── DatasetGeneratorTest (7)
│   └── TrainerTest (5)
└── quadratic-transformer (20 tests)
    ├── AttentionLayerTest (10)
    └── TransformerBlockTest (10)
```

### 8.3. Configuration Cible (`config.properties`)

```properties
# === Mode ===
mode=phase2  # phase1, phase2, train, batch, compare, explain

# === Neural Network ===
network.architecture=3,32,32,16,3
network.learningRate=0.001
network.dropoutRate=0.1
network.activation.hidden=RELU
network.activation.output=LINEAR

# === Optimizer ===
optimizer.type=ADAM  # ADAM, SGD, ADAMW, RMSPROP
optimizer.adam.beta1=0.9
optimizer.adam.beta2=0.999
optimizer.sgd.momentum=0.9
optimizer.sgd.nesterov=true

# === Training ===
training.epochs=100
training.batchSize=32
training.patience=5
training.minDelta=1e-5
training.datasetSize=10000
training.splitRatio=0.8
training.shuffle=true
training.parallelBatches=false

# === Dataset ===
dataset.distribution.twoRoots=0.60
dataset.distribution.oneRoot=0.20
dataset.distribution.complex=0.20
dataset.coeffRange=10.0
dataset.rootRange=20.0

# === Persistence ===
model.savePath=model.json
model.autoSave=true
model.compression=false

# === Logging ===
logging.level=INFO  # TRACE, DEBUG, INFO, WARN, ERROR
logging.printEvery=5
logging.exportCsv=true
logging.csvPath=loss_curve.csv
logging.exportHtml=true
logging.htmlPath=loss_curve.html

# === Visualization ===
viz.weightsOnFinish=false
viz.histogramOnFinish=true
viz.comparisonMode=false

# === Web API (si quadratic-web activé) ===
web.enabled=false
web.port=8080
web.cors.enabled=true
```

### 8.4. Documentation Cible

```
docs/
├── README.md                   (actuel, mis à jour)
├── ARCHITECTURE.md             (diagrammes UML, flux)
├── GLOSSARY.md                 (tous les termes ML/DL)
├── TUTORIALS/
│   ├── 01-getting-started.md
│   ├── 02-symbolic-ai.md
│   ├── 03-neural-networks.md
│   ├── 04-training-tuning.md
│   ├── 05-transformer-intro.md
│   └── 06-web-api.md
├── API/
│   ├── core-api.md
│   ├── neural-api.md
│   └── rest-api.md
└── VIDEOS/
    ├── demo-phase1.mp4
    ├── demo-phase2.mp4
    └── explain-backprop.mp4
```

---

## Conclusion

Ces améliorations transformeront QuadraticAI en un **POC de référence** pour l'apprentissage de l'IA et du Generative AI en Java :

✅ **Qualité industrielle** : Tests, CI/CD, configuration externe  
✅ **Extensibilité** : Abstractions, plugins, modularité  
✅ **Pédagogie avancée** : Glossaire, mode explain, visualisations  
✅ **Concepts modernes** : Transformer, attention, métriques avancées  
✅ **Accessibilité** : Web API, mode batch, compare mode  

**Priorités recommandées pour démarrer** :
1. PRIO-1 (Tests) — garantir la non-régression
2. PRIO-2 (Config) — faciliter l'expérimentation
3. PRIO-3 (CI/CD) — automatiser la qualité
4. PRIO-4 (Abstractions) — préparer les extensions

**Estimation totale : 8-10 semaines de développement solo**

---

*Propositions d'amélioration rédigées le 23 Mars 2026 par GitHub Copilot*
