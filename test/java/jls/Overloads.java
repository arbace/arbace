/*
 * @test
 * @summary JLS 15.12.2: overload resolution in its phases (widening before boxing before varargs),
 *          the most specific method, static and dynamic dispatch, fields hidden, not overridden
 */
public class Overloads {
    static String m(int x) { return "int"; }
    static String m(long x) { return "long"; }
    static String m(double x) { return "double"; }
    static String m(Integer x) { return "Integer"; }
    static String m(Object x) { return "Object"; }
    static String m(int... x) { return "int..."; }
    static String n(long x) { return "long"; }
    static String n(Integer x) { return "Integer"; }
    static String v(Object o) { return "Object"; }
    static String v(String s) { return "String"; }
    static String w(Object... os) { return "Object... " + (os == null ? "null" : os.length); }
    static String b(byte x) { return "byte"; }
    static String b(short x) { return "short"; }
    static String b(char x) { return "char"; }
    static String b(int x) { return "int"; }

    static class A { String f = "A.f"; String who() { return "A"; } static String s() { return "A.s"; } String call() { return who() + " " + f; } }
    static class B extends A { String f = "B.f"; String who() { return "B"; } static String s() { return "B.s"; } }

    public static void main(String[] args) {
        byte by = 1; short sh = 2; char ch = 'c'; long lo = 3; float fl = 4;
        Integer boxed = 5; Long boxedL = 6L;
        System.out.println(m(1) + " " + m(lo) + " " + m(fl) + " " + m(boxed) + " " + m(boxedL) + " " + m("s") + " " + m() + " " + m(1, 2));
        System.out.println(m(by) + " " + m(sh) + " " + m(ch));
        System.out.println(n(1) + " " + n(boxed));
        System.out.println(v(null) + " " + v("s") + " " + v((Object) "s"));
        System.out.println(w() + "; " + w(1, 2) + "; " + w((Object[]) null) + "; " + w((Object) null) + "; " + w(new String[] {"a", "b", "c"}));
        System.out.println(b(by) + " " + b(sh) + " " + b(ch) + " " + b((byte) (by + 1)) + " " + b(by += 1));
        A a = new B();
        System.out.println(a.who() + " " + a.f + " " + ((B) a).f + " " + a.call() + " " + A.s() + " " + B.s());
        StringBuilder sb = new StringBuilder();
        sb.append('a').append((int) 'a').append(1L).append(1.5f).append((Object) "o").append((CharSequence) "cs").append(new char[] {'x'});
        System.out.println(sb);
        System.out.println(String.valueOf((Object) null) + " " + String.valueOf(new char[] {'h', 'i'}) + " " + Math.max(1, 2L) + " " + Math.abs(-2.0f));
    }
}
