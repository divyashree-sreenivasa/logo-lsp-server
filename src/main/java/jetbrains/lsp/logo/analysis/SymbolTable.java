package jetbrains.lsp.logo.analysis;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static jetbrains.lsp.logo.parser.PositionConverter.contains;

public class SymbolTable {

    /** MAKE "varname declaration. */
    public record MakeVariable(String name, Range nameRange) {}

    /** FOR [i ...] loop variable — scoped to the FOR statement body. */
    public record ForLoopVariable(String name, Range nameRange, Range scopeRange) {}

    private final Map<String, ProcedureSymbol> procedures = new HashMap<>();
    private final Map<String, List<MakeVariable>> makeVariables = new HashMap<>();
    private final List<ForLoopVariable> forLoopVariables = new ArrayList<>();

    // Procedures
    public void addProcedure(ProcedureSymbol symbol) {
        procedures.put(symbol.getName().toUpperCase(), symbol);
    }

    public ProcedureSymbol getProcedure(String name) {
        return procedures.get(name.toUpperCase());
    }

    public boolean hasProcedure(String name) {
        return procedures.containsKey(name.toUpperCase());
    }

    public Collection<ProcedureSymbol> allProcedures() {
        return procedures.values();
    }

    public Optional<ProcedureSymbol> enclosingProcedure(Position position) {
        return procedures.values().stream()
                .filter(p -> contains(p.getDefinitionRange(), position))
                .findFirst();
    }

    // MAKE variables

    public void addMakeVariable(MakeVariable variable) {
        makeVariables
                .computeIfAbsent(variable.name().toUpperCase(), k -> new ArrayList<>())
                .add(variable);
    }

    public Optional<MakeVariable> getMakeVariable(String name) {
        List<MakeVariable> all = makeVariables.get(name.toUpperCase());
        return (all == null || all.isEmpty()) ? Optional.empty() : Optional.of(all.get(0));
    }

    // FOR loop variables

    public void addForLoopVariable(ForLoopVariable variable) {
        forLoopVariables.add(variable);
    }

    /**
     * Finds the innermost FOR loop variable with the given name whose scope
     * contains the cursor position. "Innermost" = smallest scope range (latest start).
     */
    public Optional<ForLoopVariable> getForLoopVariable(String name, Position cursor) {
        String upper = name.toUpperCase();
        return forLoopVariables.stream()
                .filter(v -> v.name().equals(upper) && contains(v.scopeRange(), cursor))
                // prefer the innermost (latest-starting) scope
                .max(Comparator.comparingInt(v -> v.scopeRange().getStart().getLine()));
    }
}
