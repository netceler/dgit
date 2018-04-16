package io.insigit.jgit.grpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.insight.jgit.*;
import io.insigit.jgit.RpcObjDatabase;
import io.insigit.jgit.services.RpcObjectService;
import io.insigit.jgit.utils.Converters;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.PackParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class GrpcClientObjectService implements RpcObjectService {

  public static final int BUFFER_SIZE = 8 * 1024;
  private final ObjectServiceGrpc.ObjectServiceBlockingStub stub;
  private final ObjectServiceGrpc.ObjectServiceStub asycStub;

  public GrpcClientObjectService(RepoClient client) {
    stub = ObjectServiceGrpc.newBlockingStub(client.channel());
    asycStub = ObjectServiceGrpc.newStub(client.channel());
  }

  @Override
  public Collection<ObjectId> resolve(AbbreviatedObjectId id) {
    ObjectIdList result = stub.resolve(ResolveRequest.newBuilder().setAbbreviatedObjectId(
        io.insight.jgit.ObjectId.newBuilder().setId(id.name())
    ).build());
    return result.getObjectIdsList().stream().map(Converters::fromObjId).collect(toList());
  }

  @Override
  public Set<ObjectId> getShallowCommits() {
    return stub.shallowCommits(Empty.getDefaultInstance()).getObjectIdsList().stream()
        .map(Converters::fromObjId).collect(toSet());
  }

  @Override
  public ObjectLoader open(AnyObjectId objectId, int typeHint) throws IOException {
    OpenRequest request = OpenRequest.newBuilder()
        .setObjectHint(typeHint)
        .setObjectId(Converters.toObjId(objectId)).build();
    Iterator<OpenReply> result = stub.open(request);
    if (!result.hasNext()) {
      throw new MissingObjectException(ObjectId.fromString(objectId.name()), typeHint);
    } else {
      return new GrpcClientObjectLoader(result);
    }
  }

  @Override
  public ObjectId insert(int inserterId,int objectType, long length, InputStream in) throws IOException {
    CompletableFuture<ObjectId> result = new CompletableFuture<>();
    StreamObserver<io.insight.jgit.ObjectId> responseObserver = new StreamObserver<io.insight.jgit.ObjectId>() {
      @Override
      public void onNext(io.insight.jgit.ObjectId value) {
        result.complete(Converters.fromObjId(value));
      }

      @Override
      public void onError(Throwable t) {
        result.completeExceptionally(t);
      }

      @Override
      public void onCompleted() {
      }
    };
    StreamObserver<ObjectInsertRequest> requestObserver = asycStub.insert(responseObserver);
    byte[] buf = new byte[BUFFER_SIZE];
    int read;
    while ((read = in.read(buf)) != -1) {
      ObjectInsertRequest req = ObjectInsertRequest.newBuilder()
          .setInserter(Inserter.newBuilder().setId(inserterId).build())
          .setObjectType(objectType)
          .setTotalLength(length)
          .setData(ByteString.copyFrom(buf, 0, read))
          .build();
      requestObserver.onNext(req);
    }
    requestObserver.onCompleted();
    try {
      return result.get();
    } catch (InterruptedException e) {
      requestObserver.onError(e);
      return null;
    } catch (ExecutionException e) {
      throw new IOException("server error", e);
    }
  }

  @Override
  public PackParser newPackParser(RpcObjDatabase odb, InputStream in) throws IOException {
    CompletableFuture<Void> serverFuture = new CompletableFuture<>();
    StreamObserver<Empty> serverObserver = new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {
      }

      @Override
      public void onError(Throwable t) {
        serverFuture.completeExceptionally(t);
      }

      @Override
      public void onCompleted() {
        serverFuture.complete(null);
      }
    };
    StreamObserver<PackParserRequest> observer = asycStub.newParser(serverObserver);

    return new GrpcPackParser(odb, in, observer, serverFuture);
  }

  @Override
  public ObjectInserter newInserter(RpcObjDatabase odb) {
    return new GrpcObjectInserter(odb, stub);
  }
}
