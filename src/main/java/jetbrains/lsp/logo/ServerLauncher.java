package jetbrains.lsp.logo;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.util.concurrent.ExecutionException;

public class ServerLauncher {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        LogoLanguageServer server = new LogoLanguageServer();

        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
                server,
                System.in,
                System.out
        );

        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }
}
