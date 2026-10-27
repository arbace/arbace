/*
 * @test
 * @summary JLS 8.1.4, 8.4.8, 9.4.1.3, 14.30.2, 15.12.4.4, 14.11.1, 6.4.1: what the Java forms' back ends
 *          (the interpreter and j2g) left open before JC4b (doc/classes/JAVA-CONFORMANCE-SURVEY.md,
 *          "As built: JC4b"): classes of the world extended by interpreted classes (Thread, ArrayList,
 *          HashMap, LinkedHashMap, ByteArrayInputStream, StringWriter, AssertionError), the most
 *          specific default method inherited through a superclass, a member class of an anonymous
 *          class shadowing a local class of the same name, a nested record pattern over a null
 *          component, a bridge of a generic method whose bounds are reordered, Outer.super.m() from
 *          an inner class, constant labels in a pattern switch over a box, the protected members
 *          of an extended class (a method, fields, a covariant override and its bridge), a
 *          synchronized method, a static method inherited from the world (Thread.sleep), a bridge
 *          to the world's method (LinkedHashSet.reversed), and the initialization of a
 *          superinterface that declares a default method (JLS 12.4.2)
 */
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BackEnds {
    // classes of the world, extended
    static class Worker extends Thread {
        int result;
        Worker(String name) { super(name); }
        @Override public void run() { for (int i = 1; i <= 10; i++) result += i; }
    }
    static class CountingList extends ArrayList<String> {
        int adds;
        @Override public boolean add(String s) { adds++; return super.add(s.toUpperCase()); }
    }
    static class DefaultMap extends HashMap<String, Integer> {
        @Override public Integer get(Object k) { Integer v = super.get(k); return v == null ? -1 : v; }
    }
    static class Lru extends LinkedHashMap<Integer, Integer> {
        Lru() { super(4, 0.75f, true); }
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, Integer> e) { return size() > 2; }
    }
    static class Upper extends ByteArrayInputStream {
        Upper(byte[] b) { super(b); }
        @Override public int read() { int c = super.read(); return c < 0 ? c : Character.toUpperCase(c); }
    }
    static class Tagged extends StringWriter {
        @Override public void write(String s) { super.write("[" + s + "]"); }
    }
    static class Failed extends AssertionError {
        Failed(String m) { super("failed: " + m); }
    }

    // protected members of an extended class of the world
    static class Flag extends Observable { void raise() { setChanged(); } }
    static class Left extends ByteArrayInputStream {
        Left(byte[] b) { super(b); }
        int left() { return count - pos; }
    }
    static class Plus extends FilterOutputStream {
        Plus(OutputStream o) { super(o); }
        @Override public void write(int b) throws IOException { out.write(b + 1); }
    }
    static class Named extends ThreadLocal<String> {
        @Override protected String initialValue() { return "initial"; }
    }
    // a synchronized method (JLS 8.4.3.6), Thread.sleep through the subclass
    static class Counter {
        int n;
        synchronized void inc() { int v = n; Thread.yield(); n = v + 1; }
    }
    static class Incrementer extends Thread {
        final Counter c;
        Incrementer(Counter c) { this.c = c; }
        @Override public void run() {
            for (int i = 0; i < 200; i++) { c.inc(); if (i == 100) { try { sleep(1); } catch (InterruptedException e) { } } }
        }
    }
    // javac's bridge to an inherited method of the world (reversed() of SequencedSet)
    static class Ordered extends LinkedHashSet<String> {}
    // JLS 12.4.2: a class's initialization initializes its superinterfaces that declare a default method
    static final List<String> inits = new ArrayList<>();
    static String init(String s) { inits.add(s); return s; }
    interface J { String J0 = init("J"); default String j() { return J0; } }
    interface K { String K0 = init("K"); }
    static class JK implements J, K { static { init("JK"); } }

    // JLS 8.4.8, 9.4.1.3: B inherits I2.m, the more specific default, though A gives I1.m
    interface I1 { default int m() { return 1; } }
    interface I2 extends I1 { default int m() { return 2; } }
    static abstract class A implements I1 {}
    static class B extends A implements I2 {}

    // JLS 6.4.1: the anonymous class's member Local shadows the method's local class Local
    boolean x1, x2;
    void locals() {
        class Local { { x1 = true; } }
        new Local();
        new Object() {
            class Local { { x2 = true; } }
            { new Local(); }
        };
    }

    // JLS 14.30.2: a record pattern does not match null
    record R4(Object o) {}
    record R5(R4 r) {}
    static int nested(R5 r) {
        return switch (r) {
            case R5(R4(String s)) -> 1;
            case R5(R4(Object o)) -> 2;
            case R5(Object o) -> 3;
        };
    }

    // JLS 8.4.8.1, 15.12.4.4: an override whose type variable's bounds are reordered, and its bridge
    static class Parent {
        public <T extends Appendable & Serializable> String name(T t) { return "Parent"; }
    }
    static class Reordered extends Parent {
        @Override public <T extends Serializable & Appendable> String name(T t) { return "Reordered"; }
    }

    // JLS 15.12.1: Outer.super.m() from an inner class
    static class Base { String who() { return "Base"; } }
    class Outer extends Base {
        @Override String who() { return "Outer"; }
        class Inner { String up() { return Outer.super.who() + "<" + Outer.this.who(); } }
    }

    // JLS 14.11.1: constant labels in a pattern switch over a box
    static String label(Integer i) {
        return switch (i) {
            case null -> "null";
            case 1 -> "one";
            case 2 -> "two";
            case Integer j when j > 10 -> "big";
            case Integer j -> "other " + j;
        };
    }
    static String charLabel(Character c) {
        return switch (c) {
            case 'a' -> "a";
            case Character d -> "char " + d;
        };
    }

    public static void main(String[] args) throws Exception {
        Worker w = new Worker("worker");
        w.start();
        w.join();
        System.out.println(w.getName() + " " + w.result + " " + (w instanceof Thread));

        CountingList l = new CountingList();
        l.add("a"); l.add("b");
        Collections.addAll(l, "c");
        System.out.println(l + " " + l.adds + " " + l.size());

        DefaultMap m = new DefaultMap();
        m.put("x", 1);
        System.out.println(m.get("x") + " " + m.get("y") + " " + m.getOrDefault("y", 7));

        Lru lru = new Lru();
        lru.put(1, 1); lru.put(2, 2); lru.get(1); lru.put(3, 3);
        System.out.println(lru.keySet());

        Upper u = new Upper("abc".getBytes());
        StringBuilder sb = new StringBuilder();
        for (int c; (c = u.read()) >= 0; ) sb.append((char) c);
        System.out.println(sb);

        Tagged t = new Tagged();
        t.write("x"); t.append('y');
        System.out.println(t);

        try { throw new Failed("x"); }
        catch (AssertionError e) { System.out.println(e.getMessage() + " " + (e instanceof Failed)); }

        A a = new B();
        System.out.println("default " + a.m());

        BackEnds be = new BackEnds();
        be.locals();
        System.out.println("locals " + be.x1 + " " + be.x2);

        System.out.println("records " + nested(new R5(new R4("s"))) + " " + nested(new R5(new R4(1)))
                           + " " + nested(new R5(new R4(null))) + " " + nested(new R5(null)));

        Parent p = new Reordered();
        System.out.println("bridge " + p.name(new StringBuilder()));

        Outer o = be.new Outer();
        System.out.println("super " + o.new Inner().up());

        Flag fl = new Flag();
        fl.raise();
        Left lf = new Left("hello".getBytes());
        lf.read();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        new Plus(bo).write('A');
        System.out.println("protected " + fl.hasChanged() + " " + lf.left() + " " + bo + " " + new Named().get());

        Counter c = new Counter();
        Incrementer[] ts = { new Incrementer(c), new Incrementer(c), new Incrementer(c) };
        for (Incrementer x : ts) x.start();
        for (Incrementer x : ts) x.join();
        System.out.println("synchronized " + c.n);

        Ordered os = new Ordered();
        os.add("a"); os.add("b");
        SequencedSet<String> rs = os.reversed();
        System.out.println("reversed " + rs);

        new JK();
        System.out.println("init " + inits);

        System.out.println("labels " + label(null) + " " + label(1) + " " + label(2) + " " + label(11) + " "
                           + label(5) + " " + charLabel('a') + " " + charLabel('b'));
    }
}
