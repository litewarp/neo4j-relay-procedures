package relay;

import org.junit.Rule;
import org.junit.Test;
// import org.neo4j.driver.v1.*;
// import org.neo4j.graphqdb.factory.GraphDatabaseSettings;
// import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.logging.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertTrue;
// import static org.neo4j.driver.v1.Values.parameters;

public class CreateConnectionTest {
  private CreateConnection<WhoKnows> connection = new CreateConnection();

  @Test
  public void shouldCreateConnection() {

    WhoKnows nick = new WhoKnows("Nick", "yeahyeah@me.com").toObject();

    List<WhoKnows> data = Arrays.asList(nick);
    Integer count = 1;
    Integer first = 10;
    String after = null;
    Integer last = null;
    String before = null;

    Connection<WhoKnows> who = connection.createConnection(data, count, first, after, before, last);

    System.out.println(who);

    Boolean hasNextPage = who.getPageInfo().isHasNextPage();

    assertTrue("Error!", hasNextPage);
  }
}
