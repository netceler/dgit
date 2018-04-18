package io.insight.jgit.server.grpc;

import io.insigit.jgit.grpc.GrpcClientRepoManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class GrpcRepoManagerTest {

  private GrpcServer server;
  private GrpcClientRepoManager repoManager;
  File baseDir = new File("/tmp");
  String repoName = "test";
  private Repository repo;

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
  }

  @After
  public void tearDown() throws Exception {
    repo.close();
    repoManager.delete(repoName);
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
  public void fetch() throws URISyntaxException, GitAPIException {
    Git git = Git.wrap(repo);
    RemoteAddCommand remoteAddCommand = git.remoteAdd();
    remoteAddCommand.setName("origin");
    remoteAddCommand.setUri(new URIish("/tmp/MEDIUM"));
    RemoteConfig cfgResult = remoteAddCommand.call();
    FetchResult result = git.fetch().setRemote("origin").setRefSpecs("refs/*:refs/*").call();
    assertTrue(result.getAdvertisedRefs().size() > 0);
  }
}