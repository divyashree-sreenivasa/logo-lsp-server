package jetbrains.lsp.logo.parser;

import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

public class LogoParserFacade {

    public static LogoParseResult parse(String source) {
        List<LogoParseResult.SyntaxError> errors = new ArrayList<>();

        CharStream input = CharStreams.fromString(source);
        LogoLexer lexer = new LogoLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener(errors));

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        LogoParser parser = new LogoParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener(errors));

        LogoParser.ProgramContext tree = parser.program();

        // Fill the token stream so hidden-channel tokens (comments) are available
        tokens.fill();

        return new LogoParseResult(tree, tokens, errors);
    }

    private static BaseErrorListener errorListener(List<LogoParseResult.SyntaxError> errors) {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                errors.add(new LogoParseResult.SyntaxError(line, charPositionInLine, msg));
            }
        };
    }
}
