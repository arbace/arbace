/*
 * @test
 * @summary JLS 8.1.3, 15.9.2: inner, local and anonymous classes, their enclosing instances and
 *          captured variables, qualified this and super, static nested classes
 */
public class Nested {
    String name = "outer";
    class Inner {
        String name = "inner";
        String names() { return name + " " + this.name + " " + Nested.this.name; }
        class Deeper {
            String all() { return names() + " " + Nested.this.name.length(); }
        }
    }
    static class Static {
        static int count;
        Static() { count++; }
    }
    static class Base {
        String who() { return "base"; }
    }
    class Derived extends Base {
        String who() { return "derived of " + super.who() + " in " + name; }
    }
    interface Counter { int next(); }

    Counter counter(int start) {
        int[] box = {start};
        return new Counter() {
            int calls;
            public int next() { calls++; return box[0]++ * 10 + calls; }
        };
    }

    public static void main(String[] args) {
        Nested o = new Nested();
        Nested.Inner in = o.new Inner();
        Nested.Inner.Deeper d = in.new Deeper();
        System.out.println(in.names() + " / " + d.all());
        new Static(); new Static();
        System.out.println("static count " + Static.count);
        System.out.println(o.new Derived().who());
        Counter c = o.counter(5);
        System.out.println(c.next() + " " + c.next() + " " + c.next());
        final int base = 100;
        class Local {
            int v;
            Local(int v) { this.v = v; }
            int plus() { return base + v; }
            Local twice() { return new Local(v * 2); }
        }
        System.out.println(new Local(3).twice().plus());
        Runnable r = new Runnable() {
            public void run() { System.out.println("anonymous " + getClass().getSuperclass().getSimpleName() + " " + base); }
        };
        r.run();
        Object anon = new Object() { public String toString() { return "custom toString"; } };
        System.out.println(anon);
        System.out.println(new Nested() { { name = "initialized in an anonymous subclass"; } }.name);
    }
}
