// the runner's self-test: TestNG, a @BeforeClass that throws (a configuration failure: its tests skipped, the run a failure)
import org.testng.annotations.*;
public class NgConfigFail {
    @BeforeClass public void setup() { throw new IllegalStateException("setup fails"); }
    @Test public void a() { }
}
