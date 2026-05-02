package jetbrains.lsp.logo.feature;

import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.analysis.SymbolTableBuilder;
import jetbrains.lsp.logo.parser.LogoParserFacade;
import jetbrains.lsp.logo.parser.LogoParseResult;
import jetbrains.lsp.logo.store.DocumentState;
import org.eclipse.lsp4j.SemanticTokens;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticTokensProviderTest {

    // Token type indices (must match the legend in LogoLanguageServer)
    private static final int TYPE_KEYWORD   = 0;
    private static final int TYPE_FUNCTION  = 1;
    private static final int TYPE_PARAMETER = 2;
    private static final int TYPE_VARIABLE  = 3;
    private static final int TYPE_NUMBER    = 4;
    private static final int TYPE_STRING    = 5;
    private static final int TYPE_COMMENT   = 6;
    private static final int TYPE_OPERATOR  = 7;

    // Modifier bitmasks
    private static final int MOD_DECLARATION = 1;
    private static final int MOD_DEFINITION  = 2;
    private static final int MOD_READONLY    = 4;

    /** Decoded semantic token — easier to assert on than a flat int array. */
    record Token(int line, int character, int length, int type, int modifiers) {}

    private List<Token> tokensFor(String source) {
        LogoParseResult result = LogoParserFacade.parse(source);
        SymbolTable table = new SymbolTableBuilder().build(result.getTree());
        DocumentState state = new DocumentState(result, table);

        SemanticTokens raw = new SemanticTokensProvider().provide(state);
        List<Integer> data = raw.getData();

        List<Token> tokens = new ArrayList<>();
        int prevLine = 0, prevChar = 0;
        for (int i = 0; i < data.size(); i += 5) {
            int deltaLine = data.get(i);
            int deltaChar = data.get(i + 1);
            int len       = data.get(i + 2);
            int type      = data.get(i + 3);
            int mods      = data.get(i + 4);

            int line = prevLine + deltaLine;
            int ch   = (deltaLine == 0) ? prevChar + deltaChar : deltaChar;
            tokens.add(new Token(line, ch, len, type, mods));
            prevLine = line;
            prevChar = ch;
        }
        return tokens;
    }

    private Token find(List<Token> tokens, int line, int type) {
        return tokens.stream()
                .filter(t -> t.line() == line && t.type() == type)
                .findFirst()
                .orElse(null);
    }

    // ── Keywords ──────────────────────────────────────────────────────────────

    @Test
    void toKeywordIsHighlighted() {
        List<Token> tokens = tokensFor("TO square :side\n  FORWARD :side\nEND\n");
        Token to = tokens.stream()
                .filter(t -> t.line() == 0 && t.character() == 0 && t.type() == TYPE_KEYWORD)
                .findFirst().orElse(null);
        assertNotNull(to, "TO should be a keyword");
        assertEquals(2, to.length());
    }

    @Test
    void whileKeywordIsHighlighted() {
        List<Token> tokens = tokensFor("WHILE [:x < 10] [FORWARD :x]\n");
        Token whileToken = tokens.stream()
                .filter(t -> t.type() == TYPE_KEYWORD && t.character() == 0)
                .findFirst().orElse(null);
        assertNotNull(whileToken, "WHILE should be highlighted as keyword");
    }

    @Test
    void forKeywordIsHighlighted() {
        List<Token> tokens = tokensFor("FOR [i 1 10] [FORWARD 10]\n");
        assertFalse(tokens.isEmpty());
        Token forToken = tokens.stream()
                .filter(t -> t.type() == TYPE_KEYWORD && t.line() == 0 && t.character() == 0)
                .findFirst().orElse(null);
        assertNotNull(forToken, "FOR should be highlighted as keyword");
    }

    @Test
    void lowercaseWhileIsHighlighted() {
        // Case-insensitive: lowercase keywords should also get highlighted
        List<Token> tokens = tokensFor("while [:x < 10] [fd :x]\n");
        Token whileToken = tokens.stream()
                .filter(t -> t.type() == TYPE_KEYWORD)
                .findFirst().orElse(null);
        assertNotNull(whileToken, "lowercase 'while' should still be highlighted as keyword");
    }

    // ── Procedure names ───────────────────────────────────────────────────────

    @Test
    void procedureDeclarationIsFunctionWithDeclarationModifier() {
        // "square" in "TO square :side" should be TYPE_FUNCTION with declaration|definition
        List<Token> tokens = tokensFor("TO square :side\n  FORWARD :side\nEND\n");
        Token name = tokens.stream()
                .filter(t -> t.line() == 0 && t.type() == TYPE_FUNCTION)
                .findFirst().orElse(null);
        assertNotNull(name, "Procedure name in TO line should be TYPE_FUNCTION");
        assertTrue((name.modifiers() & MOD_DECLARATION) != 0, "Should have declaration modifier");
        assertTrue((name.modifiers() & MOD_DEFINITION)  != 0, "Should have definition modifier");
    }

    @Test
    void procedureCallSiteIsFunctionWithDefinitionModifier() {
        // "square" in "square 100" should be TYPE_FUNCTION with definition modifier
        List<Token> tokens = tokensFor("TO square :side\n  FORWARD :side\nEND\nsquare 100\n");
        Token call = tokens.stream()
                .filter(t -> t.line() == 3 && t.type() == TYPE_FUNCTION)
                .findFirst().orElse(null);
        assertNotNull(call, "Procedure call site should be TYPE_FUNCTION");
        assertTrue((call.modifiers() & MOD_DEFINITION) != 0, "Call site should have definition modifier");
        assertEquals(0, call.modifiers() & MOD_DECLARATION, "Call site should NOT have declaration modifier");
    }

    // ── Variables and parameters ──────────────────────────────────────────────

    @Test
    void parameterInToLineIsTypeParameter() {
        // ":side" in "TO square :side" should be TYPE_PARAMETER
        List<Token> tokens = tokensFor("TO square :side\n  FORWARD :side\nEND\n");
        Token param = tokens.stream()
                .filter(t -> t.line() == 0 && t.type() == TYPE_PARAMETER)
                .findFirst().orElse(null);
        assertNotNull(param, ":side in TO line should be TYPE_PARAMETER");
        assertEquals(5, param.length(), "':side' has length 5");
    }

    @Test
    void variableReferenceInBodyIsTypeVariable() {
        // ":side" in body should be TYPE_VARIABLE
        List<Token> tokens = tokensFor("TO square :side\n  FORWARD :side\nEND\n");
        Token variable = tokens.stream()
                .filter(t -> t.line() == 1 && t.type() == TYPE_VARIABLE)
                .findFirst().orElse(null);
        assertNotNull(variable, ":side in body should be TYPE_VARIABLE");
    }

    @Test
    void forLoopVariableIsTypeParameter() {
        // "i" in "FOR [i 1 10]" should be TYPE_PARAMETER (loop variable declaration)
        List<Token> tokens = tokensFor("FOR [i 1 10] [FORWARD 10]\n");
        Token loopVar = tokens.stream()
                .filter(t -> t.type() == TYPE_PARAMETER)
                .findFirst().orElse(null);
        assertNotNull(loopVar, "FOR loop variable should be TYPE_PARAMETER");
        assertTrue((loopVar.modifiers() & MOD_DECLARATION) != 0, "Loop var should have declaration modifier");
    }

    // ── Numbers ───────────────────────────────────────────────────────────────

    @Test
    void numberIsTypeNumberWithReadonlyModifier() {
        List<Token> tokens = tokensFor("FORWARD 50\n");
        Token num = tokens.stream()
                .filter(t -> t.type() == TYPE_NUMBER)
                .findFirst().orElse(null);
        assertNotNull(num, "50 should be TYPE_NUMBER");
        assertTrue((num.modifiers() & MOD_READONLY) != 0, "Numbers should have readonly modifier");
    }

    // ── Strings ───────────────────────────────────────────────────────────────

    @Test
    void quotedStringIsTypeString() {
        List<Token> tokens = tokensFor("MAKE \"hello 0\n");
        Token str = tokens.stream()
                .filter(t -> t.type() == TYPE_STRING)
                .findFirst().orElse(null);
        assertNotNull(str, "\"hello should be TYPE_STRING");
    }

    // ── Comments ─────────────────────────────────────────────────────────────

    @Test
    void commentIsTypeComment() {
        List<Token> tokens = tokensFor("; this is a comment\nFORWARD 50\n");
        Token comment = tokens.stream()
                .filter(t -> t.type() == TYPE_COMMENT)
                .findFirst().orElse(null);
        assertNotNull(comment, "Comment should be TYPE_COMMENT");
        assertEquals(0, comment.line(), "Comment should be on line 0");
    }

    // ── Operators ─────────────────────────────────────────────────────────────

    @Test
    void operatorsAreTypeOperator() {
        List<Token> tokens = tokensFor("FORWARD 10 + 20\n");
        Token op = tokens.stream()
                .filter(t -> t.type() == TYPE_OPERATOR)
                .findFirst().orElse(null);
        assertNotNull(op, "+ should be TYPE_OPERATOR");
    }

    // ── Delta encoding ────────────────────────────────────────────────────────

    @Test
    void tokenStreamIsNonEmpty() {
        List<Token> tokens = tokensFor("TO sq :s\n  FORWARD :s\nEND\n");
        assertFalse(tokens.isEmpty());
    }

    @Test
    void tokensAreInDocumentOrder() {
        List<Token> tokens = tokensFor("TO sq :s\n  FORWARD :s\nEND\n");
        for (int i = 1; i < tokens.size(); i++) {
            Token prev = tokens.get(i - 1);
            Token curr = tokens.get(i);
            boolean ordered = curr.line() > prev.line()
                    || (curr.line() == prev.line() && curr.character() >= prev.character());
            assertTrue(ordered,
                    "Token at index " + i + " is not in document order: "
                    + prev + " -> " + curr);
        }
    }
}
