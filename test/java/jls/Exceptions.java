/*
 * @test
 * @summary JLS 14.20: try, catch and finally: finally replacing a return or an exception,
 *          try-with-resources closing in reverse order with suppressed exceptions, multi-catch,
 *          rethrow, ExceptionInInitializerError, causes
 */
public class Exceptions {
    static StringBuilder log = new StringBuilder();
    static class Res implements AutoCloseable {
        final String n; final boolean fail;
        Res(String n, boolean fail) { this.n = n; this.fail = fail; log.append("open ").append(n).append(' '); }
        public void close() { log.append("close ").append(n).append(' '); if (fail) throw new IllegalStateException("close " + n); }
    }
    @SuppressWarnings("finally")
    static int finallyWins() { try { return 1; } finally { return 2; } }
    static int finallyRuns() { int x = 1; try { return x; } finally { x = 5; log.append("finally x=5 "); } }
    @SuppressWarnings("finally")
    static String swallow() { try { throw new RuntimeException("lost"); } finally { return "swallowed"; } }
    static class Bad { static int v = 1 / Integer.parseInt("0"); }

    public static void main(String[] args) {
        System.out.println(finallyWins() + " " + finallyRuns() + " " + log + swallow()); log.setLength(0);
        try (Res a = new Res("a", true); Res b = new Res("b", true)) {
            log.append("body ");
            throw new RuntimeException("body");
        } catch (RuntimeException e) {
            System.out.print(log + "| caught " + e.getMessage() + " suppressed");
            for (Throwable s : e.getSuppressed()) System.out.print(" " + s.getMessage());
            System.out.println();
        }
        log.setLength(0);
        try (Res a = new Res("only", true)) {
            log.append("body ");
        } catch (IllegalStateException e) {
            System.out.println(log + "| " + e.getMessage() + " " + e.getSuppressed().length);
        }
        log.setLength(0);
        for (int i = 0; i < 3; i++) {
            try {
                if (i == 0) throw new IllegalArgumentException("iae");
                if (i == 1) throw new ArrayIndexOutOfBoundsException("aioobe");
                Object o = "s"; Integer n = (Integer) o;
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                System.out.println("multi " + e.getClass().getSimpleName() + " " + e.getMessage());
            } catch (ClassCastException e) {
                System.out.println("cce " + (e.getMessage() != null));
            } finally {
                System.out.println("finally " + i);
            }
        }
        try {
            try { int[] a = new int[1]; a[2] = 1; }
            catch (RuntimeException e) { throw new IllegalStateException("wrapped", e); }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage() + " caused by " + e.getCause().getClass().getName());
        }
        try { System.out.println(Bad.v); }
        catch (ExceptionInInitializerError e) { System.out.println("EIIE " + e.getCause().getClass().getSimpleName()); }
        try { System.out.println(Bad.v); }
        catch (NoClassDefFoundError e) { System.out.println("NCDFE"); }
        outer:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                try { if (j == 1) continue outer; if (i == 2) break outer; System.out.print(i + "" + j + " "); }
                finally { System.out.print("f "); }
            }
        }
        System.out.println();
        try { Object o = null; o.toString(); }
        catch (NullPointerException e) { System.out.println("NPE"); }
        try { throw new Error("custom error"); }
        catch (Throwable t) { System.out.println(t); }
    }
}
