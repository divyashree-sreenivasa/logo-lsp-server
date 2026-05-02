package jetbrains.lsp.logo;

import jetbrains.lsp.logo.feature.DiagnosticsProvider;
import jetbrains.lsp.logo.feature.GotoDeclarationProvider;
import jetbrains.lsp.logo.feature.HoverProvider;
import jetbrains.lsp.logo.feature.SemanticTokensProvider;
import jetbrains.lsp.logo.store.DocumentState;
import jetbrains.lsp.logo.store.DocumentStore;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LogoTextDocumentService implements TextDocumentService {

    private static final Logger log = LoggerFactory.getLogger(LogoTextDocumentService.class);

    private final LogoLanguageServer server;
    private LanguageClient client;

    private final DocumentStore store                      = new DocumentStore();
    private final SemanticTokensProvider semanticTokens   = new SemanticTokensProvider();
    private final GotoDeclarationProvider gotoDeclaration = new GotoDeclarationProvider();
    private final DiagnosticsProvider diagnostics          = new DiagnosticsProvider();
    private final HoverProvider hover                      = new HoverProvider();

    public LogoTextDocumentService(LogoLanguageServer server) {
        this.server = server;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    // Document lifecycle

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri  = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();
        log.info("didOpen {}", uri);
        analyzeAndPublish(uri, text);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri  = params.getTextDocument().getUri();
        String text = params.getContentChanges().get(0).getText();
        log.info("didChange {}", uri);
        analyzeAndPublish(uri, text);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        store.remove(uri);
        // Clear diagnostics when the file is closed
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
        }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    // LSP features

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        DocumentState state = store.get(params.getTextDocument().getUri());
        if (state == null) return CompletableFuture.completedFuture(new SemanticTokens(List.of()));
        return CompletableFuture.completedFuture(semanticTokens.provide(state));
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
    declaration(DeclarationParams params) {
        String uri        = params.getTextDocument().getUri();
        Position cursor   = params.getPosition();
        DocumentState state = store.get(uri);
        if (state == null) return CompletableFuture.completedFuture(Either.forLeft(List.of()));
        Optional<Location> location = gotoDeclaration.resolve(uri, cursor, state);
        return CompletableFuture.completedFuture(
                Either.forLeft(location.map(List::of).orElse(List.of())));
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        String uri        = params.getTextDocument().getUri();
        Position cursor   = params.getPosition();
        DocumentState state = store.get(uri);
        if (state == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.completedFuture(
                hover.hover(cursor, state).orElse(null));
    }

    // Internal
    private void analyzeAndPublish(String uri, String text) {
        DocumentState state = store.update(uri, text);
        if (client == null) return;

        List<Diagnostic> diags = diagnostics.check(
                state.getParseResult().getTree(),
                state.getSymbolTable(),
                state.getParseResult().getSyntaxErrors());

        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diags));
    }
}
