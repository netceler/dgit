package io.insigit.jgit;


import io.insigit.jgit.services.RepoService;
import io.insigit.jgit.services.RpcObjectService;
import io.insigit.jgit.services.RpcRefService;
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.lib.*;

public class RpcRepository extends DfsRepository {

  private RpcRefService refService;
  private RpcObjectService objectService;
  private RepoService repoService;

  public RpcRepository(DfsRepositoryBuilder builder,
                       RepoService repoService,
                       RpcRefService refService,
                       RpcObjectService objectService) {
    super(builder);
    this.refService = refService;
    this.objectService = objectService;
    this.repoService = repoService;
  }

  @Override
  public void create(boolean b) {
    repoService.create();
  }

  @Override
  public DfsObjDatabase getObjectDatabase() {
    return new RpcObjDatabase(this);
  }

  @Override
  public RefDatabase getRefDatabase() {
    return new RpcRefDatabase(this);
  }

  @Override
  public StoredConfig getConfig() {
    return repoService.getConfig();
  }

  public RpcRefService getRefService() {
    return refService;
  }

  public RpcObjectService getObjectService() {
    return objectService;
  }
}
