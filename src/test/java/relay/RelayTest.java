package relay;

import org.junit.Rule;
import org.junit.Test;
// import org.neo4j.driver.v1.*;
// import org.neo4j.graphqdb.factory.GraphDatabaseSettings;
// import org.neo4j.harness.junit.Neo4jRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
// import static org.neo4j.driver.v1.Values.parameters;

public class RelayTest {
  private Relay relay = new Relay();

  @Test
  public void relaySaysHello() {
    assertThat(relay.sayHello(), containsString("Relay!"));
  }
}
