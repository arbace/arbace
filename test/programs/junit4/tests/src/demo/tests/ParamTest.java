package demo.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

// A runner of @RunWith (Parameterized: its parameters injected into annotated fields, its names from the pattern).
@RunWith(Parameterized.class)
public class ParamTest {
  @Parameters(name = "{index}: {0}+{1}={2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{1, 1, 2}, {2, 3, 5}, {4, 4, 9}});
  }

  @Parameter(0) public int a;
  @Parameter(1) public int b;
  @Parameter(2) public int sum;

  @Test public void adds() { assertEquals(sum, a + b); }
}
