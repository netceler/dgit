package io.insigit.jgit;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.transport.PackParser;

import java.io.IOException;
import java.io.InputStream;

public class RpcObjectInserter extends ObjectInserter {
  private RpcObjDatabase db;

  public RpcObjectInserter(RpcObjDatabase db) {
    this.db = db;
  }

  @Override
  public ObjectId insert(int objectType, long length, InputStream in) throws IOException {
    return db.getObjectService().insert(objectType,length,in);
  }

  @Override
  public PackParser newPackParser(InputStream in) throws IOException {
    return new RpcPackParser(db,in);
  }

  @Override
  public ObjectReader newReader() {
    return db.newReader();
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public void close() {
  }
}
