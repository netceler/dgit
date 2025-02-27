import io.insight.jgit.jdbc.SqlRepoManager;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.Daemon;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

abstract class GitCommandBase {
    private static final Logger log = LoggerFactory.getLogger(GitCommandBase.class);


    @TempDir
    File baseDir;
    String repoName = "test";
    private SqlRepoManager repoManager;
    private Repository repo;

    private Daemon gitDeamon;

    abstract JdbcDatabaseContainer container();

    @BeforeEach
    void setUp() throws Exception {
        repoManager = new SqlRepoManager(container().getJdbcUrl(), container().getUsername(), container().getPassword());
        if (repoManager.exists(repoName)) {
            repoManager.delete(repoName);
        }
        repoManager.create(repoName);
        repo = repoManager.open(repoName);
        gitDeamon = new Daemon(new InetSocketAddress(Daemon.DEFAULT_PORT));
        gitDeamon.setRepositoryResolver(repoManager);
        gitDeamon.getService("git-receive-pack").setEnabled(true);
        gitDeamon.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        repo.close();
        // repoManager.delete(repoName);
        gitDeamon.stop();
    }

    @Test
    void createAndExists() throws IOException {
        repoManager.open(repoName);
        assertThat(repoManager.exists(repoName)).as("repo should exists now").isTrue();
        repoManager.delete(repoName);
        assertThat(repoManager.exists(repoName)).as("repo should not exists").isFalse();
    }

    @Test
    void fetchFromRemote() throws URISyntaxException, GitAPIException, IOException {

        final Git git = Git.wrap(repo);
        final RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin");
        remoteAddCommand.setUri(new URIish("https://github.com/spacedragon/dgit.git"));
        remoteAddCommand.call();
        final String ref = Constants.R_HEADS + Constants.MASTER;
        final RefSpec refSpec = new RefSpec().setForceUpdate(true).setSourceDestination(ref, ref);
        final FetchResult result = git.fetch().setRemote("origin").setRefSpecs(refSpec).call();
        assertThat(result.getAdvertisedRefs().size() > 0).isTrue();
        assertFileContent(repo, ref, "pom.xml", "dgit");
    }

    private void assertFileContent(final Repository repository, final String ref, final String path,
                                   final String expect) throws IOException {
        final Ref head = repository.exactRef(ref);
        try (final RevWalk walk = new RevWalk(this.repo)) {
            final RevCommit commit = walk.parseCommit(head.getObjectId());
            final RevTree tree = walk.parseTree(commit.getTree().getId());
            final TreeWalk treeWalk = TreeWalk.forPath(this.repo, path, tree);
            final byte[] bytes = this.repo.open(treeWalk.getObjectId(0)).getBytes();
            final String content = new String(bytes);
            assertThat(content.contains(expect)).isTrue();
        }
    }

    @Test
    void cloneFromDaemon() throws URISyntaxException, GitAPIException, IOException {
        Git git = Git.wrap(repo);
        final RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin");
        remoteAddCommand.setUri(new URIish("https://github.com/spacedragon/dgit.git"));
        remoteAddCommand.call();
        final String ref = Constants.R_HEADS + Constants.MASTER;
        final RefSpec refSpec = new RefSpec().setForceUpdate(true).setSourceDestination(ref, ref);
        final FetchResult result = git.fetch().setRemote("origin").setRefSpecs(refSpec).call();
        assertThat(result.getAdvertisedRefs().size() > 0).isTrue();

        final File cloneTo = new File(baseDir, "cloned");
        final CloneCommand cmd = Git.cloneRepository().setDirectory(cloneTo).setURI(
                "git://localhost/" + repoName);
        git = cmd.call();
        assertFileContent(git.getRepository(), ref, "pom.xml", "dgit");
    }

    @Test
    void push() throws GitAPIException, URISyntaxException, IOException {
        final String url = "https://github.com/lambdalab/test-repo.git";
        final File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.delete();

        final Git git = Git.cloneRepository().setURI(url).setRemote("origin").setBare(true).setDirectory(
                tempDir).call();
        final RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setUri(new URIish("git://localhost/" + repoName));
        remoteAddCommand.setName("a");
        remoteAddCommand.call();
        final Iterable<PushResult> result = git.push().add("master").setRemote("a").setForce(true).call();
        for (final PushResult r : result) {
            for (final RemoteRefUpdate update : r.getRemoteUpdates()) {
                assertThat(update.getStatus()).isEqualTo(RemoteRefUpdate.Status.OK);
            }
        }
        repo = repoManager.open(repoName);

        final Ref head = repo.exactRef("refs/heads/master");
        final ArrayList<String> files = new ArrayList<>();
        try (final RevWalk walk = new RevWalk(repo)) {
            final RevCommit commit = walk.parseCommit(head.getObjectId());
            final RevTree tree = walk.parseTree(commit.getTree().getId());
            final TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                files.add(treeWalk.getPathString());
            }
            treeWalk.close();
        }
        assertFileContent(git.getRepository(), "HEAD", "basic.cpp", "<stdio.h>");
    }

}
