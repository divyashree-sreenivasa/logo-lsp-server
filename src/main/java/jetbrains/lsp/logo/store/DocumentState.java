package jetbrains.lsp.logo.store;

import jetbrains.lsp.logo.analysis.SymbolTable;
import jetbrains.lsp.logo.parser.LogoParseResult;

public class DocumentState {

    private final LogoParseResult parseResult;
    private final SymbolTable symbolTable;

    public DocumentState(LogoParseResult parseResult, SymbolTable symbolTable) {
        this.parseResult = parseResult;
        this.symbolTable = symbolTable;
    }

    public LogoParseResult getParseResult() { return parseResult; }
    public SymbolTable getSymbolTable()     { return symbolTable; }
}
