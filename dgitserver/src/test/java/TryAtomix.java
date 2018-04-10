import io.atomix.cluster.ClusterEvent;
import io.atomix.cluster.Node;
import io.atomix.core.Atomix;
import io.atomix.messaging.Endpoint;
import io.atomix.primitive.partition.Partition;

public class TryAtomix {

  public static Node serverNode1 = Node.builder("server1")
      .withType(Node.Type.DATA)
      .withEndpoint(Endpoint.from(5000))
      .build();
  public static Node serverNode2 = Node.builder("server2")
      .withType(Node.Type.DATA)
      .withEndpoint(Endpoint.from(5001))
      .build();
  public static Node serverNode3 = Node.builder("server3")
      .withType(Node.Type.DATA)
      .withEndpoint(Endpoint.from(5002))
      .build();
  public static Node[] bootstrapNodes = {serverNode1, serverNode2, serverNode3};

  public static void main(String[] args) {
    Atomix server1 = Atomix.builder().withLocalNode(serverNode1).withBootstrapNodes(bootstrapNodes).build();
    Atomix server2 = Atomix.builder().withLocalNode(serverNode2).withBootstrapNodes(bootstrapNodes).build();
    Atomix server3 = Atomix.builder().withLocalNode(serverNode3).withBootstrapNodes(bootstrapNodes).build();

    server1.clusterService().addListener(event -> {
      System.out.println("event = " + event);
      
    });

    server1.start();
    server2.start();
    server3.start().join();
  }
}
