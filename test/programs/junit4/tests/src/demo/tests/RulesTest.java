package demo.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;

// JUnit's rules, found by reflection on annotated fields (as AbstractGenericTest's RuleChain, TestName and
// TestWatcher are): a class rule, a chain's order, the method's name, a temporary folder, expected exceptions,
// an error collector's two failures, a watcher's verdicts.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RulesTest {
  static ExternalResource resource(final String name) {
    return new ExternalResource() {
      @Override protected void before() { System.out.println("rules: " + name + " before"); }
      @Override protected void after() { System.out.println("rules: " + name + " after"); }
    };
  }

  @ClassRule public static ExternalResource classResource = resource("class");

  public TestName name = new TestName();
  public TemporaryFolder folder = new TemporaryFolder();
  public ExpectedException thrown = ExpectedException.none();
  public ErrorCollector collector = new ErrorCollector();
  public TestWatcher watcher = new TestWatcher() {
    @Override protected void succeeded(Description d) { System.out.println("rules: watcher: " + d.getMethodName() + " succeeded"); }
    @Override protected void failed(Throwable e, Description d) {
      System.out.println("rules: watcher: " + d.getMethodName() + " failed with " + e.getClass().getSimpleName());
    }
  };

  @Rule public TestRule chain = RuleChain.outerRule(resource("outer")).around(watcher).around(name).around(folder)
      .around(thrown).around(collector);

  @Test public void b1Name() { assertEquals("b1Name", name.getMethodName()); }

  @Test public void b2Folder() throws Exception {
    File f = folder.newFile("data.txt");
    assertTrue(f.exists());
    assertEquals("data.txt", f.getName());
    assertEquals(folder.getRoot(), f.getParentFile());
  }

  @Test public void b3ExpectedMessage() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage(containsString("not here"));
    throw new UnsupportedOperationException("the operation is not here");
  }

  @Test public void b4ExpectedMissing() { thrown.expect(UnsupportedOperationException.class); }

  @Test public void b5Collector() {
    collector.checkThat("one", 1, equalTo(2));
    collector.checkThat("two", "a", equalTo("a"));
    collector.addError(new IllegalStateException("collected"));
  }
}
