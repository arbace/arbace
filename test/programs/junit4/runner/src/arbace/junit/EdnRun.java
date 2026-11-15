/*
 * Arbace's JUnit 4 runner for a program's tests (A10 of the Ghidra case study's G1 plan, GH7 (b); doc/go/BUILD.md,
 * "Java sources by module"): JUnit's own JUnitCore over each class named on the command line, in order, with a
 * RunListener that prints one EDN map a method to System.out as the method finishes: its class, its method (the
 * Description's name, a parameterized run's index in brackets), its result (:pass, :fail, :assumption,
 * :ignored) and, for a failure, the exception's class and message. A failure of the class itself (a @BeforeClass,
 * a class rule) is a map with :method nil. After each class a summary map. The same runner runs on the JDK and on
 * a program built by Arbace, so the two are compared line for line, each test judged by its own verdict.
 * Usage: EdnRun CLASS...   (status 0 when every class ran, whatever the verdicts; 2 when a class cannot be loaded)
 */
package arbace.junit;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public final class EdnRun {
  private EdnRun() {}

  static String q(String s) {
    if (s == null) return "nil";
    StringBuilder b = new StringBuilder("\"");
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"' || c == '\\') b.append('\\').append(c);
      else if (c == '\n') b.append("\\n");
      else if (c < 32) b.append(' ');
      else b.append(c);
    }
    return b.append('"').toString();
  }

  static final class Listener extends RunListener {
    private final String cls;
    private String result;
    private Throwable failure;

    Listener(String cls) { this.cls = cls; }

    @Override public void testStarted(Description d) { result = ":pass"; failure = null; }

    @Override public void testFailure(Failure f) {
      if (f.getDescription().getMethodName() == null) {
        Throwable t = f.getException();
        System.out.println("{:class " + q(cls) + " :method nil :result :class-failure :failure "
            + q(t.getClass().getName()) + " :message " + q(t.getMessage()) + "}");
        return;
      }
      if (failure == null) { result = ":fail"; failure = f.getException(); }
      else System.out.println("{:class " + q(cls) + " :method " + q(f.getDescription().getMethodName())
          + " :also " + q(f.getException().getClass().getName()) + " :message " + q(f.getException().getMessage()) + "}");
    }

    @Override public void testAssumptionFailure(Failure f) { result = ":assumption"; failure = null; }

    @Override public void testIgnored(Description d) {
      System.out.println("{:class " + q(cls) + " :method " + q(d.getMethodName()) + " :result :ignored}");
    }

    @Override public void testFinished(Description d) {
      System.out.println("{:class " + q(cls) + " :method " + q(d.getMethodName()) + " :result " + result
          + (failure != null ? " :failure " + q(failure.getClass().getName()) + " :message " + q(failure.getMessage()) : "")
          + "}");
    }
  }

  public static void main(String[] args) {
    int status = 0;
    for (String cls : args) {
      Class<?> c;
      try {
        c = Class.forName(cls);
      } catch (Throwable t) {
        System.out.println("{:class " + q(cls) + " :method nil :result :load-error :failure " + q(t.getClass().getName()) + "}");
        status = 2;
        continue;
      }
      JUnitCore core = new JUnitCore();
      core.addListener(new Listener(cls));
      Result r = core.run(c);
      System.out.println("{:class " + q(cls) + " :run " + r.getRunCount() + " :failures " + r.getFailureCount()
          + " :ignored " + r.getIgnoreCount() + " :assumptions " + r.getAssumptionFailureCount() + "}");
    }
    System.exit(status);
  }
}
