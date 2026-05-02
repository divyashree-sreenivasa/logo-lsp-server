package jetbrains.lsp.logo.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogoParserFacadeTest {

    @Test
    void parsesEmptyProgram() {
        LogoParseResult result = LogoParserFacade.parse("");
        assertTrue(result.getSyntaxErrors().isEmpty());
        assertNotNull(result.getTree());
    }

    @Test
    void parsesProcedureDefinition() {
        LogoParseResult result = LogoParserFacade.parse("""
                TO square :side
                  REPEAT 4 [FORWARD :side RIGHT 90]
                END
                """);
        assertTrue(result.getSyntaxErrors().isEmpty());
    }

    @Test
    void parsesWhileLoop() {
        LogoParseResult result = LogoParserFacade.parse("""
                TO countdown :n
                  WHILE [:n > 0] [
                    PRINT :n
                    MAKE "n :n - 1
                  ]
                END
                """);
        assertTrue(result.getSyntaxErrors().isEmpty());
    }

    @Test
    void parsesForLoop() {
        LogoParseResult result = LogoParserFacade.parse("""
                TO printnumbers :n
                  FOR [i 1 :n] [
                    PRINT :i
                  ]
                END
                """);
        assertTrue(result.getSyntaxErrors().isEmpty());
    }

    @Test
    void parsesIfElse() {
        LogoParseResult result = LogoParserFacade.parse("""
                TO absval :x
                  IFELSE :x < 0 [OUTPUT 0 - :x] [OUTPUT :x]
                END
                """);
        assertTrue(result.getSyntaxErrors().isEmpty());
    }

    @Test
    void parsesArithmeticExpression() {
        LogoParseResult result = LogoParserFacade.parse("FORWARD (10 + 20) * 3\n");
        assertTrue(result.getSyntaxErrors().isEmpty());
    }

    @Test
    void parsesComment() {
        LogoParseResult result = LogoParserFacade.parse("""
                ; this is a comment
                FORWARD 50
                """);
        assertTrue(result.getSyntaxErrors().isEmpty());
    }

    @Test
    void reportsSyntaxErrorForMissingEnd() {
        LogoParseResult result = LogoParserFacade.parse("""
                TO broken
                  FORWARD 50
                """);
        // Missing END — ANTLR should report at least one error
        assertFalse(result.getSyntaxErrors().isEmpty());
    }

    @Test
    void isCaseInsensitive() {
        // lowercase keywords should parse without errors
        LogoParseResult lower = LogoParserFacade.parse("forward 50\nback 30\n");
        assertTrue(lower.getSyntaxErrors().isEmpty());

        LogoParseResult mixed = LogoParserFacade.parse("Forward 50\nBack 30\n");
        assertTrue(mixed.getSyntaxErrors().isEmpty());
    }

    @Test
    void tokenStreamContainsAllTokens() {
        LogoParseResult result = LogoParserFacade.parse("; comment\nFORWARD 50\n");
        // Should include the hidden-channel COMMENT token
        boolean hasComment = result.getAllTokens().stream()
                .anyMatch(t -> t.getType() == LogoLexer.COMMENT);
        assertTrue(hasComment, "Token stream should include hidden-channel COMMENT tokens");
    }
}
