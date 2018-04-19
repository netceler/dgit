package io.insigit.jgit.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.insight.jgit.Inserter;
import io.insight.jgit.ObjectServiceGrpc;
import io.insight.jgit.PackParserRequest;
import io.insigit.jgit.RpcObjDatabase;
import org.eclipse.jgit.internal.storage.file.PackLock;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.transport.PackedObjectInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class GrpcPackParser extends PackParser {
  private final long streamId;
  private ObjectServiceGrpc.ObjectServiceBlockingStub stub;
  private Inserter inserter;



  public GrpcPackParser(RpcObjDatabase odb, Inserter inserter, InputStream in, ManagedChannel channel) {
    super(odb,in);
    this.inserter = inserter;
    streamId = GrpcClientRemoteStream.newRemoteStream(channel, in);
    stub = ObjectServiceGrpc.newBlockingStub(channel);
  }

  @Override
  public PackLock parse(ProgressMonitor receiving, ProgressMonitor resolving) throws IOException {
    try {
      stub.parse(PackParserRequest.newBuilder()
          .setInserter(inserter)
          .setStreamId(streamId)
          .build());
    } catch (StatusRuntimeException e) {
      if(e.getStatus().getCode()== Status.Code.INTERNAL){
        throw new IOException(e.getStatus().getDescription());
      }
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
