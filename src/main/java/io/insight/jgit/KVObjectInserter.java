package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.transport.PackParser;

import java.io.IOException;
import java.io.InputStream;

public class KVObjectInserter extends ObjectInserter {
  private String repositoryName;
  private KVObjectDatabase objectDatabase;
  private KVObjectService objectService;



  public KVObjectInserter(KVObjectDatabase objectDatabase,KVObjectService objectService) {
    this.objectDatabase = objectDatabase;
    this.objectService = objectService;
  }

  @Override
  public ObjectId insert(int objectType, long length, InputStream in) throws IOException {
    String id =  objectService.insertLooseObject(objectType, length, in).getObjectId();
    return ObjectId.fromString(id);
  }

  @Override
  public PackParser newPackParser(InputStream in) throws IOException {
    return new KVPackParser(objectService, this.objectDatabase, in);
  }

  @Override
  public ObjectReader newReader() {
    return new KVObjectReader(objectService);
  }

  @Override
  public void flush() throws IOException {

  }

  @Override
  public void close() {

  }
}
