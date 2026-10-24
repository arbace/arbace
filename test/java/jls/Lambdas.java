/*
 * @test
 * @summary JLS 15.13, 15.27: lambdas and method references of every kind, captured values,
 *          functional interfaces with default methods, generic functional interfaces
 */
import java.util.*;
import java.util.function.*;

public class Lambdas {
    interface Op { int apply(int a, int b); default Op twice() { return (a, b) -> apply(apply(a, b), b); } }
    String prefix = "p:";
    String tag(String s) { return prefix + s; }
    static String shout(String s) { return s.toUpperCase(); }

    public static void main(String[] args) {
        Op add = (a, b) -> a + b;
        Op mul = (a, b) -> { return a * b; };
        System.out.println(add.apply(2, 3) + " " + mul.apply(2, 3) + " " + add.twice().apply(2, 3) + " " + mul.twice().apply(2, 3));
        Function<String, String> st = Lambdas::shout;
        Lambdas l = new Lambdas();
        Function<String, String> bound = l::tag;
        BiFunction<Lambdas, String, String> unbound = Lambdas::tag;
        Supplier<List<String>> ctor = ArrayList::new;
        IntFunction<int[]> arr = int[]::new;
        List<String> list = ctor.get();
        list.add("b"); list.add("a"); list.add("c");
        list.sort(Comparator.naturalOrder());
        System.out.println(st.apply("x") + " " + bound.apply("y") + " " + unbound.apply(l, "z") + " " + list + " " + arr.apply(4).length);
        int base = 10;
        IntUnaryOperator plus = x -> x + base;
        System.out.println(plus.andThen(x -> x * 2).applyAsInt(1) + " " + plus.compose(x -> x * 2).applyAsInt(1));
        Function<Integer, Function<Integer, Integer>> curried = a -> b -> a - b;
        System.out.println(curried.apply(10).apply(3));
        Runnable[] rs = new Runnable[3];
        for (int i = 0; i < 3; i++) { int j = i; rs[i] = () -> System.out.print("r" + j + " "); }
        for (Runnable r : rs) r.run();
        System.out.println();
        Predicate<String> empty = String::isEmpty;
        System.out.println(empty.negate().test("") + " " + empty.or(s -> s.startsWith("a")).test("abc"));
        BinaryOperator<Integer> max = BinaryOperator.maxBy(Comparator.naturalOrder());
        System.out.println(max.apply(3, 9));
        Optional<String> o = Optional.of("v").map(s -> s + s).filter(s -> s.length() == 2);
        System.out.println(o.orElse("none") + " " + Optional.empty().isPresent());
        Comparator<String> byLen = Comparator.comparingInt(String::length);
        List<String> words = new ArrayList<>(List.of("ccc", "a", "bb", "dd"));
        words.sort(byLen.thenComparing(Comparator.reverseOrder()));
        System.out.println(words);
        Callable c = () -> "callable";
        try { System.out.println(c.call()); } catch (Exception e) { throw new RuntimeException(e); }
    }
    interface Callable { Object call() throws Exception; }
}
