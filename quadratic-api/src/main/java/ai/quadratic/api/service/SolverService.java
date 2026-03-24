package ai.quadratic.api.service;
import ai.quadratic.api.dto.EquationRequest;
import ai.quadratic.api.dto.SolveResponse;
import ai.quadratic.core.model.ParsedEquation;
import ai.quadratic.core.model.Solution;
import ai.quadratic.core.solver.QuadraticSolver;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
public class SolverService {
    private final QuadraticSolver solver = new QuadraticSolver();
    public SolveResponse solve(EquationRequest req) {
        double a = req.a(), b = req.b(), c = req.c();
        ParsedEquation eq = new ParsedEquation(a, b, c, buildEquationStr(a, b, c));
        Solution solution = solver.solve(eq);
        double delta = solver.computeDelta(a, b, c);
        List<String> steps = buildSteps(a, b, c, delta, solution);
        return switch (solution) {
            case Solution.TwoRealRoots r  -> new SolveResponse("TWO_REAL",   delta, r.x1(),       r.x2(),       eq.toStandardForm(), steps);
            case Solution.OneDoubleRoot r -> new SolveResponse("ONE_DOUBLE", delta, r.x0(),       r.x0(),       eq.toStandardForm(), steps);
            case Solution.ComplexRoots  r -> new SolveResponse("COMPLEX",    delta, r.realPart(), r.imagPart(), eq.toStandardForm(), steps);
        };
    }
    private List<String> buildSteps(double a, double b, double c, double delta, Solution solution) {
        List<String> steps = new ArrayList<>();
        steps.add("Equation : " + buildEquationStr(a, b, c) + " = 0");
        steps.add("Delta = b^2 - 4ac = " + fmt(b) + "^2 - 4*" + fmt(a) + "*" + fmt(c) + " = " + fmt(delta));
        switch (solution) {
            case Solution.TwoRealRoots r  -> { steps.add("Delta > 0 : deux racines reelles"); steps.add("x1 = " + fmt(r.x1())); steps.add("x2 = " + fmt(r.x2())); }
            case Solution.OneDoubleRoot r -> { steps.add("Delta = 0 : racine double"); steps.add("x0 = " + fmt(r.x0())); }
            case Solution.ComplexRoots  r -> { steps.add("Delta < 0 : racines complexes"); steps.add("z1 = " + r.z1AsString()); steps.add("z2 = " + r.z2AsString()); }
        }
        return steps;
    }
    private String buildEquationStr(double a, double b, double c) {
        StringBuilder sb = new StringBuilder();
        if (a == 1) sb.append("x^2"); else if (a == -1) sb.append("-x^2"); else sb.append(fmt(a)).append("x^2");
        if (b > 0) sb.append(" + ").append(fmt(b)).append("x"); else if (b < 0) sb.append(" - ").append(fmt(Math.abs(b))).append("x");
        if (c > 0) sb.append(" + ").append(fmt(c)); else if (c < 0) sb.append(" - ").append(fmt(Math.abs(c)));
        return sb.toString();
    }
    private String fmt(double d) {
        return (d == Math.floor(d) && !Double.isInfinite(d)) ? String.valueOf((long) d) : String.format("%.4f", d);
    }
}