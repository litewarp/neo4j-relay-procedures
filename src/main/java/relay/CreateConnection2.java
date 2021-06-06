package relay;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static java.lang.String.format;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

import org.neo4j.logging.Log;
import org.neo4j.procedure.*;


public class CreateConnection2<T> {

  private String prefix = "some-prefix";

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

  public static class ArraySliceMetaInfo {
    public final Integer sliceStart;
    public final Integer totalCount;

    public ArraySliceMetaInfo(Integer sliceStart, Integer totalCount) {
      this.sliceStart = sliceStart;
      this.totalCount = totalCount;
    }
  }
  /**
   * 
   */
  // @Procedure(value = "example.createConnection")
  // @Description("Input a totalCount and list of edges, get a connection")
  public Connection<T> connectionFromArraySlice(
    List<T> arraySlice, 
    ConnectionArguments args,
    ArraySliceMetaInfo meta
    ) {

    Integer sliceStart = meta.sliceStart;
    Integer sliceEnd = sliceStart + arraySlice.size();
    Integer beforeOffset = cursorToOffset(args.before, meta.totalCount);
    Integer afterOffset = cursorToOffset(args.after, -1);

    Integer startOffset = Math.max(sliceStart -1, afterOffset -1) + 1;
    Integer endOffset = Math.min(sliceEnd, Math.min(beforeOffset, meta.totalCount));

    if (args.first instanceof Integer) {
      if (args.first < 0) {
        throw new InvalidPageSizeException(format("The page size must not be negative: 'first'=%s", args.first));
      }
      endOffset = Math.min(endOffset, startOffset + args.first);
    }

    if (args.last instanceof Integer) {
      if (args.last < 0) {
        throw new InvalidPageSizeException(format("The page size must not be negative: 'last'=%s", args.last));
      }

      startOffset = Math.max(startOffset, endOffset - args.last);
    }

    // if supplied slice is too large, trim it down before mapping over it.
    List<T> slice = arraySlice.subList(
      Math.max(startOffset - sliceStart, 0), 
      arraySlice.size() - (sliceEnd - endOffset)
    );

    List<Edge<T>> edges = new ArrayList<>();
    int ix = 0;
    for (T object : slice) {
      edges.add(new DefaultEdge<T>(object, new DefaultConnectionCursor(offsetToCursor(ix++))));
    }

    Edge<T> firstEdge = edges.get(0);
    Edge<T> lastEdge = edges.get(edges.size() - 1);
    Integer lowerBound = args.after != null ? afterOffset - 1 : 0;
    Integer upperBound = args.before != null ? beforeOffset : meta.totalCount;
    
    ConnectionCursor startCursor = firstEdge != null ? firstEdge.getCursor() : null;
    ConnectionCursor endCursor = lastEdge != null ? lastEdge.getCursor() : null;

    Boolean hasPreviousPage = args.last instanceof Integer ? startOffset > lowerBound : false;
    Boolean hasNextPage = args.first instanceof Integer ? endOffset < upperBound : false;

    PageInfo pageInfo = new DefaultPageInfo(
      startCursor,
      endCursor,
      hasPreviousPage,
      hasNextPage
    );

    return new DefaultConnection<>(
      edges,
      pageInfo
    );

  }

  private int cursorToOffset(String cursor, int defaultValue) {
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

  private String offsetToCursor(int offset) {
    byte[] bytes = (prefix + Integer.toString(offset)).getBytes(StandardCharsets.UTF_8);
    return getEncoder().encodeToString(bytes);
  }
}