package demo.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

// JUnit's lifecycle and verdicts: the class and method fixtures in order, a pass, an assertion's failure, an error,
// an expected exception thrown and not thrown, an assumption, an ignored method.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicsTest {
  static int before;

  @BeforeClass public static void setUpClass() { System.out.println("basics: before class"); }
  @AfterClass public static void tearDownClass() { System.out.println("basics: after class, " + before + " methods set up"); }
  @Before public void setUp() { before++; }
  @After public void tearDown() { }

  @Test public void a1Passes() { assertEquals(4, 2 + 2); }
  @Test public void a2Fails() { assertEquals("expected", "actual"); }
  @Test public void a3Errs() { throw new IllegalArgumentException("thrown by the test"); }
  @Test(expected = IllegalStateException.class) public void a4ExpectedThrown() { throw new IllegalStateException(); }
  @Test(expected = IllegalStateException.class) public void a5ExpectedNotThrown() { }
  @Test public void a6Assumption() { assumeTrue("not here", false); }
  @Ignore("by design") @Test public void a7Ignored() { assertTrue(false); }
}
