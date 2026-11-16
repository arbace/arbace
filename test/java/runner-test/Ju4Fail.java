// the runner's self-test: JUnit 4, a test that throws what it did not expect
import org.junit.Test;
import static org.junit.Assert.*;
public class Ju4Fail {
    @Test public void a() { assertEquals(1L, 1L); }
    @Test(expected = IllegalStateException.class) public void b() { throw new IllegalArgumentException(); }
}
