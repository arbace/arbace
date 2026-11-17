// the runner's self-test: TestNG, an expected exception not thrown (a failure)
import org.testng.annotations.*;
public class NgExpected {
    @Test(expectedExceptions = IllegalArgumentException.class) public void a() { }
}
