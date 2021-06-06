package relay;

public class WhoKnows {
  private final String name;
  private final String email;

  public WhoKnows(String name, String email) {
    this.email = email;
    this.name = name;
  }

  public WhoKnows toObject() {
    WhoKnows demo = new WhoKnows(name, email);
    return demo;
  }
}
