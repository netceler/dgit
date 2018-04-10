import io.atomix.Atomix;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Transport;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.group.DistributedGroup;
import io.atomix.group.GroupMember;
import io.atomix.resource.Resource;

import java.util.concurrent.CompletableFuture;

public class Try {
  public static void main(String[] args) {
    AtomixReplica.Builder builder = AtomixReplica.builder(new Address("localhost", 8700));
    Storage storage=new Storage();
    Transport transport=new NettyTransport();
    AtomixReplica replica = AtomixReplica.builder(new Address("localhost", 8700))
        .withStorage(storage)
        .withTransport(transport)
        .build();

    CompletableFuture<AtomixReplica> future = replica.bootstrap();
     future.join();
    DistributedGroup group = replica.getGroup("test").join();
    group.election().onElection(term -> {
      GroupMember leader = term.leader();
    });

  }
}
