package jetbrains.lsp.logo.feature;

import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.analysis.SymbolTableBuilder;
import jetbrains.lsp.logo.parser.LogoParserFacade;
import jetbrains.lsp.logo.parser.LogoParseResult;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticsProviderTest {

    private List<Diagnostic> diagnose(String source) {
        LogoParseResult result = LogoParserFacade.parse(source);
        SymbolTable table = new SymbolTableBuilder().build(result.getTree());
        return new DiagnosticsProvider().check(result.getTree(), table, result.getSyntaxErrors());
    }

    private boolean hasError(List<Diagnostic> diags, String fragment) {
        return diags.stream()
                .filter(d -> d.getSeverity() == DiagnosticSeverity.Error)
                .anyMatch(d -> d.getMessage().contains(fragment));
    }

    private boolean hasWarning(List<Diagnostic> diags, String fragment) {
        return diags.stream()
                .filter(d -> d.getSeverity() == DiagnosticSeverity.Warning)
                .anyMatch(d -> d.getMessage().contains(fragment));
    }

    // ── Valid code ────────────────────────────────────────────────────────────

    @Test
    void noErrorsForValidProcedureCall() {
        List<Diagnostic> diags = diagnose("""
                TO square :side
                  REPEAT 4 [FORWARD :side RIGHT 90]
                END
                square 100
                """);
        assertFalse(hasError(diags, "square"), "Valid call should not produce an error");
    }

    @Test
    void noErrorsForBuiltinCommands() {
        List<Diagnostic> diags = diagnose("FORWARD 50\nBACK 30\nLEFT 90\n");
        assertTrue(diags.isEmpty(), "Built-in commands should not produce diagnostics");
    }

    @Test
    void noErrorsForMakeVariable() {
        List<Diagnostic> diags = diagnose("""
                MAKE "x 10
                FORWARD :x
                """);
        assertFalse(hasWarning(diags, ":x"), "Variable defined via MAKE should not warn");
    }

    @Test
    void noErrorsForProcedureParameter() {
        List<Diagnostic> diags = diagnose("""
                TO move :dist
                  FORWARD :dist
                END
                """);
        assertFalse(hasWarning(diags, ":dist"));
    }

    @Test
    void noErrorsForForLoopVariable() {
        List<Diagnostic> diags = diagnose("""
                TO loop :n
                  FOR [i 1 :n] [PRINT :i]
                END
                """);
        assertFalse(hasWarning(diags, ":i"), ":i inside FOR body should not warn");
    }

    // ── Wrong arity ───────────────────────────────────────────────────────────

    @Test
    void errorForTooFewArguments() {
        List<Diagnostic> diags = diagnose("""
                TO square :side
                  REPEAT 4 [FORWARD :side RIGHT 90]
                END
                square
                """);
        assertTrue(hasError(diags, "expects 1"), "Too few args should be an error");
    }

    @Test
    void errorForTooManyArguments() {
        List<Diagnostic> diags = diagnose("""
                TO square :side
                  REPEAT 4 [FORWARD :side RIGHT 90]
                END
                square 50 100
                """);
        assertTrue(hasError(diags, "expects 1"), "Too many args should be an error");
    }

    @Test
    void errorForWrongArityMultiParam() {
        List<Diagnostic> diags = diagnose("""
                TO polygon :sides :size
                  REPEAT :sides [FORWARD :size RIGHT 360 / :sides]
                END
                polygon 6
                """);
        assertTrue(hasError(diags, "expects 2"), "Missing second arg should be an error");
    }

    @Test
    void noErrorForCorrectArity() {
        List<Diagnostic> diags = diagnose("""
                TO polygon :sides :size
                  REPEAT :sides [FORWARD :size RIGHT 360 / :sides]
                END
                polygon 6 50
                """);
        assertFalse(hasError(diags, "polygon"));
    }

    // ── Unknown procedures ────────────────────────────────────────────────────

    @Test
    void warningForUnknownProcedure() {
        List<Diagnostic> diags = diagnose("unknownproc 1 2\n");
        assertTrue(hasWarning(diags, "Unknown procedure"), "Unknown procedure should warn");
    }

    @Test
    void noWarningForCallToDefinedProcedure() {
        List<Diagnostic> diags = diagnose("""
                TO greet
                  PRINT "hello
                END
                greet
                """);
        assertFalse(hasWarning(diags, "GREET"));
    }

    // ── Undefined variables ───────────────────────────────────────────────────

    @Test
    void warningForUndefinedVariable() {
        List<Diagnostic> diags = diagnose("FORWARD :x\n");
        assertTrue(hasWarning(diags, ":x"), "Undeclared variable should warn");
    }

    @Test
    void warningOnlyForUndefinedNotParam() {
        List<Diagnostic> diags = diagnose("""
                TO move :dist
                  FORWARD :dist
                  FORWARD :other
                END
                """);
        assertFalse(hasWarning(diags, ":dist"), ":dist is a param — should not warn");
        assertTrue(hasWarning(diags, ":other"), ":other is undeclared — should warn");
    }

    // ── Syntax errors ─────────────────────────────────────────────────────────

    @Test
    void syntaxErrorForMissingEnd() {
        List<Diagnostic> diags = diagnose("""
                TO broken
                  FORWARD 50
                """);
        assertTrue(hasError(diags, ""), "Missing END should produce syntax error");
        assertTrue(diags.stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.Error));
    }
}
