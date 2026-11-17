// A18's fixture (test/j2g/unit.ae's test-forname, the Ghidra case study's G2 plan): Class.forName(name, false, loader)
// finds a class without running its initializer, as ghidra.util.classfinder.ClassSearcherTest's forNameSafe asks;
// the initializer runs on the first active use (the constructor called by reflection). Its committed forms Lazy.ae.
package f;

public class Lazy {
    public static boolean dead;

    public interface Super { }

    public static class Side implements Super {
        static {
            dead = true;
        }
        public Side() { }
    }

    public static int probe(ClassLoader loader) throws Exception {
        dead = false;
        Class<?> c = Class.forName("f.Lazy$Side", false, loader);
        boolean before = dead;
        Object o = c.getConstructor().newInstance();
        return (before ? 1 : 0) + (dead ? 2 : 0) + (o instanceof Super ? 4 : 0);
    }

    public static Class<?> eager(ClassLoader loader) throws Exception {
        return Class.forName("f.Lazy$Side", true, loader);
    }
}
