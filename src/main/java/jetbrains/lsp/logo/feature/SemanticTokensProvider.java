package jetbrains.lsp.logo.feature;

import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.parser.LogoLexer;
import jetbrains.lsp.logo.parser.LogoParseResult;
import jetbrains.lsp.logo.store.DocumentState;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.SemanticTokens;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Converts the ANTLR token stream into the LSP semantic tokens wire format.
 *
 * Token type indices match the legend declared in LogoLanguageServer.initialize():
 *   0=keyword  1=function  2=parameter  3=variable
 *   4=number   5=string    6=comment    7=operator
 *
 * Modifier bits:
 *   bit 0 = declaration   bit 1 = definition   bit 2 = readonly
 */
public class SemanticTokensProvider {

    // Token type indices (must match legend order)
    private static final int TYPE_KEYWORD   = 0;
    private static final int TYPE_FUNCTION  = 1;
    private static final int TYPE_PARAMETER = 2;
    private static final int TYPE_VARIABLE  = 3;
    private static final int TYPE_NUMBER    = 4;
    private static final int TYPE_STRING    = 5;
    private static final int TYPE_COMMENT   = 6;
    private static final int TYPE_OPERATOR  = 7;

    // Modifier bitmasks
    private static final int MOD_DECLARATION     = 1;
    private static final int MOD_DEFINITION      = 2;
    private static final int MOD_READONLY        = 4;

    // Control flow, variables, I/O
    private static final Set<Integer> KEYWORDS = Set.of(
            LogoLexer.TO, LogoLexer.END,
            LogoLexer.REPEAT, LogoLexer.WHILE, LogoLexer.UNTIL,
            LogoLexer.FOR, LogoLexer.FOREVER,
            LogoLexer.IF, LogoLexer.IFELSE,
            LogoLexer.MAKE, LogoLexer.LOCAL, LogoLexer.LOCALMAKE,
            LogoLexer.OUTPUT, LogoLexer.STOP,
            LogoLexer.PRINT, LogoLexer.SHOW
    );

    // Turtle / pen commands
    private static final Set<Integer> MACROS = Set.of(
            LogoLexer.FORWARD, LogoLexer.BACK, LogoLexer.LEFT, LogoLexer.RIGHT,
            LogoLexer.HOME, LogoLexer.SETX, LogoLexer.SETY, LogoLexer.SETXY,
            LogoLexer.SETHEADING, LogoLexer.ARC,
            LogoLexer.PENUP, LogoLexer.PENDOWN,
            LogoLexer.SETPC, LogoLexer.SETPENCOLOR, LogoLexer.SETPENSIZE,
            LogoLexer.CLEARSCREEN, LogoLexer.HIDETURTLE, LogoLexer.SHOWTURTLE
    );

    // Value-returning built-in functions
    private static final Set<Integer> BUILTIN_FUNCS = Set.of(
            LogoLexer.SIN, LogoLexer.COS, LogoLexer.SQRT,
            LogoLexer.ABS, LogoLexer.INT, LogoLexer.ROUND, LogoLexer.RANDOM,
            LogoLexer.FIRST, LogoLexer.LAST, LogoLexer.COUNT, LogoLexer.SENTENCE,
            LogoLexer.AND, LogoLexer.OR, LogoLexer.NOT
    );

    private static final Set<Integer> OPERATORS = Set.of(
            LogoLexer.PLUS, LogoLexer.MINUS, LogoLexer.STAR, LogoLexer.SLASH,
            LogoLexer.EQUAL, LogoLexer.NOTEQUAL,
            LogoLexer.LT, LogoLexer.GT, LogoLexer.LE, LogoLexer.GE
    );

    public SemanticTokens provide(DocumentState state) {
        LogoParseResult result = state.getParseResult();
        SymbolTable symbolTable = state.getSymbolTable();
        List<Token> tokens = result.getAllTokens();
        List<Integer> data = new ArrayList<>();

        int prevLine = 0;
        int prevChar = 0;

        int i = 0;
        while (i < tokens.size()) {
            Token token = tokens.get(i);
            int type = token.getType();

            // Skip whitespace, EOL, and the EOF sentinel
            if (type == LogoLexer.WS || type == LogoLexer.EOL || type == Token.EOF) {
                i++;
                continue;
            }

            // COLON is the start of a :variable reference - consume COLON + IDENT together
            if (type == LogoLexer.COLON) {
                if (i + 1 < tokens.size()) {
                    Token next = tokens.get(i + 1);
                    if (next.getType() == LogoLexer.IDENT) {
                        // Determine context: are we inside a TO parameter declaration?
                        boolean isParamDecl = isPrecedingTokenOnSameLine(tokens, i, LogoLexer.TO)
                                || isPrecedingIdentOnToLine(tokens, i);
                        int tokenType = isParamDecl ? TYPE_PARAMETER : TYPE_VARIABLE;

                        int combinedLen = 1 + next.getText().length(); // ':' + name
                        int line = token.getLine() - 1;
                        int col  = token.getCharPositionInLine();
                        encode(data, line, col, combinedLen, tokenType,
                                isParamDecl ? MOD_DECLARATION : 0,
                                prevLine, prevChar);
                        prevLine = line;
                        prevChar = col;
                        i += 2; // consume both COLON and IDENT
                        continue;
                    }
                }
                // bare colon with no following IDENT — skip
                i++;
                continue;
            }

            int tokenTypeIndex = classifyToken(token, tokens, i, symbolTable);
            int modifiers = modifiersFor(token, tokens, i);

            if (tokenTypeIndex < 0) {
                i++;
                continue;
            }

            int line = token.getLine() - 1;
            int col  = token.getCharPositionInLine();
            int len  = token.getText().length();

            encode(data, line, col, len, tokenTypeIndex, modifiers, prevLine, prevChar);
            prevLine = line;
            prevChar = col;
            i++;
        }

        return new SemanticTokens(data);
    }

