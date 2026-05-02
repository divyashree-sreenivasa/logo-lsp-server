package jetbrains.lsp.logo.analysis;

import org.eclipse.lsp4j.Range;

import java.util.List;
import java.util.Map;

/**
 * Represents a user-defined LOGO procedure collected during symbol table construction.
 *
 * Example:  TO square :side
 *             ...
 *           END
 *
 * nameRange       — the "square" token only (go-to-declaration target)
 * definitionRange — from TO to END (used to find enclosing procedure for :var resolution)
 * paramRanges     — { "SIDE" -> range of ":side" on the TO line }
 */
public class ProcedureSymbol {

    private final String name;
    private final Range nameRange;
    private final Range definitionRange;
    private final List<String> parameters;
    private final Map<String, Range> paramRanges;

    public ProcedureSymbol(String name,
                           Range nameRange,
                           Range definitionRange,
                           List<String> parameters,
                           Map<String, Range> paramRanges) {
        this.name = name;
        this.nameRange = nameRange;
        this.definitionRange = definitionRange;
        this.parameters = parameters;
        this.paramRanges = paramRanges;
    }

    public String getName()               { return name; }
    public Range getNameRange()           { return nameRange; }
    public Range getDefinitionRange()     { return definitionRange; }
    public List<String> getParameters()   { return parameters; }
    public Map<String, Range> getParamRanges() { return paramRanges; }
}
