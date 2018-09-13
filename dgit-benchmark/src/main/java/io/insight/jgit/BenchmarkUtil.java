package io.insight.jgit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.Daemon;
import org.eclipse.jgit.transport.DaemonClient;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import io.insight.jgit.cache.CachedObjectService;
import io.insight.jgit.cache.CachedRefService;
import io.insight.jgit.cache.CachedRepoManager;
import io.insight.jgit.jdbc.SqlRepoManager;

public class BenchmarkUtil implements RepositoryResolver<DaemonClient> {

    private final Daemon server;

    public BenchmarkUtil() {
        server = new Daemon(new InetSocketAddress(Daemon.DEFAULT_PORT));
        server.getService("git-receive-pack").setEnabled(true);
        server.setRepositoryResolver(this);
    }

    public void startDaemon() throws IOException {
        server.start();
    }

    public void stopDaemon() {
        server.stop();
    }

    private final File baseDir = new File("/tmp");

    private final SqlRepoManager mysqlRepoManager = new SqlRepoManager("jdbc:mysql://localhost:3306/test",
            "root", "lambdalab-dev");

    private final CachedRepoManager cachedRepoManager = new CachedRepoManager(mysqlRepoManager);

    public void printCacheStats() {
        final CachedObjectService objService = cachedRepoManager.cacheLayer().objService();
        final CachedRefService refService = cachedRepoManager.cacheLayer().refService();
        printRate("Ref cache ", refService.cacheStats());
        printRate("object cache ", objService.objectCacheStats());
        printRate("object data cache ", objService.dataCacheStats());
    }

    private static void printRate(final String name, final CacheStats refStats) {
        System.out.println(String.format("%s %d/%d = %.1f %% ", name, refStats.hitCount(),
                refStats.requestCount(), refStats.hitRate() * 100));
    }

    @Override
    public Repository open(final DaemonClient req, final String url) throws RepositoryNotFoundException,
            ServiceNotAuthorizedException, ServiceNotEnabledException, ServiceMayNotContinueException {
        final String strs[] = url.split("/");
        final String engine = strs[0];
        final String name = strs[1];
        try (Repository repo = openRepo(engine, name)) {
            if (repo == null) {
                throw new RepositoryNotFoundException(url);
            }
            return repo;
        } catch (GitAPIException | IOException e) {
            throw new ServiceMayNotContinueException(e);
        }
    }

    public Repository openRepo(final String engine, final String name) throws IOException, GitAPIException {
        switch (engine) {
            case "file":
                final File dir = new File(baseDir, name);
                if (dir.exists()) {
                    final Git git = Git.open(dir);
                    return git.getRepository();
                } else {
                    final Git git = Git.init().setBare(true).setDirectory(dir).call();
                    return git.getRepository();
                }
            case "dgit":
                if (cachedRepoManager.exists(name)) {
                    return cachedRepoManager.open(name);
                }
                return cachedRepoManager.create(name);
        }
        return null;
    }

    private final String SMALL_REPO_URL = "https://github.com/lambdalab/test-repo.git";

    private final String MEDIUM_REPO_URL = "https://github.com/lambdalab/javascript-typescript-langserver.git";

    private final String LARGE_REPO_URL = "https://github.com/lambdalab/gerrit.git";

    public String repoUrl(final String repo) {
        switch (repo) {
            case "SMALL":
                return SMALL_REPO_URL;
            case "MEDIUM":
                return MEDIUM_REPO_URL;
            default:
                return LARGE_REPO_URL;
        }
    }

    public void clone(final File path, final String repoName) throws GitAPIException {
        if (!path.exists()) {
            final Git result = Git.cloneRepository().setURI(repoUrl(repoName)).setDirectory(path).call();
            result.close();
        }
    }

    public void push(final File path, final String engine, final String repoName)
            throws IOException, URISyntaxException, GitAPIException {
        final Git git = Git.open(path);
        final RemoteAddCommand remote = git.remoteAdd();
        final String remoteName = engine;
        remote.setName(remoteName);
        remote.setUri(new URIish("git://localhost/" + engine + "/" + repoName));
        remote.call();
        final PushCommand push = git.push().add("master").setRemote(remoteName).setForce(true);
        push.call();
    }
}
