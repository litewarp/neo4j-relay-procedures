package relay;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
// import org.neo4j.driver.v1.*;
// import org.neo4j.graphqdb.factory.GraphDatabaseSettings;
// import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.logging.Log;
import org.neo4j.test.mockito.mock.Properties;
import org.neo4j.test.mockito.mock.Property;

import relay.CreateConnection.ArraySliceMetaInfo;
import relay.CreateConnection.ConnectionArguments;
import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.neo4j.test.mockito.mock.GraphMock.*; 
import static org.neo4j.test.mockito.mock.Properties.properties;
// import static org.neo4j.driver.v1.Values.parameters;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CreateConnectionTest {
  private CreateConnection connection = new CreateConnection();

  @Test
  public void shouldCreateConnection() {
    Properties properties = properties();

    long id = new Long(0);

    Node nick = node(id, properties, "Person");
    ArraySliceMetaInfo meta = connection.createMetaInfo(0, 1); 

    ConnectionArguments args = connection.createConnectionArguments(
      10, 
      null, 
      null, 
      null
    );

    List<Node> data = Arrays.asList(nick);

    Connection<Node> who = connection.connectionFromArraySlice(data, args, meta);

    System.out.println(who);

    Boolean hasNextPage = who.getPageInfo().isHasNextPage();

    assertFalse("Error!", hasNextPage);
  }

}
