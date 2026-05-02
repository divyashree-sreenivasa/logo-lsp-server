package jetbrains.lsp.logo.feature;

import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.analysis.SymbolTableBuilder;
import jetbrains.lsp.logo.parser.LogoParserFacade;
import jetbrains.lsp.logo.parser.LogoParseResult;
import jetbrains.lsp.logo.store.DocumentState;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GotoDeclarationProviderTest {

    private static final String URI = "file:///test.logo";

    private DocumentState stateFor(String source) {
        LogoParseResult result = LogoParserFacade.parse(source);
        SymbolTable table = new SymbolTableBuilder().build(result.getTree());
        return new DocumentState(result, table);
    }

    private Optional<Location> resolve(String source, int line, int character) {
        DocumentState state = stateFor(source);
        Position cursor = new Position(line, character);
        return new GotoDeclarationProvider().resolve(URI, cursor, state);
    }

    // ── Procedure call → TO declaration ──────────────────────────────────────

    @Test
    void resolvesProcedureCallToDeclaration() {
        // Line 0: TO square :side
        // Line 3: square 100   ← cursor here on "square"
        String source = "TO square :side\n  REPEAT 4 [FORWARD :side RIGHT 90]\nEND\nsquare 100\n";
        Optional<Location> loc = resolve(source, 3, 2); // col 2 = inside "square"
        assertTrue(loc.isPresent(), "Should resolve procedure call");
        assertEquals(URI, loc.get().getUri());
        // Should point to line 0, col 3 (the "square" in "TO square")
        assertEquals(0, loc.get().getRange().getStart().getLine());
        assertEquals(3, loc.get().getRange().getStart().getCharacter());
    }

    @Test
    void noResultOnDeclarationSiteItself() {
        // Clicking the name in "TO square" should NOT navigate anywhere
        String source = "TO square :side\n  FORWARD :side\nEND\n";
        Optional<Location> loc = resolve(source, 0, 3); // on "square" after TO
        assertTrue(loc.isEmpty(), "Declaration site itself should not navigate");
    }

    @Test
    void noResultForUnknownProcedure() {
        String source = "unknown 50\n";
        Optional<Location> loc = resolve(source, 0, 0);
        assertTrue(loc.isEmpty());
    }

    // ── :param → TO parameter ─────────────────────────────────────────────────

    @Test
    void resolvesColonParamToToLine() {
        // Line 0: TO square :side
        // Line 1:   REPEAT 4 [FORWARD :side ...]
        // Cursor on ":side" at line 1
        String source = "TO square :side\n  REPEAT 4 [FORWARD :side RIGHT 90]\nEND\n";
        // ":side" in body is at col 20 (after "  REPEAT 4 [FORWARD ")
        Optional<Location> loc = resolve(source, 1, 20);
        assertTrue(loc.isPresent(), "Should resolve :param to TO line");
        assertEquals(0, loc.get().getRange().getStart().getLine(), "Should land on TO line");
    }

    @Test
    void resolvesIdentPartOfColonVariable() {
        // Click on "side" (the IDENT part of :side), not the colon
        String source = "TO square :side\n  FORWARD :side\nEND\n";
        Optional<Location> loc = resolve(source, 1, 11); // col 11 = "side" in ":side"
        assertTrue(loc.isPresent());
        assertEquals(0, loc.get().getRange().getStart().getLine());
    }

    // ── :var → MAKE declaration ───────────────────────────────────────────────

    @Test
    void resolvesColonVarToMakeDeclaration() {
        // Line 0: MAKE "size 50
        // Line 1: FORWARD :size
        String source = "MAKE \"size 50\nFORWARD :size\n";
        Optional<Location> loc = resolve(source, 1, 8); // col 8 = inside ":size"
        assertTrue(loc.isPresent(), "Should resolve :var to MAKE line");
        assertEquals(0, loc.get().getRange().getStart().getLine(), "Should land on MAKE line");
    }

    // ── :i → FOR loop variable ────────────────────────────────────────────────

    @Test
    void resolvesColonVarToForLoopVariable() {
        String source = """
                TO printnumbers :n
                  FOR [i 1 :n] [
                    PRINT :i
                  ]
                END
                """;
        // ":i" is on line 2, col 10 (after "    PRINT ")
        Optional<Location> loc = resolve(source, 2, 10);
        assertTrue(loc.isPresent(), "Should resolve :i to FOR loop variable");
        // FOR [i ...] is on line 1, so the declaration is there
        assertEquals(1, loc.get().getRange().getStart().getLine(), "Should point to FOR line");
    }

    @Test
    void procParamTakesPriorityOverMakeVariable() {
        // Both :side as a param and MAKE "side exist
        String source = "MAKE \"side 99\nTO square :side\n  FORWARD :side\nEND\n";
        // :side on line 2 (inside procedure body) should point to TO line, not MAKE
        Optional<Location> loc = resolve(source, 2, 10);
        assertTrue(loc.isPresent());
        assertEquals(1, loc.get().getRange().getStart().getLine(),
                "Param should shadow MAKE variable");
    }
}
