package jetbrains.lsp.logo.feature;

import jetbrains.lsp.logo.analysis.ProcedureSymbol;
import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.parser.LogoLexer;
import jetbrains.lsp.logo.parser.LogoParseResult;
import jetbrains.lsp.logo.store.DocumentState;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;

import java.util.List;
import java.util.Optional;

public class GotoDeclarationProvider {

    /**
     * Resolves the cursor position to a declaration Location, or empty if nothing is navigable.
     *
     * Two cases:
     *  1. Cursor on a procedure name (IDENT in a call): jump to the TO line's name token
     *  2. Cursor on a variable reference (:name): jump to the :param on the TO line
     */
    public Optional<Location> resolve(String uri, Position cursor, DocumentState state) {
        List<Token> tokens = state.getParseResult().getAllTokens();
        SymbolTable symbolTable = state.getSymbolTable();

        Token hit = tokenAt(tokens, cursor);
        if (hit == null) return Optional.empty();

        // Case 1: cursor on IDENT - could be a procedure call
        if (hit.getType() == LogoLexer.IDENT) {
            // Guard: skip the IDENT that is the procedure name in its own declaration (after TO)
            Token prev = prevMeaningful(tokens, indexOf(tokens, hit));
            boolean isDeclaration = prev != null && prev.getType() == LogoLexer.TO;
            if (!isDeclaration) {
                ProcedureSymbol proc = symbolTable.getProcedure(hit.getText());
                if (proc != null) {
                    return Optional.of(new Location(uri, proc.getNameRange()));
                }
            }
        }

        // Case 2: cursor on COLON - treat as the :variable that follows
        if (hit.getType() == LogoLexer.COLON) {
            int idx = indexOf(tokens, hit);
            if (idx + 1 < tokens.size() && tokens.get(idx + 1).getType() == LogoLexer.IDENT) {
                return resolveVariable(tokens.get(idx + 1), cursor, uri, symbolTable);
            }
        }

        // Case 2b: cursor on the IDENT part of :variable (user clicked the name, not the colon)
        if (hit.getType() == LogoLexer.IDENT) {
            int idx = indexOf(tokens, hit);
            Token prev = prevMeaningful(tokens, idx);
            if (prev != null && prev.getType() == LogoLexer.COLON) {
                return resolveVariable(hit, cursor, uri, symbolTable);
            }
        }

        return Optional.empty();
    }

    private Optional<Location> resolveVariable(Token nameToken, Position cursor,
                                                String uri, SymbolTable symbolTable) {
        String varName = nameToken.getText().toUpperCase();

        // 1. Check procedure parameters first
        Optional<ProcedureSymbol> enclosing = symbolTable.enclosingProcedure(cursor);
        if (enclosing.isPresent()) {
            ProcedureSymbol proc = enclosing.get();
            if (proc.getParamRanges().containsKey(varName)) {
                return Optional.of(new Location(uri, proc.getParamRanges().get(varName)));
            }
        }

        // 2. FOR loop variable: innermost enclosing FOR that declares this name
        Optional<SymbolTable.ForLoopVariable> forVar =
                symbolTable.getForLoopVariable(varName, cursor);
        if (forVar.isPresent()) {
            return Optional.of(new Location(uri, forVar.get().nameRange()));
        }

        // 3. Fall back to MAKE "varname declarations
        return symbolTable.getMakeVariable(varName)
                .map(mv -> new Location(uri, mv.nameRange()));
    }

    /** Returns the token whose text span contains the given cursor position, or null. */
    private Token tokenAt(List<Token> tokens, Position cursor) {
        int line = cursor.getLine();
        int ch   = cursor.getCharacter();
        for (Token t : tokens) {
            if (t.getType() == Token.EOF) continue;
            int tokenLine  = t.getLine() - 1;
            int tokenStart = t.getCharPositionInLine();
            int tokenEnd   = tokenStart + t.getText().length();
            if (tokenLine == line && tokenStart <= ch && ch < tokenEnd) {
                return t;
            }
        }
        return null;
    }

    /** Linear scan for the index of a known token instance. */
    private int indexOf(List<Token> tokens, Token target) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) == target) return i;
        }
        return -1;
    }

    /** Previous token that is not WS, EOL, or COMMENT. */
    private Token prevMeaningful(List<Token> tokens, int idx) {
        for (int i = idx - 1; i >= 0; i--) {
            int type = tokens.get(i).getType();
            if (type != LogoLexer.WS && type != LogoLexer.EOL
                    && type != LogoLexer.COMMENT && type != Token.EOF) {
                return tokens.get(i);
            }
        }
        return null;
    }
}
