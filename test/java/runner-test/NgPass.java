// the runner's self-test (test/java/runner-test.bash): TestNG, every test passes (data provider, expectedExceptions, dependsOnMethods, a skip)
import org.testng.SkipException;
import org.testng.annotations.*;
import static org.testng.Assert.*;
public class NgPass {
    int n;
    @BeforeClass void setup() { n = 41; }
    @DataProvider(name = "d") Object[][] d() { return new Object[][] {{1, "a"}, {2, "b"}}; }
    @Test(dataProvider = "d") public void a(int i, String s) { assertTrue(i > 0); assertEquals(s.length(), 1); }
    @Test(expectedExceptions = IllegalStateException.class) public void b() { throw new IllegalStateException(); }
    @Test(dependsOnMethods = "a") public void c() { assertEquals(n + 1, 42); expectThrows(ArithmeticException.class, () -> { int z = 1 / (n - 41); }); }
    @Test public void e() { throw new SkipException("skipped on purpose"); }
}
