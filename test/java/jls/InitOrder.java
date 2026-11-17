/*
 * @test
 * @summary JLS 12.4, 12.5: class initialization when first used, superclass first; instance
 *          creation: the super constructor (calling an overridden method that sees the defaults),
 *          then field initializers and instance initializers in text order, then the body;
 *          15.10.2, 15.8.2: neither an array creation nor a class literal initializes the class
 */
public class InitOrder {
    static StringBuilder log = new StringBuilder();
    static void p(String s) { log.append(s).append(' '); }

    static class A {
        static { p("A.static"); }
        int a = init("A.a");
        { p("A.init"); }
        A() { p("A()"); show(); }
        void show() { p("A.show"); }
        static int init(String s) { p(s); return 1; }
    }
    static class B extends A {
        static { p("B.static"); }
        int b = init("B.b");
        String s = "set";
        { p("B.init"); }
        B() { super(); p("B() b=" + b); }
        @Override void show() { p("B.show b=" + b + " s=" + s); }
    }
    static class Lazy {
        static { p("Lazy.static"); }
        static final int CONST = 7;     // a constant: no initialization
        static int value = 8;
    }
    interface I {
        int X = InitOrder.mark("I.X");
    }
    static class C implements I {
        static { p("C.static"); }
    }
    static class Unused {
        static { p("Unused.static"); }
        static void touch() { }
    }
    static int mark(String s) { p(s); return 3; }

    public static void main(String[] args) {
        new B();
        System.out.println(log); log.setLength(0);
        new B();
        System.out.println(log); log.setLength(0);
        int k = Lazy.CONST;
        System.out.println("const " + k + ": " + log); log.setLength(0);
        k = Lazy.value;
        System.out.println("value " + k + ": " + log); log.setLength(0);
        new C();
        System.out.println("C: " + log); log.setLength(0);
        k = I.X;
        System.out.println("I.X " + k + ": " + log); log.setLength(0);
        Object[] arr = new Unused[2];
        Object[][] grid = new Unused[2][3];
        System.out.println("arrays of Unused: " + arr.length + " " + grid[1].length + " [" + log + "]"); log.setLength(0);
        Class<?> c = Unused.class;
        System.out.println("Unused.class: " + (arr.getClass().getComponentType() == c) + " [" + log + "]"); log.setLength(0);
        Unused.touch();
        System.out.println("Unused.touch: " + log);
    }
}
