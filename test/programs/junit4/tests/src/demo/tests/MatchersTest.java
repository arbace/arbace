package demo.tests;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

// hamcrest-core's matchers through assertThat, and a failing match's description (the message is hamcrest's).
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MatchersTest {
  @Test public void c1Core() {
    assertThat(3, is(3));
    assertThat("arbace", allOf(startsWith("arb"), endsWith("ace"), containsString("ba")));
    assertThat("x", anyOf(equalTo("y"), equalTo("x")));
    assertThat(null, nullValue());
    assertThat(Integer.valueOf(1), instanceOf(Number.class));
    assertThat(Arrays.asList("a", "b"), hasItem("b"));
    assertThat(1, not(2));
  }

  @Test public void c2Arrays() { assertArrayEquals(new int[] {1, 2, 3}, new int[] {1, 2, 4}); }

  @Test public void c3Mismatch() { assertThat("the value", "abc", allOf(startsWith("a"), endsWith("x"))); }
}
