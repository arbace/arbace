/*
 * @test
 * @summary JLS 8.1.4, 8.1.3, 8.6.7, 9.9, 15.13.2, 15.27.3, 15.9.4: what the Java forms' back ends (the
 *          interpreter and j2g) left open after JC4b (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As
 *          built: JC4c"): jrt's hand-written classes extended by interpreted classes (WeakReference,
 *          SoftReference, AtomicReference, InheritableThreadLocal) and CopyOnWriteArraySet, a
 *          protected static method of the world called through a subclass, the outer instance two
 *          levels out from classes of constructors' prologues, a lambda's bridges in reflection, a
 *          class's own non-public members in reflection, javac's synthetic null check of a
 *          qualified class instance creation, a lambda of an intersection type cast to one of its
 *          interfaces, a method reference to a generic method returning a box of another primitive,
 *          and a lambda of an int method whose body throws
 */
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class BackEnds2 {
    // jrt's hand-written classes, extended (JCS111)
    static class Named extends AtomicReference<String> {
        Named(String s) { super(s); }
        @Override public String toString() { return "Named(" + get() + ")"; }
    }
    static class Weak extends WeakReference<Object> {
        final String tag;
        Weak(Object o, String tag) { super(o); this.tag = tag; }
    }
    static class Soft extends SoftReference<String> {
        Soft(String s) { super(s); }
        @Override public String get() { String s = super.get(); return s == null ? null : s + "!"; }
    }
    static class Inherited extends InheritableThreadLocal<String> {
        @Override protected String childValue(String parent) { return parent + "-child"; }
    }
    static class Cows extends CopyOnWriteArraySet<Integer> {
        int adds;
        @Override public boolean add(Integer i) { adds++; return super.add(i); }
    }

    // a protected static method of the world through a subclass (JCS112)
    static class Task extends RecursiveAction {
        int surplus = -1;
        @Override protected void compute() { surplus = getSurplusQueuedTaskCount() >= 0 ? 0 : 1; }
    }

    // the outer instance two levels out, from classes of two constructors' prologues (JCS113)
    interface Run { String run(); }
    String g() { return "g"; }
    class Inner1 {
        final String s;
        Inner1() {
            this(new Run() {
                class Inner2 {
                    final String r;
                    Inner2() { this(new Run() { public String run() { return m() + g(); } }); }
                    Inner2(Run o) { r = o.run(); }
                }
                public String run() { return new Inner2().r; }
                String m() { return "m"; }
            });
        }
        Inner1(Run o) { s = o.run(); }
    }

    // a lambda's bridges (JCS116)
    interface K<T> { void m(T t); }
    interface M { void m(String s); }
    interface KM extends K<String>, M {}
    interface H { Object h(); }
    interface N extends H { String h(); }
    static String methodsOf(Object o) {
        List<String> l = new ArrayList<>();
        for (Method m : o.getClass().getDeclaredMethods()) {
            StringBuilder sb = new StringBuilder(m.getName()).append('(');
            for (Class<?> p : m.getParameterTypes()) sb.append(p.getSimpleName());
            l.add(sb.append(')').append(m.getReturnType().getSimpleName()).toString());
        }
        Collections.sort(l);
        return l.toString();
    }

    // a class's own members in reflection (JCS117)
    static class Hidden {
        private static int count = 3;
        private int field = 4;
        private Hidden() {}
        private String secret(int i) { return "secret" + i; }
        static String pkg() { return "pkg"; }
        protected int prot() { return 5; }
    }

    // javac's synthetic null check (Objects.requireNonNull) of outer.new Inner() (JCS118)
    class Inside {}
    static String nullCheck(BackEnds2 outer) {
        try {
            outer.new Inside();
            return "no NPE";
        } catch (NullPointerException e) {
            for (StackTraceElement el : e.getStackTrace())
                if (el.getClassName().equals("java.util.Objects") && el.getMethodName().equals("requireNonNull"))
                    return "requireNonNull";
            return "NPE";
        }
    }

    // a lambda of an intersection type, cast to one of its interfaces (JCS118)
    interface IA { void a(); }
    interface IB { String b(); }
    interface IAB extends IA, IB { default void a() {} default String b() { return "B"; } }
    interface IC { void c(); }

    // a method reference to a generic method returning T, as an int (JCS118)
    interface ISi { int m(Short a); }
    static class E<T> { T id(T t) { return t; } }

    // a lambda of an int method whose body throws (JCS118)
    interface Bar { int m(Integer i) throws Exception; }

    public static void main(String[] args) throws Exception {
        Named n = new Named("a");
        n.set("b");
        System.out.println("atomic " + n + " " + n.compareAndSet("b", "c") + " " + n.get());

        Object kept = new Object();
        Weak w = new Weak(kept, "w");
        System.out.println("weak " + w.tag + " " + (w.get() == kept) + " " + w.refersTo(kept));
        w.clear();
        System.out.println("cleared " + (w.get() == null));
        Soft s = new Soft("soft");
        Reference<String> r = s;
        System.out.println("soft " + s.get() + " " + r.get());

        Inherited it = new Inherited();
        it.set("parent");
        String[] seen = new String[1];
        Thread t = new Thread(() -> seen[0] = it.get());
        t.start();
        t.join();
        System.out.println("inherited " + it.get() + " " + seen[0]);

        Cows cows = new Cows();
        cows.add(1); cows.add(2); cows.add(1);
        System.out.println("cows " + cows + " " + cows.adds);

        Task task = new Task();
        ForkJoinPool.commonPool().invoke(task);
        System.out.println("surplus " + task.surplus);

        System.out.println("outer " + new BackEnds2().new Inner1().s);

        StringBuilder calls = new StringBuilder();
        KM km = x -> calls.append(x);
        K<String> k = km;
        M mm = km;
        k.m("k");
        mm.m("m");
        N nn = () -> "h";
        H h = nn;
        System.out.println("bridges " + methodsOf(km) + " " + methodsOf(nn) + " " + calls + " " + h.h());

        Method sec = Hidden.class.getDeclaredMethod("secret", int.class);
        sec.setAccessible(true);
        Field cf = Hidden.class.getDeclaredField("count");
        cf.setAccessible(true);
        Constructor<Hidden> hc = Hidden.class.getDeclaredConstructor();
        hc.setAccessible(true);
        Hidden hidden = hc.newInstance();
        Field ff = Hidden.class.getDeclaredField("field");
        ff.setAccessible(true);
        List<String> names = new ArrayList<>();
        for (Method m : Hidden.class.getDeclaredMethods()) names.add(m.getName() + " " + Modifier.toString(m.getModifiers()));
        Collections.sort(names);
        System.out.println("own " + sec.invoke(hidden, 7) + " " + cf.get(null) + " " + ff.get(hidden) + " " + names);

        System.out.println("null check " + nullCheck(null));

        Runnable ab = ((IA & IB & IAB & IC) () -> {})::b;
        Object x = (IA & IB & IAB & IC) () -> {};
        System.out.println("intersection " + ((IAB) x).b() + " " + (x instanceof IC));
        ab.run();

        ISi si = new E<Short>()::id;
        System.out.println("unbox " + si.m((short) 77));

        Bar bar = i -> { throw new Exception("thrown " + i); };
        try { bar.m(3); } catch (Exception e) { System.out.println("lambda " + e.getMessage()); }
    }
}
