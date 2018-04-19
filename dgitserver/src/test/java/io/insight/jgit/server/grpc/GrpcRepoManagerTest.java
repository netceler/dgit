package io.insight.jgit.server.grpc;

import com.google.common.io.Files;
import io.insigit.jgit.grpc.GrpcClientRepoManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class GrpcRepoManagerTest implements RepositoryResolver<DaemonClient> {

  private GrpcServer server;
  private GrpcClientRepoManager repoManager;
  File baseDir = new File("/tmp");
  String repoName = "test";
  private Repository repo;
  private Daemon gitDeamon;

  @Before
  public void setUp() throws Exception {
    server = new GrpcServer(10000, baseDir);
    server.start();
    repoManager = new GrpcClientRepoManager("localhost", 10000);
    if (repoManager.exists(repoName)) {
      repoManager.delete(repoName);
    }
    repoManager.create(repoName);
    repo = repoManager.open(repoName);
    gitDeamon = new Daemon(new InetSocketAddress(Daemon.DEFAULT_PORT));
    gitDeamon.setRepositoryResolver(this);
    gitDeamon.getService("git-receive-pack").setEnabled(true);
    gitDeamon.start();
  }

  @After
  public void tearDown() throws Exception {
    repo.close();
    repoManager.delete(repoName);
    gitDeamon.stop();
    server.stop();
  }

  @Test
  public void testCreateAndExists() throws IOException {
    File dir = new File(baseDir, "test");
    if (dir.exists()) {
      FileUtils.delete(dir, FileUtils.RECURSIVE);
      assertFalse(dir.exists());
    }
    assertFalse("repo should not exists", repoManager.exists("test"));
    Repository repo = repoManager.create("test");
    assertTrue("repo should exists now", repoManager.exists("test"));
    repoManager.delete("test");
    assertFalse("repo should not exists", repoManager.exists("test"));
  }

 
  @Test
  public void fetch() throws URISyntaxException, GitAPIException, IOException {
//    Repository repo = Git.init().setBare(true).setDirectory(new File("/tmp/1")).call().getRepository();

    Git git = Git.wrap(repo);
    RemoteAddCommand remoteAddCommand = git.remoteAdd();
    remoteAddCommand.setName("origin");
    remoteAddCommand.setUri(new URIish("https://github.com/spacedragon/dgit.git"));
    remoteAddCommand.call();
    String ref = Constants.R_HEADS + Constants.MASTER;
    RefSpec refSpec=new RefSpec().setForceUpdate(true).setSourceDestination(ref,ref);
    FetchResult result = git.fetch().setRemote("origin").setRefSpecs(refSpec).call();
    assertTrue(result.getAdvertisedRefs().size() > 0);
    Ref head = repo.exactRef(ref);
    try(RevWalk walk = new RevWalk(repo)) {
      RevCommit commit = walk.parseCommit(head.getObjectId());
      RevTree tree = walk.parseTree(commit.getTree().getId());
      TreeWalk treeWalk = TreeWalk.forPath(repo, "pom.xml", tree);
      byte[] bytes = repo.open(treeWalk.getObjectId(0)).getBytes();
      String content = new String(bytes);
      assertTrue(content.contains("dgit"));
    }
  }

  @Test
  public void push() throws GitAPIException, URISyntaxException, IOException {
    String url = "https://github.com/lambdalab/test-repo.git";
    File tempDir = Files.createTempDir();
    tempDir.delete();

    Git git = Git.cloneRepository().setURI(url)
        .setRemote("origin")
        .setBare(true).setDirectory(tempDir).call();
    RemoteAddCommand remoteAddCommand = git.remoteAdd();
    remoteAddCommand.setUri(new URIish("git://localhost/"+repoName));
    remoteAddCommand.setName("a");
    remoteAddCommand.call();
    Iterable<PushResult> result = git.push()
        .add("master")
        .setRemote("a")
        .setForce(true)
        .call();
    for (PushResult r : result) {
      for (RemoteRefUpdate update : r.getRemoteUpdates()) {
        assertEquals(RemoteRefUpdate.Status.OK, update.getStatus());
      }
    }
    repo = repoManager.open(repoName);

    Ref head = repo.exactRef("refs/heads/master");
    ArrayList<String> files=new ArrayList<>();
    try (RevWalk walk = new RevWalk(repo)) {
      RevCommit commit = walk.parseCommit(head.getObjectId());
      RevTree tree = walk.parseTree(commit.getTree().getId());
      TreeWalk treeWalk = new TreeWalk(repo);
      treeWalk.addTree(tree);
      treeWalk.setRecursive(true);
      while (treeWalk.next()) {
        files.add(treeWalk.getPathString());
      }
      treeWalk.close();
    }
  }

  @Override
  public Repository open(DaemonClient req, String name) throws RepositoryNotFoundException, ServiceNotAuthorizedException, ServiceNotEnabledException, ServiceMayNotContinueException {
    if(repoManager.exists(name)){
      return repoManager.open(name);
    }else {
      return repoManager.create(name);
    }
  }
}