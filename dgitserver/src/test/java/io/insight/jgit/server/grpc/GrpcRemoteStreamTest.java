package io.insight.jgit.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.insigit.jgit.grpc.GrpcClientRemoteStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class GrpcRemoteStreamTest {


  private Server server;
  public GrpcRemoteStream service;
  private GrpcClientRemoteStream client;
  private ManagedChannel channel;

  @Before
  public void setUp() throws Exception {
    service = new GrpcRemoteStream();
    server = ServerBuilder.forPort(12345)
        .addService(service)
        .build();
    server.start();
    channel = ManagedChannelBuilder.forAddress("localhost", 12345)
        .usePlaintext()
        .build();


  }

  @After
  public void tearDown() throws Exception {
    server.shutdownNow();
  }

  @Test
  public void remoteStream() throws IOException {
    byte[] bytes = new byte[]{0, 1, 2, 3, 4};
    InputStream localStream = new ByteArrayInputStream(bytes);
    long id = GrpcClientRemoteStream.newRemoteStream(channel,localStream);
    assertEquals(0, id);
    InputStream remoteStream = service.get(id);
    assertNotNull(remoteStream);
    assertEquals(localStream.available(), remoteStream.available());
    assertEquals(localStream.markSupported(), remoteStream.markSupported());
    byte[] buf =new byte[2];
    int read = remoteStream.read(buf);
    assertEquals(2, read);
    assertEquals(0, buf[0]);
    assertEquals(1, buf[1]);
    remoteStream.mark(2);
    byte[] buf2 =new byte[20];
    read = remoteStream.read(buf2);
    assertEquals(3, read);
    assertEquals(2, buf2[0]);
    assertEquals(3, buf2[1]);
    remoteStream.reset();
    int b = remoteStream.read();
    assertEquals(2, b);
    remoteStream.close();
    assertNull(service.get(id));
    
  }
}