package jetbrains.lsp.logo.feature;

import jetbrains.lsp.logo.analysis.ProcedureSymbol;
import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.parser.LogoBaseVisitor;
import jetbrains.lsp.logo.parser.LogoParser;
import jetbrains.lsp.logo.parser.LogoParseResult;
import jetbrains.lsp.logo.parser.PositionConverter;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Walks the parse tree and produces LSP Diagnostics.
 *
 * Checks:
 *  1. Syntax errors from the ANTLR parser
 *  2. Calls to undefined procedures (Warnings to avoid false positives for unknown built-ins)
 *  3. Wrong argument count for known user-defined procedures (Error)
 *  4. Undefined variable references - not a param, FOR var, or MAKE (Warning)
 */
public class DiagnosticsProvider extends LogoBaseVisitor<Void> {

    private SymbolTable symbolTable;
    private List<Diagnostic> diagnostics;

    public List<Diagnostic> check(LogoParser.ProgramContext tree,
                                  SymbolTable symbolTable,
                                  List<LogoParseResult.SyntaxError> syntaxErrors) {
        this.symbolTable = symbolTable;
        this.diagnostics = new ArrayList<>();

        // Convert ANTLR syntax errors first
        for (LogoParseResult.SyntaxError err : syntaxErrors) {
            int line = err.line() - 1;
            int col  = err.charPositionInLine();
            Range range = new Range(new Position(line, col), new Position(line, col + 1));
            diagnostics.add(new Diagnostic(range, err.message(), DiagnosticSeverity.Error, "logo"));
        }

        visit(tree);
        return diagnostics;
    }

    // Procedure call sites

    /** Statement-position call: procedureCall : IDENT expr* */
    @Override
    public Void visitProcedureCall(LogoParser.ProcedureCallContext ctx) {
        checkCall(ctx.IDENT().getSymbol(), ctx.expr().size());
        return visitChildren(ctx);
    }

    /** Expression-position call with no args: IDENT */
    @Override
    public Void visitCallExprNoArg(LogoParser.CallExprNoArgContext ctx) {
        checkCall(ctx.IDENT().getSymbol(), 0);
        return visitChildren(ctx);
    }

    /** Expression-position call with parenthesised args: IDENT ( expr* ) */
    @Override
    public Void visitCallExprParen(LogoParser.CallExprParenContext ctx) {
        checkCall(ctx.IDENT().getSymbol(), ctx.expr().size());
        return visitChildren(ctx);
    }

    private void checkCall(Token nameToken, int actualArgs) {
        String name = nameToken.getText().toUpperCase();
        ProcedureSymbol proc = symbolTable.getProcedure(name);

        if (proc == null) {
            // Warning rather than Error: the name might be a built-in we don't
            // have in the grammar (e.g. setitem, item, …).
            warn(PositionConverter.toRange(nameToken), "Unknown procedure: " + name);
        } else {
            int expected = proc.getParameters().size();
            if (actualArgs != expected) {
                error(PositionConverter.toRange(nameToken),
                        "'" + name + "' expects " + expected
                                + " argument(s), got " + actualArgs);
            }
        }
    }

    // Variable references

    /** :varname reference in an expression */
    @Override
    public Void visitVarExpr(LogoParser.VarExprContext ctx) {
        Token colonToken = ctx.COLON().getSymbol();
        Token nameToken  = ctx.IDENT().getSymbol();
        String varName   = nameToken.getText().toUpperCase();
        Position pos     = PositionConverter.toPosition(colonToken);

        if (!isDefined(varName, pos)) {
            warn(PositionConverter.toRange(colonToken, nameToken),
                    "Undefined variable: :" + varName.toLowerCase());
        }
        return visitChildren(ctx);
    }

    private boolean isDefined(String varName, Position pos) {
        // 1. Procedure parameter in the enclosing TO block
        Optional<ProcedureSymbol> enclosing = symbolTable.enclosingProcedure(pos);
        if (enclosing.isPresent()
                && enclosing.get().getParamRanges().containsKey(varName)) {
            return true;
        }
        // 2. FOR loop variable in the innermost enclosing FOR
        if (symbolTable.getForLoopVariable(varName, pos).isPresent()) return true;
        // 3. MAKE declaration anywhere in the file
        if (symbolTable.getMakeVariable(varName).isPresent()) return true;
        return false;
    }

    // Helpers

    private void error(Range range, String message) {
        diagnostics.add(new Diagnostic(range, message, DiagnosticSeverity.Error, "logo"));
    }

    private void warn(Range range, String message) {
        diagnostics.add(new Diagnostic(range, message, DiagnosticSeverity.Warning, "logo"));
    }
}
