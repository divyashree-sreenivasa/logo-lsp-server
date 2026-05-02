package jetbrains.lsp.logo.analysis;

import jetbrains.lsp.logo.parser.LogoParserFacade;
import jetbrains.lsp.logo.parser.LogoParseResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SymbolTableBuilderTest {

    private SymbolTable buildFrom(String source) {
        LogoParseResult result = LogoParserFacade.parse(source);
        return new SymbolTableBuilder().build(result.getTree());
    }

    // ── Procedures ────────────────────────────────────────────────────────────

    @Test
    void collectsProcedureWithNoParams() {
        SymbolTable table = buildFrom("""
                TO home.turtle
                  HOME
                END
                """);
        assertTrue(table.hasProcedure("HOME.TURTLE"));
        assertEquals(0, table.getProcedure("HOME.TURTLE").getParameters().size());
    }

    @Test
    void collectsProcedureWithParams() {
        SymbolTable table = buildFrom("""
                TO square :side
                  REPEAT 4 [FORWARD :side RIGHT 90]
                END
                """);
        ProcedureSymbol sq = table.getProcedure("SQUARE");
        assertNotNull(sq);
        assertEquals(1, sq.getParameters().size());
        assertEquals("SIDE", sq.getParameters().get(0));
    }

    @Test
    void collectsProcedureWithMultipleParams() {
        SymbolTable table = buildFrom("""
                TO polygon :sides :size
                  REPEAT :sides [FORWARD :size RIGHT 360 / :sides]
                END
                """);
        ProcedureSymbol p = table.getProcedure("POLYGON");
        assertNotNull(p);
        assertEquals(2, p.getParameters().size());
        assertTrue(p.getParameters().contains("SIDES"));
        assertTrue(p.getParameters().contains("SIZE"));
    }

    @Test
    void collectsMultipleProcedures() {
        SymbolTable table = buildFrom("""
                TO square :side
                  REPEAT 4 [FORWARD :side RIGHT 90]
                END
                TO triangle :side
                  REPEAT 3 [FORWARD :side RIGHT 120]
                END
                """);
        assertTrue(table.hasProcedure("SQUARE"));
        assertTrue(table.hasProcedure("TRIANGLE"));
    }

    @Test
    void procedureNameRangePointsToIdentToken() {
        SymbolTable table = buildFrom("TO square :side\n  FORWARD :side\nEND\n");
        ProcedureSymbol sq = table.getProcedure("SQUARE");
        // "square" starts at line 0, column 3 (after "TO ")
        assertEquals(0, sq.getNameRange().getStart().getLine());
        assertEquals(3, sq.getNameRange().getStart().getCharacter());
    }

    @Test
    void definitionRangeSpansWholeBlock() {
        SymbolTable table = buildFrom("TO sq :s\n  FORWARD :s\nEND\n");
        ProcedureSymbol sq = table.getProcedure("SQ");
        // Starts on line 0 (TO), ends on line 2 (END)
        assertEquals(0, sq.getDefinitionRange().getStart().getLine());
        assertEquals(2, sq.getDefinitionRange().getEnd().getLine());
    }

    @Test
    void paramRangeCoversBothColonAndName() {
        SymbolTable table = buildFrom("TO sq :side\n  FORWARD :side\nEND\n");
        ProcedureSymbol sq = table.getProcedure("SQ");
        // ":side" starts at col 6 on line 0 (after "TO sq ")
        assertEquals(0, sq.getParamRanges().get("SIDE").getStart().getLine());
        assertEquals(6, sq.getParamRanges().get("SIDE").getStart().getCharacter());
    }

    // ── MAKE variables ────────────────────────────────────────────────────────

    @Test
    void collectsMakeVariable() {
        SymbolTable table = buildFrom("MAKE \"count 0\n");
        Optional<SymbolTable.MakeVariable> mv = table.getMakeVariable("COUNT");
        assertTrue(mv.isPresent());
    }

    @Test
    void makeVariableRangePointsToQuotedString() {
        SymbolTable table = buildFrom("MAKE \"size 50\n");
        SymbolTable.MakeVariable mv = table.getMakeVariable("SIZE").orElseThrow();
        // "size starts at col 5 (after "MAKE ")
        assertEquals(0, mv.nameRange().getStart().getLine());
        assertEquals(5, mv.nameRange().getStart().getCharacter());
    }

    @Test
    void collectsMakeInsideProcedure() {
        SymbolTable table = buildFrom("""
                TO spiral :step
                  MAKE "size :step
                  FORWARD :size
                END
                """);
        assertTrue(table.getMakeVariable("SIZE").isPresent());
    }

    // ── FOR loop variables ────────────────────────────────────────────────────

    @Test
    void collectsForLoopVariable() {
        SymbolTable table = buildFrom("""
                TO printnumbers :n
                  FOR [i 1 :n] [PRINT :i]
                END
                """);
        org.eclipse.lsp4j.Position insideBody = new org.eclipse.lsp4j.Position(1, 20);
        Optional<SymbolTable.ForLoopVariable> fv = table.getForLoopVariable("I", insideBody);
        assertTrue(fv.isPresent());
    }

    @Test
    void forLoopVariableNotVisibleOutsideScope() {
        SymbolTable table = buildFrom("""
                TO test
                  FOR [i 1 5] [PRINT :i]
                END
                PRINT :i
                """);
        // Position on line 3 (PRINT :i at top level) is outside the FOR scope
        org.eclipse.lsp4j.Position outside = new org.eclipse.lsp4j.Position(3, 7);
        assertTrue(table.getForLoopVariable("I", outside).isEmpty());
    }
}
