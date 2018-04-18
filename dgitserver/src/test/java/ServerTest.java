import io.insight.jgit.server.grpc.GrpcServer;

import java.io.File;
import java.io.IOException;

public class ServerTest {
  public static void main(String[] args) throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(10000, new File("/tmp/grpc"));
    server.start();
    server.join();
  }
}
