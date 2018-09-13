import org.eclipse.jgit.transport.Daemon;

import java.io.IOException;
import java.net.InetSocketAddress;

import io.insight.jgit.jdbc.SqlRepoManager;

public class GitDaemonMain {
    public static void main(final String[] args) throws IOException {

        final SqlRepoManager repoManager = new SqlRepoManager("jdbc:mysql://localhost:3306/test", "root",
                "lambdalab-dev");

        final Daemon gitDeamon = new Daemon(new InetSocketAddress(Daemon.DEFAULT_PORT));
        gitDeamon.setRepositoryResolver(repoManager);
        gitDeamon.getService("git-receive-pack").setEnabled(true);
        gitDeamon.start();
    }
}
