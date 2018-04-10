package io.insight.jgit.server.grpc;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.PackFile;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class LocalDiskRepoManager {

  private final File baseDir;

  private LoadingCache<String, FileRepository> repoCache;


  public LocalDiskRepoManager(File baseDir, int maxOpenRepos) {
    this.baseDir = baseDir;
    this.repoCache = CacheBuilder.newBuilder()
        .maximumSize(maxOpenRepos)
        .removalListener(this::removeListener)
        .build(new CacheLoader<String, FileRepository>() {
          @Override
          public FileRepository load(String name) throws Exception {
            File repoDir = new File(baseDir, name);
            return new FileRepository(repoDir);
          }
        });
  }

  private void removeListener(RemovalNotification<String, Repository> note) {
    note.getValue().close();
  }

  public FileRepository openRepo(String name) {
    try {
      return repoCache.get(name);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean removeRepo(String name) {
    repoCache.invalidate(name);
    File repoDir = new File(baseDir, name);
    return repoDir.delete();
  }

  public FileRepository createRepo(String name) {
    if (!exists(name)) {
      File repoDir = new File(baseDir, name);
      Git git;
      try {
        git = Git.init().setBare(true).setDirectory(repoDir).call();
      } catch (GitAPIException e) {
        throw new RuntimeException(e);
      }
      FileRepository repository = (FileRepository) git.getRepository();
      repoCache.put(name, repository);
      Collection<PackFile> packs = repository.getObjectDatabase().getPacks();

      return repository;
    } else {
      return openRepo(name);
    }
  }

  public boolean exists(String name) {
    File repoDir = new File(baseDir, name);
    return repoDir.exists();
  }
}