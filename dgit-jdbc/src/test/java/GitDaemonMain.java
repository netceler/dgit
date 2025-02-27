import io.insight.jgit.jdbc.SqlRepoManager;
import org.eclipse.jgit.transport.Daemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GitDaemonMain {
    private static final Logger log = LoggerFactory.getLogger(GitDaemonMain.class);

    public static void main(final String[] args) throws IOException {
        final JdbcDatabaseContainer mysql = new PostgreSQLContainer().withInitScript("pg-init.sql").withDatabaseName("foo");
        mysql.start();

        final SqlRepoManager repoManager = new SqlRepoManager(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());

        final Daemon gitDeamon = new Daemon(new InetSocketAddress(Daemon.DEFAULT_PORT));
        gitDeamon.setRepositoryResolver(repoManager);
        gitDeamon.getService("git-receive-pack").setEnabled(true);
        gitDeamon.start();
        log.info("GIT Daemon started : git clone git://{}:{}/{}", "localhost", gitDeamon.getAddress().getPort(), "");

    }
}
