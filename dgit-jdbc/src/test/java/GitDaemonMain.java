import io.insight.jgit.jdbc.MysqlRepoManager;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.Daemon;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GitDaemonMain {
  public static void main(String[] args) throws IOException {



    MysqlRepoManager repoManager = new MysqlRepoManager("jdbc:mysql://localhost:3306/test", "root", "lambdalab-dev");

    Daemon gitDeamon = new Daemon(new InetSocketAddress(Daemon.DEFAULT_PORT));
    gitDeamon.setRepositoryResolver(repoManager);
    gitDeamon.getService("git-receive-pack").setEnabled(true);
    gitDeamon.start();
  }
}
