// the runner's self-test: TestNG, one test of three fails (its verdict must be a failure)
import org.testng.annotations.*;
import static org.testng.Assert.*;
public class NgFail {
    @Test public void a() { assertEquals(1, 1); }
    @Test public void b() { assertEquals("x", "y"); }
    @Test public void c() { }
}
