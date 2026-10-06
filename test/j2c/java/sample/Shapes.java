package sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The worked example of doc/classes/SPEC.md §11.4. */
public final class Shapes {
  private Shapes() {}
  public static double area(Shape s) {
    return switch (s) {
      case Circle c -> Math.PI * c.r() * c.r();
      case Square(double side) -> side * side;
    };
  }
  public static <T extends Comparable<? super T>> T max(List<? extends T> xs) {
    T best = null;
    for (T x : xs)
      if (best == null || x.compareTo(best) > 0) best = x;
    return best;
  }
  static long nonBlank(Path p) throws IOException {
    try (var lines = Files.lines(p)) {
      return lines.filter(l -> !l.isBlank()).count();
    }
  }
  static int rowOf(int[][] grid, int v) {
    outer:
    for (int i = 0; i < grid.length; i++)
      for (int j = 0; j < grid[i].length; j++) {
        if (grid[i][j] < 0) continue outer;
        if (grid[i][j] == v) return i;
      }
    return -1;
  }
}
