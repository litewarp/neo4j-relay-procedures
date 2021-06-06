package relay;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import static java.lang.String.format;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import static org.neo4j.internal.helpers.collection.MapUtil.stringMap;

public class CreateConnection<T> {

  private String prefix = "some-prefix";

  @Context
  public Log log;

  /**
   * 
   */
  // @Procedure(value = "example.createConnection")
  // @Description("Input a totalCount and list of edges, get a connection")
  public Connection<T> createConnection(
    List<T> data, 
    Integer count, 
    Integer first, 
    String after,
    String before,
    Integer last
    ) {
    List<Edge<T>> edges = new ArrayList<>();
    int ix = 0;
    for (T object : data) {
      edges.add(new DefaultEdge<T>(object, new DefaultConnectionCursor(createCursor(ix++))));
    }
    if (edges.size() == 0) {
      return emptyConnection();
    }

    ConnectionCursor firstPresliceCursor = edges.get(0).getCursor();
    ConnectionCursor lastPresliceCursor = edges.get(edges.size() - 1).getCursor();

    // int afterOffset = getOffsetFromCursor(after, -1);
    // int begin = Math.max(afterOffset, -1) + 1;
    // int beforeOffset = getOffsetFromCursor(before, edges.size());
    // int end = Math.min(beforeOffset, edges.size());

    // if (begin > end) begin = end;

    // edges = edges.subList(begin, end);

    // if (edges.size() == 0) {
    //   return emptyConnection();
    // }

    if (first != null) {
      if (first < 0) {
        throw new InvalidPageSizeException(format("The page size must not be negative: 'first'=%s", first));
      }
      System.out.println((edges.size()));
      edges = edges.subList(last > edges.size() ? 0 : edges.size() - last, edges.size());
    }
    if (last != null) {
      if (last < 0) {
        throw new InvalidPageSizeException(format("The page size must not be negative: 'last'=%s", last));
      }
      edges = edges.subList(last > edges.size() ? 0 : edges.size() - last, edges.size());
    }

    if (edges.isEmpty()) {
      return emptyConnection();
    }

    Edge<T> firstEdge = edges.get(0);
    Edge<T> lastEdge = edges.get(edges.size() - 1);

    PageInfo pageInfo = new DefaultPageInfo(
      firstEdge.getCursor(),
      lastEdge.getCursor(),
      !firstEdge.getCursor().equals(firstPresliceCursor),
      !lastEdge.getCursor().equals(lastPresliceCursor)
    );

    return new DefaultConnection<>(
      edges,
      pageInfo
    );

  }

  private int getOffsetFromCursor(String cursor, int defaultValue) {
      if (cursor == null) {
          return defaultValue;
      }
      byte[] decode;
      try {
          decode = getDecoder().decode(cursor);
      } catch (IllegalArgumentException e) {
          throw new InvalidCursorException(format("The cursor is not in base64 format : '%s'", cursor), e);
      }
      String string = new String(decode, StandardCharsets.UTF_8);
      if (prefix.length() > string.length()) {
          throw new InvalidCursorException(format("The cursor prefix is missing from the cursor : '%s'", cursor));
      }
      try {
          return Integer.parseInt(string.substring(prefix.length()));
      } catch (NumberFormatException nfe) {
          throw new InvalidCursorException(format("The cursor was not created by this class  : '%s'", cursor), nfe);
      }
  }

  private Connection<T> emptyConnection() {
    PageInfo pageInfo = new DefaultPageInfo(null, null, false, false);
    return new DefaultConnection<>(Collections.emptyList(), pageInfo);
  }

  private String createCursor(int offset) {
    byte[] bytes = (prefix + Integer.toString(offset)).getBytes(StandardCharsets.UTF_8);
    return getEncoder().encodeToString(bytes);
  }
}
