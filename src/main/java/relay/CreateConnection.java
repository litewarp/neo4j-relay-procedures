package relay;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

import org.neo4j.logging.Log;
import org.neo4j.procedure.*;


public class CreateConnection {

  private static String prefix = "some-prefix";

  @Context
  public Log log;

  public static class ConnectionArguments {
    public final String after;
    public final String before;
    public final Integer first;
    public final Integer last;

    public ConnectionArguments(Integer first, Integer last, String after, String before) {
      this.first = first;
      this.last = last;
      this.after = after;
      this.before = before;
    }
  }

  public ConnectionArguments createConnectionArguments(
    Integer first,
    Integer last,
    String after,
    String before
  ) {
    return new ConnectionArguments(first, last, after, before);
  }

  public static class ArraySliceMetaInfo {
    public final Integer sliceStart;
    public final Integer totalCount;

    public ArraySliceMetaInfo(Integer sliceStart, Integer totalCount) {
      this.sliceStart = sliceStart;
      this.totalCount = totalCount;
    }
  }

  public ArraySliceMetaInfo createMetaInfo(
    Integer sliceStart,
    Integer totalCount
  ) {
    return new ArraySliceMetaInfo(sliceStart, totalCount);
  }

  public static class ConnectionOutput {
    public Map<String, Object> pageInfo;
    public List<Map<String, Object>> edges;

    public ConnectionOutput(Map<String, Object> connection) {
      this.pageInfo = (Map<String, Object>) connection.get("pageInfo");
      this.edges = (List<Map<String, Object>>) connection.get("edges");
    }
  }

  public static class OffsetOutput {
    public Number offset;

    public OffsetOutput(Number offset) {
      this.offset = offset;
    }
  }
  /**
   * 
   */
  @Procedure(value = "relay.createConnection")
  @Description("Input a totalCount and list of edges, get a connection")
  public Stream<ConnectionOutput> createConnection(
    @Name("nodes") List<Map<String, Object>> arraySlice, 
    @Name("arguments") Map<String,Object> arguments,
    @Name("totalCount") Number count
  ) {

    Number first = (Number) arguments.get("first");
    Number last = (Number) arguments.get("last");
    String after = (String) arguments.get("after");
    String before = (String) arguments.get("before");

    ConnectionArguments args = new ConnectionArguments(
      first != null ? first.intValue() : null, 
      last != null ? last.intValue() : null, 
      after, 
      before
    );
    ArraySliceMetaInfo meta = new ArraySliceMetaInfo(
      cursorToOffset(after, 0),
      count != null ? count.intValue() : 0
    );

    return Stream.of(
      new ConnectionOutput(
        connectionFromArraySlice(arraySlice, args, meta)
      )
    );
  }

  public Map<String, Object> connectionFromArraySlice(
    List<Map<String, Object>> arraySlice, 
    ConnectionArguments args,
    ArraySliceMetaInfo meta
    ) {
    
    Integer sliceStart = meta.sliceStart;
    Integer sliceEnd = sliceStart + arraySlice.size();
    Integer beforeOffset = cursorToOffset(args.before, meta.totalCount);
    Integer afterOffset = cursorToOffset(args.after, -1);

    Integer startOffset = Math.max(sliceStart - 1, afterOffset - 1) + 1;
    Integer endOffset = Math.min(sliceEnd, Math.min(beforeOffset, meta.totalCount));

    if (args.first != null && args.first instanceof Integer) {
      if (args.first < 0) {
        throw new InvalidPageSizeException(format("The page size must not be negative: 'first'=%s", args.first));
      }
      endOffset = Math.min(endOffset, startOffset + args.first);
    }

    if (args.last != null && args.last instanceof Integer) {
      if (args.last < 0) {
        throw new InvalidPageSizeException(format("The page size must not be negative: 'last'=%s", args.last));
      }

      startOffset = Math.max(startOffset, endOffset - args.last);
    }

    System.out.println("Size");
    System.out.println(arraySlice.size());
    System.out.println("Slice Start and End");
    System.out.println(sliceStart);
    System.out.println(sliceEnd);
    System.out.println("Offset Start and End");
    System.out.println(startOffset);
    System.out.println(endOffset);
    // if supplied slice is too large, trim it down before mapping over it.
    List<Map<String, Object>> slice = arraySlice.subList(
      Math.max(startOffset - sliceStart, 0), 
      arraySlice.size() - (sliceEnd - endOffset)
    );

    List<Map<String, Object>> edges = new ArrayList<>();
    int ix = 0;
    for (Map<String, Object> object : slice) {
      edges.add(
        Map.ofEntries(
          Map.entry("cursor", offsetToCursor(ix++)),
          Map.entry("node", object)
        )
      );
    }

    Map<String, Object> firstEdge = edges.get(0);
    Map<String, Object> lastEdge = edges.get(edges.size() - 1);
    Integer lowerBound = args.after != null ? afterOffset - 1 : 0;
    Integer upperBound = args.before != null ? beforeOffset : meta.totalCount;
    
    String startCursor = firstEdge != null ? (String) firstEdge.get("cursor") : null;
    String endCursor = lastEdge != null ? (String) lastEdge.get("cursor") : null;

    Boolean hasPreviousPage = args.last instanceof Integer ? startOffset > lowerBound : false;
    Boolean hasNextPage = args.first instanceof Integer ? endOffset < upperBound : false;

    Map<String, Object> pageInfo = Map.ofEntries(
      Map.entry("startCursor", startCursor),
      Map.entry("endCursor", endCursor),
      Map.entry("hasPreviousPage", hasPreviousPage),
      Map.entry("hasNextPage", hasNextPage)
    );

    Map<String, Object> connection = Map.ofEntries(
      Map.entry("edges", edges),
      Map.entry("pageInfo", pageInfo)
    );
    return connection;
  }

  public int cursorToOffset(String cursor, int defaultValue) {
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

  @Procedure(value = "relay.cursorToOffset")
  @Description("Enter a cursor and get an offset")
  public Stream<OffsetOutput> getOffset (@Name("cursor") String cursor) {
    Number offset = cursorToOffset(cursor, 0);
    return Stream.of(new OffsetOutput(offset));
  }

  public String offsetToCursor(int offset) {
    byte[] bytes = (prefix + Integer.toString(offset)).getBytes(StandardCharsets.UTF_8);
    return getEncoder().encodeToString(bytes);
  }
}
