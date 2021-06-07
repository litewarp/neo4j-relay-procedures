package relay;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.internal.helpers.Exceptions;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.*;
import org.neo4j.driver.Result;
import org.neo4j.harness.*;
import org.neo4j.harness.junit.rule.Neo4jRule;

import static org.junit.Assert.assertFalse;
// import static org.neo4j.driver.v1.Values.parameters;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CreateConnectionTest {

  @Rule
  public Neo4jRule neo4j = new Neo4jRule().withProcedure(CreateConnection.class);

  // @BeforeAll
  // void initializeNeo4j() {
  //   this.embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder().withDisabledServer().withProcedure(CreateConnection.class).build();

  //   driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(), driverConfig);
  // }

  // @AfterAll
  // void closeDriver() {
  //   driver.close();
  //   this.embeddedDatabaseServer.close();
  // }

  // @AfterEach
  // void cleanDb() {
  //   try(Session session = driver.session()) {
  //     session.run("MATCH (n) DETACH DELETE n");
  //   }
  // }

  @Test
  public void shouldCreateConnection() {
    try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.builder().withoutEncryption().build())) {

      Session session = driver.session();

      session.run(String.format("Create (p:Person { name: 'Nick Nick' } ) RETURN p"));
      session.run(String.format("Create (p:Person { name: 'John John' } ) RETURN p"));
      session.run(String.format("Create (p:Person { name: 'Sam Sam' } ) RETURN p"));


      Result record = session.run(
        "MATCH (p:Person)\n"
        .concat("WITH COLLECT (p) as people, COUNT(p) as total\n")
        .concat("CALL relay.createConnection(people, { first: 10, after: null }, total)\n")
        .concat("YIELD edges, pageInfo\n")
        .concat("RETURN edges, pageInfo")
      );
      System.out.println(record.list());
      driver.close();
    }
  }

}
