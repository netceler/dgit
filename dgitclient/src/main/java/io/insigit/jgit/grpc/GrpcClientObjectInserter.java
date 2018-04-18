package io.insigit.jgit.grpc;

import io.insight.jgit.Empty;
import io.insight.jgit.Inserter;
import io.insight.jgit.ObjectServiceGrpc;
import io.insigit.jgit.RpcObjDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.transport.PackParser;

import java.io.IOException;
import java.io.InputStream;

public class GrpcClientObjectInserter extends ObjectInserter {
  private final ObjectServiceGrpc.ObjectServiceBlockingStub stub;
  private RpcObjDatabase db;
  private Inserter inserter;

  public GrpcClientObjectInserter(RpcObjDatabase db, ObjectServiceGrpc.ObjectServiceBlockingStub stub) {
    this.db = db;
    this.inserter = stub.newInserter(Empty.getDefaultInstance());
    this.stub=stub;
  }
  private synchronized Inserter getInserter() {
    if (inserter == null) {
      this.inserter = stub.newInserter(Empty.getDefaultInstance());
    }
    return this.inserter;
  }

  @Override
  public ObjectId insert(int objectType, long length, InputStream in) throws IOException {
    return db.getObjectService().insert(getInserter(),objectType, length, in);
  }

  @Override
  public PackParser newPackParser(InputStream in) throws IOException {
    return db.getObjectService().newPackParser(getInserter(),db, in);
  }

  @Override
  public ObjectReader newReader() {
    return db.newReader();
  }

  @Override
  public void flush() throws IOException {
    this.stub.flushInserter(inserter);
  }

  @Override
  public void close() {
    this.stub.closeInserter(inserter);
    this.inserter = null;
  }
}
