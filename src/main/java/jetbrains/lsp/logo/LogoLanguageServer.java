package jetbrains.lsp.logo;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LogoLanguageServer implements LanguageServer, LanguageClientAware {

    private final LogoTextDocumentService textDocumentService;
    private final LogoWorkspaceService workspaceService;
    private LanguageClient client;

    public LogoLanguageServer() {
        this.textDocumentService = new LogoTextDocumentService(this);
        this.workspaceService = new LogoWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities caps = new ServerCapabilities();

        // Full sync, re-send entire document on every change (LOGO files are small)
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);

        // Semantic tokens (syntax highlighting)
        SemanticTokensLegend legend = new SemanticTokensLegend(
                List.of("keyword", "function", "parameter", "variable",
                        "number", "string", "comment", "operator"),
                List.of("declaration", "definition", "readonly")
        );
        SemanticTokensWithRegistrationOptions semanticTokensOptions =
                new SemanticTokensWithRegistrationOptions(legend, true, null);
        caps.setSemanticTokensProvider(semanticTokensOptions);

        // Go-to-declaration
        caps.setDeclarationProvider(Either.forLeft(true));

        // Hover
        caps.setHoverProvider(Either.forLeft(true));

        ServerInfo serverInfo = new ServerInfo("LOGO LSP Server", "1.0");
        return CompletableFuture.completedFuture(new InitializeResult(caps, serverInfo));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        this.textDocumentService.setClient(client);
    }

    public LanguageClient getClient() {
        return client;
    }
}
