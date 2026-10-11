package sample;

public record Square(double side) implements Shape {
  public Square { if (side < 0) throw new IllegalArgumentException("side"); }
}
