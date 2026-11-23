import java.util.*;
import java.util.function.*;
public class Hello implements Runnable {
    static int counter = 5;
    long total;
    final String name;
    public Hello(String name) { this.name = name; }
    public void run() { total += name.length(); }
    static int fib(int n) { return n < 2 ? n : fib(n - 1) + fib(n - 2); }
    static double mix(int a, long b, float c, double d) { return a + b * c / d; }
    public static String main2(String[] args) {
        StringBuilder sb = new StringBuilder();
        Hello h = new Hello("abc");
        h.run(); h.run();
        sb.append(h.total).append(' ').append(fib(15)).append(' ').append(mix(3, 4L, 2.5f, 2.0));
        int[] xs = {5, 3, 9, 1};
        Arrays.sort(xs);
        for (int x : xs) sb.append(',').append(x);
        List<String> l = new ArrayList<>();
        l.add("x"); l.add("yy");
        Function<String, Integer> f = String::length;
        int s = 0;
        for (String e : l) s += f.apply(e);
        sb.append(" s=" + s);
        Supplier<String> sup = () -> h.name + counter;
        sb.append(' ').append(sup.get());
        try { Object o = null; o.toString(); } catch (NullPointerException e) { sb.append(" npe"); }
        try { int z = 0; int q = 5 / z; } catch (ArithmeticException e) { sb.append(" ae:" + e.getMessage()); }
        switch (args.length) { case 0: sb.append(" zero"); break; case 1: sb.append(" one"); break; default: sb.append(" many"); }
        String k = "b";
        switch (k) { case "a" -> sb.append(" A"); case "b" -> sb.append(" B"); default -> sb.append(" ?"); }
        synchronized (sb) { sb.append(" sync"); }
        long[][] m = new long[2][3]; m[1][2] = 7; sb.append(" m=" + m[1][2]);
        Object[] objs = new String[1];
        try { objs[0] = Integer.valueOf(1); } catch (ArrayStoreException e) { sb.append(" ase"); }
        sb.append(" " + (char)('a' + 2) + (byte)300 + (short)70000 + (int)3.9e10 + (long)-2.5f);
        return sb.toString();
    }
}
