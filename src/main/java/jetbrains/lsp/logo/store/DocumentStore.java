package jetbrains.lsp.logo.store;

import jetbrains.lsp.logo.parser.LogoParseResult;
import jetbrains.lsp.logo.parser.LogoParserFacade;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentStore {

    private final Map<String, LogoParseResult> documents = new ConcurrentHashMap<>();

    public LogoParseResult update(String uri, String text) {
        LogoParseResult result = LogoParserFacade.parse(text);
        documents.put(uri, result);
        return result;
    }

    public LogoParseResult get(String uri) {
        return documents.get(uri);
    }

    public void remove(String uri) {
        documents.remove(uri);
    }
}
