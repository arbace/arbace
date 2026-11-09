// the runner's self-test: JUnit 5, one invocation of a parameterized test fails
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class JuFail {
    @ParameterizedTest @ValueSource(ints = {1, 2, 3}) void v(int i) { assertNotEquals(2, i); }
}
