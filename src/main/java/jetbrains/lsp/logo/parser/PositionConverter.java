package jetbrains.lsp.logo.parser;

import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class PositionConverter {

    /** Convert a single ANTLR token to an LSP Range covering that token's text. */
    public static Range toRange(Token token) {
        int line = token.getLine() - 1;               // ANTLR is 1-based, LSP is 0-based
        int start = token.getCharPositionInLine();    // both are 0-based for character
        int end = start + token.getText().length();
        return new Range(new Position(line, start), new Position(line, end));
    }

    /** Range spanning from the start of `from` token to the end of `to` token (may be multi-line). */
    public static Range toRange(Token from, Token to) {
        int startLine = from.getLine() - 1;
        int startChar = from.getCharPositionInLine();
        int endLine   = to.getLine() - 1;
        int endChar   = to.getCharPositionInLine() + to.getText().length();
        return new Range(new Position(startLine, startChar), new Position(endLine, endChar));
    }

    public static Position toPosition(Token token) {
        return new Position(token.getLine() - 1, token.getCharPositionInLine());
    }

    /** Returns true if `pos` falls within `range` (LSP convention: end is exclusive). */
    public static boolean contains(Range range, Position pos) {
        Position start = range.getStart();
        Position end = range.getEnd();
        if (pos.getLine() < start.getLine() || pos.getLine() > end.getLine()) return false;
        if (pos.getLine() == start.getLine() && pos.getCharacter() < start.getCharacter()) return false;
        if (pos.getLine() == end.getLine() && pos.getCharacter() >= end.getCharacter()) return false;
        return true;
    }
}
