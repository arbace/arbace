/*
 * @test
 * @summary JLS 4.6, 8.4.8.3, 15.12.4.5: generics erased, bridge methods dispatching to the
 *          override, covariant returns, bounded type parameters, generic methods inferred
 */
import java.util.*;

public class Generics {
    interface Box<T> { T get(); Box<T> self(); }
    static class IntBox implements Box<Integer>, Comparable<IntBox> {
        final int v;
        IntBox(int v) { this.v = v; }
        public Integer get() { return v; }
        public IntBox self() { return this; }
        public int compareTo(IntBox o) { return Integer.compare(v, o.v); }
        public String toString() { return "IntBox(" + v + ")"; }
    }
    static <T extends Comparable<? super T>> T max(List<? extends T> xs) {
        T best = null;
        for (T x : xs) if (best == null || x.compareTo(best) > 0) best = x;
        return best;
    }
    static <K, V> Map<V, K> invert(Map<K, V> m) {
        Map<V, K> r = new TreeMap<>();
        for (Map.Entry<K, V> e : m.entrySet()) r.put(e.getValue(), e.getKey());
        return r;
    }
    static abstract class Shape { abstract Shape copy(); String name() { return "shape"; } }
    static class Sq extends Shape { Sq copy() { return new Sq(); } String name() { return "square"; } }
    static double sum(Collection<? extends Number> ns) { double s = 0; for (Number n : ns) s += n.doubleValue(); return s; }
    static void fill(List<? super Integer> l) { for (int i = 0; i < 3; i++) l.add(i); }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) {
        Box<Integer> b = new IntBox(4);
        Box raw = b;
        System.out.println(b.get() + 1 + " " + raw.get() + " " + b.self().get() + " " + (raw.self() instanceof IntBox));
        Comparable c = new IntBox(1);
        System.out.println(c.compareTo(new IntBox(2)));
        try { c.compareTo("not a box"); } catch (ClassCastException e) { System.out.println("bridge cast: ClassCastException"); }
        List<IntBox> boxes = new ArrayList<>(List.of(new IntBox(3), new IntBox(9), new IntBox(5)));
        System.out.println(max(boxes) + " " + max(Arrays.asList("pear", "apple", "zoo")));
        Collections.sort(boxes);
        System.out.println(boxes + " " + Collections.max(boxes));
        Map<String, Integer> m = new HashMap<>();
        m.put("one", 1); m.put("two", 2); m.put("three", 3);
        System.out.println(invert(m));
        Shape s = new Sq();
        System.out.println(s.copy().name() + " " + s.copy().getClass().getSimpleName());
        List<Number> nums = new ArrayList<>();
        fill(nums);
        nums.add(2.5);
        System.out.println(nums + " " + sum(nums));
        List<String> strs = new ArrayList<>();
        List rawList = strs;
        rawList.add(42);
        try { String first = strs.get(0); System.out.println(first); }
        catch (ClassCastException e) { System.out.println("heap pollution: ClassCastException"); }
        Object o = strs;
        System.out.println(o instanceof List<?> l ? "a list of " + l.size() : "no");
        var inferred = new ArrayList<Map<String, List<Integer>>>();
        inferred.add(Map.of("k", List.of(1, 2)));
        System.out.println(inferred.get(0).get("k").get(1) * 10);
    }
}
