package relay;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

public class RelayHelpers<T> {
  public static class ResolvedGlobalId {
    public ResolvedGlobalId(String type, String id) {
        this.type = type;
        this.id = id;
    }

    private final String type;
    private final String id;

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }
  }


  private static final java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding(); 
  private static final java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder(); 

  public String toGlobalId(String type, String id) {
    return encoder.encodeToString((type + ":" + id).getBytes(StandardCharsets.UTF_8));
  }

  public ResolvedGlobalId fromGlobalId(String globalId) {
    String[] split = new String(decoder.decode(globalId), StandardCharsets.UTF_8).split(":", 2);
    if (split.length != 2) {
        throw new IllegalArgumentException(String.format("expecting a valid global id, got %s", globalId));
    }
    return new ResolvedGlobalId(split[0], split[1]);
  }


  private List<Edge<T>> buildEdges(List<T> data) {
    List<Edge<T>> edges = new ArrayList<>();
    int ix = 0;
    String cursor = "simple-cursor";
    for (T object : data) {
      edges.add(new DefaultEdge<>(object, new DefaultConnectionCursor(createCursor(ix++, cursor))));
    }
    return edges;
  }

  private String createCursor(int offset, String prefix) {
      byte[] bytes = (prefix + Integer.toString(offset)).getBytes(StandardCharsets.UTF_8);
      return getEncoder().encodeToString(bytes);
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
    String prefix = "simple-cursor";
    if (prefix.length() > string.length()) {
        throw new InvalidCursorException(format("The cursor prefix is missing from the cursor : '%s'", cursor));
    }
    try {
        return Integer.parseInt(string.substring(prefix.length()));
    } catch (NumberFormatException nfe) {
        throw new InvalidCursorException(format("The cursor was not created by this class  : '%s'", cursor), nfe);
    }
}

}
