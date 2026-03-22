package ai.quadratic.core;

import ai.quadratic.core.model.ParsedEquation;
import ai.quadratic.core.model.Solution;
import ai.quadratic.core.solver.QuadraticSolver;
import ai.quadratic.core.validator.InputValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le module core.
 *
 * <p>Couvre : QuadraticSolver (3 cas de Δ, cas limites),
 * InputValidator (formats valides, invalides, a=0, commandes).</p>
 */
@DisplayName("Tests du module quadratic-core")
class CoreTest {

    private final QuadraticSolver solver = new QuadraticSolver();
    private final InputValidator  validator = new InputValidator();

    // ── Tests QuadraticSolver ────────────────────────────────────────────────

    @Nested
    @DisplayName("QuadraticSolver")
    class SolverTest {

        @Test
        @DisplayName("Δ > 0 → deux racines réelles : x²-5x+6=0 → x1=3, x2=2")
        void twoRealRoots() {
            var eq  = new ParsedEquation(1, -5, 6, "test");
            var sol = solver.solve(eq);

            assertInstanceOf(Solution.TwoRealRoots.class, sol);
            var r = (Solution.TwoRealRoots) sol;
            assertEquals(3.0, r.x1(), 1e-9, "x1 doit être 3.0");
            assertEquals(2.0, r.x2(), 1e-9, "x2 doit être 2.0");
            assertTrue(r.delta() > 0, "delta doit être positif");
        }

        @Test
        @DisplayName("Δ = 0 → racine double : x²+2x+1=0 → x0=-1")
        void oneDoubleRoot() {
            var eq  = new ParsedEquation(1, 2, 1, "test");
            var sol = solver.solve(eq);

            assertInstanceOf(Solution.OneDoubleRoot.class, sol);
            var r = (Solution.OneDoubleRoot) sol;
            assertEquals(-1.0, r.x0(), 1e-9, "x0 doit être -1.0");
            assertEquals(0.0, r.delta(), 1e-9, "delta doit être 0");
        }

        @Test
        @DisplayName("Δ < 0 → racines complexes : x²+1=0")
        void complexRoots() {
            var eq  = new ParsedEquation(1, 0, 1, "test");
            var sol = solver.solve(eq);

            assertInstanceOf(Solution.ComplexRoots.class, sol);
            var r = (Solution.ComplexRoots) sol;
            assertEquals(0.0, r.realPart(), 1e-9, "partie réelle = 0");
            assertEquals(1.0, r.imagPart(), 1e-9, "partie imaginaire = 1");
            assertTrue(r.delta() < 0, "delta doit être négatif");
        }

        @Test
        @DisplayName("Vérification : substituer x dans f(x) doit donner ≈ 0")
        void verificationIsNearZero() {
            var eq  = new ParsedEquation(2, -5, 3, "test");
            var sol = (Solution.TwoRealRoots) solver.solve(eq);

            assertEquals(0.0, solver.verify(eq, sol.x1()), 1e-9, "f(x1) ≈ 0");
            assertEquals(0.0, solver.verify(eq, sol.x2()), 1e-9, "f(x2) ≈ 0");
        }

        @Test
        @DisplayName("a = 0 → IllegalArgumentException attendue")
        void aEqualsZeroThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> new ParsedEquation(0, 5, 3, "test"),
                "a=0 doit lever une exception"
            );
        }

        @Test
        @DisplayName("Coefficients décimaux : 0.5x²-1.5x+1=0")
        void decimalCoefficients() {
            var eq  = new ParsedEquation(0.5, -1.5, 1.0, "test");
            var sol = solver.solve(eq);
            assertInstanceOf(Solution.TwoRealRoots.class, sol);
        }
    }

    // ── Tests InputValidator ─────────────────────────────────────────────────

    @Nested
    @DisplayName("InputValidator")
    class ValidatorTest {

        @ParameterizedTest(name = "Entrée valide : {0}")
        @ValueSource(strings = {"2", "-3.5", "+1.0", "0.001", "100", "1/2", "-3/4"})
        void acceptsValidInputs(String input) {
            var result = validator.validate(input, false, 1);
            assertTrue(result.valid(), "Devrait accepter : " + input);
        }

        @Test
        @DisplayName("Virgule → point : '2,5' devient 2.5")
        void commaConvertedToPoint() {
            var result = validator.validate("2,5", false, 1);
            assertTrue(result.valid());
            assertEquals(2.5, result.value(), 1e-9);
        }

        @Test
        @DisplayName("Fraction 1/2 → 0.5")
        void fractionParsed() {
            var result = validator.validate("1/2", false, 1);
            assertTrue(result.valid());
            assertEquals(0.5, result.value(), 1e-9);
        }

        @ParameterizedTest(name = "Entrée invalide rejetée : {0}")
        @ValueSource(strings = {"deux", "2x", "abc", "2@#", "null", "", "  "})
        void rejectsInvalidInputs(String input) {
            var result = validator.validate(input, false, 1);
            assertFalse(result.valid(), "Devrait rejeter : " + input);
            assertNotNull(result.errorMessage(), "Message d'erreur attendu");
        }

        @Test
        @DisplayName("a = 0 → message d'erreur spécifique")
        void aEqualsZeroGivesSpecificMessage() {
            var result = validator.validate("0", true, 1);
            assertFalse(result.valid());
            assertTrue(result.errorMessage().contains("ne peut pas être 0"),
                "Message doit expliquer pourquoi a≠0");
        }

        @Test
        @DisplayName("Commande 'quitter' → signal de commande")
        void conversationalCommandDetected() {
            var result = validator.validate("quitter", false, 1);
            assertFalse(result.valid());
            assertTrue(result.errorMessage().startsWith("__COMMAND__"),
                "Doit signaler une commande, pas une erreur de format");
        }
    }
}
