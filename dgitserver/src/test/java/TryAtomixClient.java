import io.atomix.cluster.ClusterEvent;
import io.atomix.cluster.Node;
import io.atomix.core.Atomix;
import io.atomix.core.value.AtomicValue;
import io.atomix.messaging.Endpoint;
import io.atomix.primitive.partition.Partition;

public class TryAtomixClient {
  public static void main(String[] args) {
    Node clientNode = Node.builder("client")
        .withType(Node.Type.CLIENT)
        .withEndpoint(Endpoint.from(5003))
        .build();

    Atomix client = Atomix.builder()
        .withLocalNode(clientNode)
        .withBootstrapNodes(TryAtomix.bootstrapNodes)
        .build();


    client.start().join();
    Partition partition = client.partitionService().getDefaultPartitionGroup().getPartition("test1111");
    System.out.println("partition = " + partition.primary());

    AtomicValue<Integer> v = client.primitivesService().<Integer>atomicValueBuilder("test").build();
    v.set(0);

  }
}
