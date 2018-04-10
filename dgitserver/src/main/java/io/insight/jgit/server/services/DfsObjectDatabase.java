package io.insight.jgit.server.services;

import com.google.common.io.Files;
import org.eclipse.jgit.internal.fsck.FsckPackParser;
import org.eclipse.jgit.internal.storage.dfs.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.PackFile;
import org.eclipse.jgit.internal.storage.pack.PackExt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DfsObjectDatabase extends DfsObjDatabase {

  FileRepository repository;
  File tempDir = Files.createTempDir();
  protected DfsObjectDatabase(DfsRepository repository) {
    super(repository, new DfsReaderOptions());
  }

  @Override
  protected DfsPackDescription newPack(PackSource source) throws IOException {
    String name = "pack-"+ source.name().toLowerCase() + "-" + UUID.randomUUID().toString().replace("-","");
    DfsPackDescription desc = new DfsPackDescription(this.getRepository().getDescription(), name);
    return desc;
  }

  @Override
  protected void commitPackImpl(Collection<DfsPackDescription> packs, Collection<DfsPackDescription> replaces) throws IOException {
    for (DfsPackDescription desc : packs) {

      for (PackExt ext : PackExt.values()) {
       if (desc.hasFileExt(ext)){
         String name = desc.getFileName(ext);
       }
      }
    }
  }

  @Override
  protected void rollbackPack(Collection<DfsPackDescription> desc) {

  }

  @Override
  protected List<DfsPackDescription> listPacks() throws IOException {
    repository.getObjectDatabase().getPacks().stream().map(f -> {
      String packName = f.getPackName();

      DfsPackDescription desc=new DfsPackDescription(getRepository().getDescription(), packName);

    });
    return null;
  }

  @Override
  protected ReadableChannel openFile(DfsPackDescription desc, PackExt ext) throws FileNotFoundException, IOException {
    return null;
  }

  @Override
  protected DfsOutputStream writeFile(DfsPackDescription desc, PackExt ext) throws IOException {
    desc.addFileExt(ext);

    return null;
  }
}
