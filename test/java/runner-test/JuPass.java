// the runner's self-test: JUnit 5, every test passes (PER_CLASS, TestInfo, MethodSource, ValueSource, CsvSource, an aborted assumption)
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JuPass {
    int n;
    @BeforeAll void all() { n = 3; }
    @Test void t1(TestInfo info) { assertEquals("t1()", info.getDisplayName()); }
    static Stream<Arguments> src() { return Stream.of(Arguments.of(1, "x"), Arguments.of(2, "y")); }
    @ParameterizedTest @MethodSource("src") void p(int i, String s) { assertNotNull(s); assertTrue(i < n); }
    @ParameterizedTest @ValueSource(ints = {1, 2}) void v(int i) { assertEquals(i, Integer.valueOf(i)); }
    @ParameterizedTest @CsvSource({"1, a", "2, 'b c'"}) void c(long i, String s) {
        assertThrows(IllegalArgumentException.class, () -> { throw new IllegalArgumentException(s); });
    }
    @Test void aborted() { Assumptions.assumeTrue(false, "aborted on purpose"); }
}
