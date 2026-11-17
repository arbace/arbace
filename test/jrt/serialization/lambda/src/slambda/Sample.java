package slambda;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.IntSupplier;

/** JCS169's night check (check.ae): serializable lambdas and method references of an interpreted class. */
public class Sample {
    int base = 100;
    String name = "s";

    public interface F extends Serializable { int get(); }

    public static IntSupplier statik(int x) { return (IntSupplier & Serializable) () -> x + 1; }
    public F instance(int y) { return () -> base + y; }
    public static Function<String, Integer> ref() { return (Function<String, Integer> & Serializable) String::length; }
    public Function<String, String> bound() { return (Function<String, String> & Serializable) name::concat; }
    public static Comparator<String> comparator() { return Comparator.comparing(String::length); }
    public static Runnable plain() { return () -> {}; }
}
