// the runner's self-test: JUnit 5, a @BeforeAll that throws (a container failure)
import org.junit.jupiter.api.*;
class JuBeforeAllFail {
    @BeforeAll static void all() { throw new IllegalStateException("fails"); }
    @Test void t() { }
}
