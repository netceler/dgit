package io.insigit.jgit.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.insight.jgit.StreamRequest.DataChunk;
import io.insight.jgit.RemoteStreamGrpc;
import io.insight.jgit.StreamReply;
import io.insight.jgit.StreamRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class GrpcClientRemoteStream {

  private static final Logger logger = LoggerFactory.getLogger(GrpcClientRemoteStream.class);

  public static long newRemoteStream(ManagedChannel channel, InputStream inputStream) {
    RemoteStreamGrpc.RemoteStreamStub stub = RemoteStreamGrpc.newStub(channel);
    CompletableFuture<Long> future = new CompletableFuture<>();
    AtomicReference<StreamObserver<StreamRequest>> ref = new AtomicReference<>();
    ref.set(stub.inputStream(new StreamObserver<StreamReply>() {

      @Override
      public void onNext(StreamReply req) {
        try {
          switch (req.getCmd()) {
            case INIT:
              if (!future.isDone())
                future.complete(req.getArg());
              break;
            case READ:
              byte[] buffer = new byte[(int) req.getArg()];
              int read = inputStream.read(buffer);
              ref.get().onNext(
                  StreamRequest.newBuilder().setDataChunk(
                      DataChunk.newBuilder()
                          .setData(ByteString.copyFrom(buffer,0,read))
                          .setRet(read)).build());
              break;
            case SKIP:
              long ret = inputStream.skip(req.getArg());
              ref.get().onNext(
                  StreamRequest.newBuilder().setDataChunk(
                      DataChunk.newBuilder()
                          .clearData()
                          .setRet(ret)).build());
              break;
            case MARK:
              inputStream.mark((int) req.getArg());
              ref.get().onNext(
                  StreamRequest.newBuilder().setDataChunk(
                      DataChunk.newBuilder()
                          .clearData()
                          .clearRet()).build());
              break;
            case RESET:
              inputStream.reset();
              ref.get().onNext(
                  StreamRequest.newBuilder().setDataChunk(
                      DataChunk.newBuilder()
                          .clearData()
                          .clearRet()).build());
              break;
            case AVAIL:
              int avail = inputStream.available();
              ref.get().onNext(
                  StreamRequest.newBuilder().setDataChunk(
                      DataChunk.newBuilder()
                          .clearData()
                          .setRet(avail)).build());
              break;
            case CLOSE:
              inputStream.close();
          }
        } catch (IOException e) {
          ref.get().onError(Status.INTERNAL.
              withDescription(e.getMessage())
              .withCause(e)
              .asRuntimeException()
          );
        }
      }

      @Override
      public void onError(Throwable t) {
        logger.error("remote error", t);
      }

      @Override
      public void onCompleted() { }
    }));
    try {

      ref.get()
          .onNext(StreamRequest.newBuilder()
              .setInit(StreamRequest.InitStream.newBuilder()
                  .setMarkSupport(inputStream.markSupported()))
              .build());
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

}
