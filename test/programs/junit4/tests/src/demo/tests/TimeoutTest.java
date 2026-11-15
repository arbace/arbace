package demo.tests;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

// @Test(timeout): JUnit runs the method on a thread of its own (FailOnTimeout: a FutureTask in a thread group) and
// fails it with TestTimedOutException when the time is up, interrupting it.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TimeoutTest {
  @Test(timeout = 20000) public void d1InTime() { assertEquals(6, 2 * 3); }

  @Test(timeout = 200) public void d2TooLong() throws InterruptedException { Thread.sleep(30000); }
}