    private int classifyToken(Token token, List<Token> tokens, int i, SymbolTable symbolTable) {
        int type = token.getType();

        if (KEYWORDS.contains(type))      return TYPE_KEYWORD;
        if (MACROS.contains(type))        return TYPE_KEYWORD;       // turtle/pen commands
        if (BUILTIN_FUNCS.contains(type)) return TYPE_KEYWORD;       // value-returning builtins
        if (OPERATORS.contains(type))     return TYPE_OPERATOR;

        if (type == LogoLexer.NUMBER)        return TYPE_NUMBER;
        if (type == LogoLexer.QUOTED_STRING) return TYPE_STRING;
        if (type == LogoLexer.COMMENT)       return TYPE_COMMENT;

        if (type == LogoLexer.IDENT) {
            // FOR [ i ... ] - the variable name is a loop variable, not a function
            if (isForLoopVariable(tokens, i)) return TYPE_PARAMETER;
            return TYPE_FUNCTION;
        }

        return -1; // LBRACK, RBRACK, LPAREN, RPAREN — no highlighting
    }

    private int modifiersFor(Token token, List<Token> tokens, int i) {
        if (token.getType() == LogoLexer.NUMBER) return MOD_READONLY;

        if (token.getType() == LogoLexer.IDENT) {
            Token prev = prevNonWs(tokens, i);
            if (prev != null && prev.getType() == LogoLexer.TO) {
                return MOD_DECLARATION | MOD_DEFINITION;
            }
            if (isForLoopVariable(tokens, i)) {
                return MOD_DECLARATION;
            }
            // Call site 'definition' modifier to make themes render with function colour
            // rather than falling back to plain default text.
            return MOD_DEFINITION;
        }
        return 0;
    }

    /**
     * Returns true if the IDENT at index i is the loop variable in a FOR statement.
     * Pattern in token stream: FOR  LBRACK  <this IDENT>
     */
    private boolean isForLoopVariable(List<Token> tokens, int i) {
        Token prev = prevNonWs(tokens, i);
        if (prev == null || prev.getType() != LogoLexer.LBRACK) return false;
        int lbrackIdx = lastIndexOf(tokens, prev, i);
        Token prevPrev = prevNonWs(tokens, lbrackIdx);
        return prevPrev != null && prevPrev.getType() == LogoLexer.FOR;
    }

    /** Scan backwards from `before` to find the index of token instance `target`. */
    private int lastIndexOf(List<Token> tokens, Token target, int before) {
        for (int j = before - 1; j >= 0; j--) {
            if (tokens.get(j) == target) return j;
        }
        return -1;
    }

    /** Appends one 5-integer LSP semantic token entry (delta-encoded). */
    private void encode(List<Integer> data,
                        int line, int col, int len, int tokenType, int modifiers,
                        int prevLine, int prevChar) {
        int deltaLine = line - prevLine;
        int deltaChar = (deltaLine == 0) ? col - prevChar : col;
        data.add(deltaLine);
        data.add(deltaChar);
        data.add(len);
        data.add(tokenType);
        data.add(modifiers);
    }

    /** Find the previous non-WS, non-EOL token before index i. */
    private Token prevNonWs(List<Token> tokens, int i) {
        for (int j = i - 1; j >= 0; j--) {
            int t = tokens.get(j).getType();
            if (t != LogoLexer.WS && t != LogoLexer.EOL && t != Token.EOF)
                return tokens.get(j);
        }
        return null;
    }

    /**
     * Returns true if the token at `colonIndex` is on the same line as a TO keyword,
     * meaning it's a parameter declaration (TO name :param1 :param2).
     */
    private boolean isPrecedingTokenOnSameLine(List<Token> tokens, int colonIndex, int targetType) {
        Token colon = tokens.get(colonIndex);
        for (int j = colonIndex - 1; j >= 0; j--) {
            Token t = tokens.get(j);
            if (t.getType() == Token.EOF) break;
            if (t.getLine() != colon.getLine()) break; // crossed a line boundary
            if (t.getType() == targetType) return true;
        }
        return false;
    }

    /**
     * Returns true if this COLON is on the same line as a TO + IDENT pair,
     * i.e. we are in the parameter list of a procedure definition.
     */
    private boolean isPrecedingIdentOnToLine(List<Token> tokens, int colonIndex) {
        Token colon = tokens.get(colonIndex);
        boolean sawIdent = false;
        for (int j = colonIndex - 1; j >= 0; j--) {
            Token t = tokens.get(j);
            if (t.getType() == Token.EOF) break;
            if (t.getLine() != colon.getLine()) break;
            if (t.getType() == LogoLexer.WS) continue;
            if (!sawIdent && t.getType() == LogoLexer.IDENT) { sawIdent = true; continue; }
            if (sawIdent && t.getType() == LogoLexer.TO) return true;
        }
        return false;
    }
}
