package jetbrains.lsp.logo.store;

import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.analysis.SymbolTableBuilder;
import jetbrains.lsp.logo.parser.LogoParseResult;
import jetbrains.lsp.logo.parser.LogoParserFacade;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentStore {

    private final Map<String, DocumentState> documents = new ConcurrentHashMap<>();

    public DocumentState update(String uri, String text) {
        LogoParseResult parseResult = LogoParserFacade.parse(text);
        SymbolTable symbolTable = new SymbolTableBuilder().build(parseResult.getTree());
        DocumentState state = new DocumentState(parseResult, symbolTable);
        documents.put(uri, state);
        return state;
    }

    public DocumentState get(String uri) {
        return documents.get(uri);
    }

    public void remove(String uri) {
        documents.remove(uri);
    }
}
