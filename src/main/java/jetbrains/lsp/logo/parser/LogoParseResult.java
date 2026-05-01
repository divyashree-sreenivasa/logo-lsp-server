package jetbrains.lsp.logo.parser;

import jetbrains.lsp.logo.parser.LogoParser.ProgramContext;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class LogoParseResult {

    private final ProgramContext tree;
    private final CommonTokenStream tokenStream;
    private final List<SyntaxError> syntaxErrors;

    public LogoParseResult(ProgramContext tree, CommonTokenStream tokenStream,
                           List<SyntaxError> syntaxErrors) {
        this.tree = tree;
        this.tokenStream = tokenStream;
        this.syntaxErrors = syntaxErrors;
    }

    public ProgramContext getTree() { return tree; }
    public CommonTokenStream getTokenStream() { return tokenStream; }
    public List<SyntaxError> getSyntaxErrors() { return syntaxErrors; }

    /** All tokens including those on hidden channels (e.g. comments). */
    public List<Token> getAllTokens() {
        tokenStream.fill();
        return tokenStream.getTokens();
    }

    public record SyntaxError(int line, int charPositionInLine, String message) {}
}
