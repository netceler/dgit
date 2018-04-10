package io.insigit.jgit;

import io.insigit.jgit.services.RpcObjectService;
import org.eclipse.jgit.internal.storage.dfs.*;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.lib.ObjectInserter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class RpcObjDatabase extends DfsObjDatabase {
  public RpcObjDatabase(RpcRepository repository) {
    super(repository, new DfsReaderOptions());
  }

  @Override
  protected RpcRepository getRepository() {
    return (RpcRepository) super.getRepository();
  }

  public RpcObjectService getObjectService(){
    return getRepository().getObjectService();
  }

  @Override
  protected DfsPackDescription newPack(PackSource source) throws IOException {
    throw new UnsupportedOperationException();
  }


  @Override
  public DfsReader newReader() {
    return new RpcObjectReader(this);
  }

  @Override
  public ObjectInserter newInserter() {
    return new RpcObjectInserter(this);
  }

  @Override
  public void close() {
  }

  @Override
  protected void commitPackImpl(Collection<DfsPackDescription> desc, Collection<DfsPackDescription> replaces) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void rollbackPack(Collection<DfsPackDescription> desc) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected List<DfsPackDescription> listPacks() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ReadableChannel openFile(DfsPackDescription desc, PackExt ext) throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected DfsOutputStream writeFile(DfsPackDescription desc, PackExt ext) throws IOException {
    throw new UnsupportedOperationException();
  }
}
