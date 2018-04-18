import io.insight.jgit.server.grpc.GrpcRepoManager;
import io.insight.jgit.server.grpc.GrpcServer;
import io.insigit.jgit.grpc.GrpcClientRepoManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;

import java.io.File;
import java.io.IOException;

public class ClientTest {
  public static void main(String[] args) throws GitAPIException, IOException {
    GrpcClientRepoManager repoManager=new GrpcClientRepoManager("localhost", 10000);
    GrpcServer server = new GrpcServer(10000, new File("/tmp"));
    server.start();

    server.stop();
  }
}
