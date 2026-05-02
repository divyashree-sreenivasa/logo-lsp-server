package jetbrains.lsp.logo;

import jetbrains.lsp.logo.feature.GotoDeclarationProvider;
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

    private final DocumentStore store = new DocumentStore();
    private final SemanticTokensProvider semanticTokensProvider = new SemanticTokensProvider();
    private final GotoDeclarationProvider gotoDeclarationProvider = new GotoDeclarationProvider();

    public LogoTextDocumentService(LogoLanguageServer server) {
        this.server = server;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    // Document lifecycle

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();
        log.info("didOpen {}", uri);
        store.update(uri, text);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getContentChanges().get(0).getText();
        log.info("didChange {}", uri);
        store.update(uri, text);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        store.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    // LSP features

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        String uri = params.getTextDocument().getUri();
        DocumentState state = store.get(uri);
        if (state == null) {
            return CompletableFuture.completedFuture(new SemanticTokens(List.of()));
        }
        return CompletableFuture.completedFuture(semanticTokensProvider.provide(state));
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
    declaration(DeclarationParams params) {
        String uri = params.getTextDocument().getUri();
        Position cursor = params.getPosition();
        DocumentState state = store.get(uri);
        if (state == null) {
            return CompletableFuture.completedFuture(Either.forLeft(List.of()));
        }
        Optional<Location> location = gotoDeclarationProvider.resolve(uri, cursor, state);
        return CompletableFuture.completedFuture(
                Either.forLeft(location.map(List::of).orElse(List.of())));
    }
}
