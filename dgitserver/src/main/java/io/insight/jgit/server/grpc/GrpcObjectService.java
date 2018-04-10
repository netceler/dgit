package io.insight.jgit.server.grpc;

import io.grpc.stub.StreamObserver;
import io.insight.jgit.*;
import io.insight.jgit.server.services.ObjectServiceImpl;
import io.insight.jgit.server.utils.ByteBufferBackedInputStream;
import io.insight.jgit.server.utils.FileCachedByteBuffer;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Set;

import static io.insight.jgit.server.grpc.RepoNameServerInterceptor.REPO_CTX_KEY;

public class GrpcObjectService extends ObjectServiceGrpc.ObjectServiceImplBase {
  private ObjectServiceImpl getImpl() {
    Repository repo = REPO_CTX_KEY.get();
    return new ObjectServiceImpl(repo);
  }

  @Override
  public void resolve(ResolveRequest request, StreamObserver<ObjectIdList> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    Collection<ObjectId> result = impl.resolve(AbbreviatedObjectId.fromString(request.getAbbreviatedObjectId().getId()));
    ObjectIdList.Builder builder = toObjectIdList(result);
    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  private ObjectIdList.Builder toObjectIdList(Collection<ObjectId> result) {
    ObjectIdList.Builder builder = ObjectIdList.newBuilder();
    for (ObjectId objectId : result) {
      builder.addObjectIds(io.insight.jgit.ObjectId.newBuilder().setId(objectId.getName()));
    }
    return builder;
  }

  @Override
  public void shallowCommits(Empty request, StreamObserver<ObjectIdList> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    Set<ObjectId> result = impl.getShallowCommits();
    responseObserver.onNext(toObjectIdList(result).build());
    responseObserver.onCompleted();
  }

  @Override
  public StreamObserver<ObjectInsertRequest> insert(StreamObserver<io.insight.jgit.ObjectId> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    return new StreamObserver<ObjectInsertRequest>() {
      FileCachedByteBuffer.ClosableByteBuf buf;
      int length;
      int objectType;
      @Override
      public void onNext(ObjectInsertRequest req) {
        if (buf == null) {
          try {
            this.objectType = req.getObjectType();
            this.length = (int) req.getTotalLength();
            buf = FileCachedByteBuffer.createBuffer(length);
            ByteBuffer bb = buf.nioBuffer();
            bb.position((int) req.getPos());
            req.getData().copyTo(bb);
          } catch (IOException e) {
            responseObserver.onError(e);
          }
        }
      }
      @Override
      public void onError(Throwable t) {
        responseObserver.onError(t);
      }

      @Override
      public void onCompleted() {
        buf.setIndex(0, length);
        InputStream in = new ByteBufferBackedInputStream(buf.nioBuffer());
        try {
          ObjectId result = impl.insert(objectType, length, in);
          responseObserver.onNext(io.insight.jgit.ObjectId.newBuilder().setId(
              result.getName()).build());
        } catch (IOException e) {
          responseObserver.onError(e);
        } finally {
          if (buf != null) {
            try {
              buf.close();
            } catch (Exception e) {
              responseObserver.onError(e);
            }
          }
        }
      }
    };
  }
}
