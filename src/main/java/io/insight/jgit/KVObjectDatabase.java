package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;

public class KVObjectDatabase extends ObjectDatabase {


  private KVObjectService kvObjectService;

  public KVObjectDatabase(KVObjectService kvObjectService) {
    this.kvObjectService = kvObjectService;
  }

  @Override
  public ObjectInserter newInserter() {
    return new KVObjectInserter(this,kvObjectService);
  }

  @Override
  public ObjectReader newReader() {
    return new KVObjectReader(kvObjectService);
  }

  @Override
  public void close() {

  }
}
