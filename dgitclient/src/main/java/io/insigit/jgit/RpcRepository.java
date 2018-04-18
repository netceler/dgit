package io.insigit.jgit;


import io.insigit.jgit.services.RepoService;
import io.insigit.jgit.services.RpcObjectService;
import io.insigit.jgit.services.RpcRefService;
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.util.FS;

public class RpcRepository extends DfsRepository {

  private RpcRefService refService;
  private RpcObjectService objectService;
  private RepoService repoService;
  public RpcRefDatabase rpcRefDatabase;
  public RpcObjDatabase rpcObjDatabase;

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
    if(rpcObjDatabase==null) {
      rpcObjDatabase = new RpcObjDatabase(this);
    }
    return rpcObjDatabase;
  }

  @Override
  public RefDatabase getRefDatabase() {
    if(rpcRefDatabase ==null) {
      rpcRefDatabase = new RpcRefDatabase(this);
    }
    return rpcRefDatabase;
  }

  @Override
  public StoredConfig getConfig() {
    return repoService.getConfig();
  }

  @Override
  public FS getFS() {
    return FS.detect();
  }

  public RpcRefService getRefService() {
    return refService;
  }

  public RpcObjectService getObjectService() {
    return objectService;
  }
}
