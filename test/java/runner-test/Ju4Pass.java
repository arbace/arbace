// the runner's self-test: JUnit 4, every test passes
import org.junit.*;
import static org.junit.Assert.*;
public class Ju4Pass {
    static int classes;
    int n;
    @BeforeClass public static void once() { classes++; }
    @Before public void each() { n = 1; }
    @Test public void a() { assertEquals(1, n); assertEquals(1, classes); }
    @Test(expected = IllegalStateException.class) public void b() { throw new IllegalStateException(); }
}
