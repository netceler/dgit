package io.insigit.jgit.grpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.insight.jgit.Inserter;
import io.insight.jgit.PackParserRequest;
import io.insigit.jgit.RpcObjDatabase;
import org.eclipse.jgit.internal.storage.file.PackLock;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.transport.PackedObjectInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GrpcPackParser extends PackParser {
  private Inserter inserter;
  private final StreamObserver<PackParserRequest> observer;
  private final CompletableFuture<Void> serverFuture;
  private final InputStream in;

  public GrpcPackParser(RpcObjDatabase db, Inserter inserter, InputStream src, StreamObserver<PackParserRequest> observer, CompletableFuture<Void> serverFuture) {
    super(db, src);
    this.inserter = inserter;
    this.observer = observer;
    this.in = src;
    this.serverFuture = serverFuture;
  }

  @Override
  public PackLock parse(ProgressMonitor receiving, ProgressMonitor resolving) throws IOException {
    byte[] buf = new byte[GrpcClientObjectService.BUFFER_SIZE];
    try {
      int read;
      while ((read = in.read(buf)) != -1) {
        PackParserRequest.Builder req = PackParserRequest.newBuilder()
            .setInserter(inserter)
            .setData(ByteString.copyFrom(buf, 0, read));
        observer.onNext(req.build());
      }
      observer.onCompleted();
    } catch (IOException e) {
      observer.onError(e);
    }
    try {
      serverFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
    return null;
  }


  @Override
  protected void onStoreStream(byte[] raw, int pos, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onObjectHeader(Source src, byte[] raw, int pos, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onObjectData(Source src, byte[] raw, int pos, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onInflatedObjectData(PackedObjectInfo obj, int typeCode, byte[] data) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onPackHeader(long objCnt) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onPackFooter(byte[] hash) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean onAppendBase(int typeCode, byte[] data, PackedObjectInfo info) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onEndThinPack() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ObjectTypeAndSize seekDatabase(PackedObjectInfo obj, ObjectTypeAndSize info) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ObjectTypeAndSize seekDatabase(UnresolvedDelta delta, ObjectTypeAndSize info) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected int readDatabase(byte[] dst, int pos, int cnt) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean checkCRC(int oldCRC) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onBeginWholeObject(long streamPosition, int type, long inflatedSize) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onEndWholeObject(PackedObjectInfo info) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onBeginOfsDelta(long deltaStreamPosition, long baseStreamPosition, long inflatedSize) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onBeginRefDelta(long deltaStreamPosition, AnyObjectId baseId, long inflatedSize) throws IOException {
    throw new UnsupportedOperationException();
  }
}
