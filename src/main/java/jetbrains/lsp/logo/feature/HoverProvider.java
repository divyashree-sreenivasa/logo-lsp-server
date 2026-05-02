package jetbrains.lsp.logo.feature;

import jetbrains.lsp.logo.analysis.ProcedureSymbol;
import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.parser.LogoLexer;
import jetbrains.lsp.logo.parser.PositionConverter;
import jetbrains.lsp.logo.store.DocumentState;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HoverProvider {

    // Built-in documentation

    private static final Map<Integer, String> BUILTIN_DOCS = Map.ofEntries(
            // Turtle motion
            Map.entry(LogoLexer.FORWARD,
                    "**FORWARD** (FD) *distance*\n\nMoves the turtle forward by *distance* steps."),
            Map.entry(LogoLexer.BACK,
                    "**BACK** (BK) *distance*\n\nMoves the turtle backward by *distance* steps."),
            Map.entry(LogoLexer.LEFT,
                    "**LEFT** (LT) *angle*\n\nTurns the turtle left by *angle* degrees."),
            Map.entry(LogoLexer.RIGHT,
                    "**RIGHT** (RT) *angle*\n\nTurns the turtle right by *angle* degrees."),
            Map.entry(LogoLexer.HOME,
                    "**HOME**\n\nMoves the turtle to the centre of the screen, facing up."),
            Map.entry(LogoLexer.SETX,
                    "**SETX** *x*\n\nMoves the turtle horizontally to x-coordinate *x*."),
            Map.entry(LogoLexer.SETY,
                    "**SETY** *y*\n\nMoves the turtle vertically to y-coordinate *y*."),
            Map.entry(LogoLexer.SETXY,
                    "**SETXY** *x* *y*\n\nMoves the turtle to position (*x*, *y*)."),
            Map.entry(LogoLexer.SETHEADING,
                    "**SETHEADING** (SETH) *angle*\n\nSets the turtle's heading to *angle* degrees (0 = up)."),
            Map.entry(LogoLexer.ARC,
                    "**ARC** *angle* *radius*\n\nDraws an arc of *angle* degrees with the given *radius*."),
            // Pen
            Map.entry(LogoLexer.PENUP,
                    "**PENUP** (PU)\n\nLifts the pen; turtle movement will not draw."),
            Map.entry(LogoLexer.PENDOWN,
                    "**PENDOWN** (PD)\n\nLowers the pen; turtle movement will draw."),
            Map.entry(LogoLexer.SETPC,
                    "**SETPC** *colour*\n\nSets the pen colour by number."),
            Map.entry(LogoLexer.SETPENCOLOR,
                    "**SETPENCOLOR** *colour*\n\nSets the pen colour."),
            Map.entry(LogoLexer.SETPENSIZE,
                    "**SETPENSIZE** *width*\n\nSets the pen width in pixels."),
            Map.entry(LogoLexer.CLEARSCREEN,
                    "**CLEARSCREEN** (CS)\n\nClears the drawing and sends the turtle home."),
            Map.entry(LogoLexer.HIDETURTLE,
                    "**HIDETURTLE** (HT)\n\nMakes the turtle invisible."),
            Map.entry(LogoLexer.SHOWTURTLE,
                    "**SHOWTURTLE** (ST)\n\nMakes the turtle visible."),
            // Control flow
            Map.entry(LogoLexer.REPEAT,
                    "**REPEAT** *count* `[` *statements* `]`\n\nRuns *statements* *count* times."),
            Map.entry(LogoLexer.WHILE,
                    "**WHILE** `[` *condition* `]` `[` *statements* `]`\n\nRuns *statements* repeatedly while *condition* is true."),
            Map.entry(LogoLexer.UNTIL,
                    "**UNTIL** `[` *condition* `]` `[` *statements* `]`\n\nRuns *statements* repeatedly until *condition* becomes true."),
            Map.entry(LogoLexer.FOR,
                    "**FOR** `[` *var* *start* *end* `]` `[` *statements* `]`\n\nRuns *statements* with *var* stepping from *start* to *end*."),
            Map.entry(LogoLexer.FOREVER,
                    "**FOREVER** `[` *statements* `]`\n\nRuns *statements* in an infinite loop."),
            Map.entry(LogoLexer.IF,
                    "**IF** *condition* `[` *statements* `]`\n\nRuns *statements* if *condition* is true."),
            Map.entry(LogoLexer.IFELSE,
                    "**IFELSE** *condition* `[` *then* `]` `[` *else* `]`\n\nRuns *then* if *condition* is true, otherwise runs *else*."),
            // Variables
            Map.entry(LogoLexer.MAKE,
                    "**MAKE** `\"name` *value*\n\nAssigns *value* to the variable *name*."),
            Map.entry(LogoLexer.LOCALMAKE,
                    "**LOCALMAKE** `\"name` *value*\n\nCreates a local variable *name* with *value*."),
            Map.entry(LogoLexer.LOCAL,
                    "**LOCAL** `\"name`\n\nDeclares a local variable *name*."),
            Map.entry(LogoLexer.OUTPUT,
                    "**OUTPUT** (OP) *value*\n\nReturns *value* from the current procedure."),
            Map.entry(LogoLexer.STOP,
                    "**STOP**\n\nExits the current procedure immediately."),
            // I/O
            Map.entry(LogoLexer.PRINT,
                    "**PRINT** (PR) *value*\n\nPrints *value* followed by a newline."),
            Map.entry(LogoLexer.SHOW,
                    "**SHOW** *value*\n\nPrints *value* with brackets for lists."),
            // Math
            Map.entry(LogoLexer.SIN,
                    "**SIN** *angle*\n\nReturns the sine of *angle* (in degrees)."),
            Map.entry(LogoLexer.COS,
                    "**COS** *angle*\n\nReturns the cosine of *angle* (in degrees)."),
            Map.entry(LogoLexer.SQRT,
                    "**SQRT** *n*\n\nReturns the square root of *n*."),
            Map.entry(LogoLexer.ABS,
                    "**ABS** *n*\n\nReturns the absolute value of *n*."),
            Map.entry(LogoLexer.INT,
                    "**INT** *n*\n\nTruncates *n* to an integer."),
            Map.entry(LogoLexer.ROUND,
                    "**ROUND** *n*\n\nRounds *n* to the nearest integer."),
            Map.entry(LogoLexer.RANDOM,
                    "**RANDOM** *n*\n\nReturns a random integer between 0 and *n* − 1."),
            // List
            Map.entry(LogoLexer.FIRST,
                    "**FIRST** *list*\n\nReturns the first element of *list*."),
            Map.entry(LogoLexer.LAST,
                    "**LAST** *list*\n\nReturns the last element of *list*."),
            Map.entry(LogoLexer.COUNT,
                    "**COUNT** *list*\n\nReturns the number of elements in *list*."),
            Map.entry(LogoLexer.SENTENCE,
                    "**SENTENCE** (SE) *a* *b*\n\nCombines *a* and *b* into a list."),
            // Boolean
            Map.entry(LogoLexer.AND,
                    "**AND** *a* *b*\n\nReturns true if both *a* and *b* are true."),
            Map.entry(LogoLexer.OR,
                    "**OR** *a* *b*\n\nReturns true if either *a* or *b* is true."),
            Map.entry(LogoLexer.NOT,
                    "**NOT** *a*\n\nReturns the logical negation of *a*.")
    );

    // Public API

    public Optional<Hover> hover(Position cursor, DocumentState state) {
        List<Token> tokens = state.getParseResult().getAllTokens();
        SymbolTable symbolTable = state.getSymbolTable();

        Token hit = tokenAt(tokens, cursor);
        if (hit == null) return Optional.empty();

        // Built-in keyword or function
        String builtinDoc = BUILTIN_DOCS.get(hit.getType());
        if (builtinDoc != null) {
            return Optional.of(makeHover(builtinDoc, PositionConverter.toRange(hit)));
        }

        // User-defined procedure (IDENT that resolves in the symbol table)
        if (hit.getType() == LogoLexer.IDENT) {
            ProcedureSymbol proc = symbolTable.getProcedure(hit.getText());
            if (proc != null) {
                return Optional.of(makeHover(procedureDoc(proc), PositionConverter.toRange(hit)));
            }
        }

        // :variable reference - show which declaration it resolves to
        if (hit.getType() == LogoLexer.COLON) {
            int idx = indexOf(tokens, hit);
            if (idx + 1 < tokens.size()
                    && tokens.get(idx + 1).getType() == LogoLexer.IDENT) {
                Token nameToken = tokens.get(idx + 1);
                Range range = PositionConverter.toRange(hit, nameToken);
                return Optional.of(makeHover(
                        "**:" + nameToken.getText().toLowerCase() + "**\n\nVariable reference",
                        range));
            }
        }

        return Optional.empty();
    }

    // Helpers

    private String procedureDoc(ProcedureSymbol proc) {
        StringBuilder sb = new StringBuilder("**")
                .append(proc.getName().toLowerCase())
                .append("**");
        for (String param : proc.getParameters()) {
            sb.append(" `:").append(param.toLowerCase()).append("`");
        }
        sb.append("\n\nUser-defined procedure");
        if (!proc.getParameters().isEmpty()) {
            sb.append(" — ").append(proc.getParameters().size()).append(" parameter(s)");
        }
        return sb.toString();
    }

    private Hover makeHover(String markdown, Range range) {
        MarkupContent content = new MarkupContent(MarkupKind.MARKDOWN, markdown);
        Hover hover = new Hover(content);
        hover.setRange(range);
        return hover;
    }

    private Token tokenAt(List<Token> tokens, Position cursor) {
        int line = cursor.getLine();
        int ch   = cursor.getCharacter();
        for (Token t : tokens) {
            if (t.getType() == Token.EOF) continue;
            int tokenLine  = t.getLine() - 1;
            int tokenStart = t.getCharPositionInLine();
            int tokenEnd   = tokenStart + t.getText().length();
            if (tokenLine == line && tokenStart <= ch && ch < tokenEnd) return t;
        }
        return null;
    }

    private int indexOf(List<Token> tokens, Token target) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) == target) return i;
        }
        return -1;
    }
}
