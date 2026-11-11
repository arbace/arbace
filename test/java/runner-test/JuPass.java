// the runner's self-test: JUnit 5, every test passes (PER_CLASS, TestInfo, MethodSource, ValueSource, CsvSource, an aborted assumption;
// a factory's Object[] rows spread over one parameter, a 2-dimensional array one argument)
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
    Object[][] rows() { return new Object[][] {{1}, {2}}; }
    @ParameterizedTest @MethodSource("rows") void r(int i) { assertTrue(i > 0); }
    static Stream<int[][]> grids() { return Stream.of(new int[][] {{1}}, new int[][] {{2}, {3}}); }
    @ParameterizedTest @MethodSource("grids") void g(int[][] a) { assertTrue(a.length > 0); }
    @ParameterizedTest @ValueSource(ints = {1, 2}) void v(int i) { assertEquals(i, Integer.valueOf(i)); }
    @ParameterizedTest @CsvSource({"1, a", "2, 'b c'"}) void c(long i, String s) {
        assertThrows(IllegalArgumentException.class, () -> { throw new IllegalArgumentException(s); });
    }
    @Test void aborted() { Assumptions.assumeTrue(false, "aborted on purpose"); }
}
