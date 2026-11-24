import java.util.*;
public class Plain {
    static int counter;
    int f;
    Plain(int f) { this.f = f; }
    static String run(int n) {
        StringBuilder sb = new StringBuilder();
        Object o = n > 3 ? (Object) "s" : (Object) Integer.valueOf(n);
        sb.append(o);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i * i;
        long sum = 0;
        for (int x : a) sum += x;
        sb.append(sum);
        try {
            if (n == 2) throw new IllegalStateException("two");
            sb.append(100 / (n - 5));
        } catch (ArithmeticException e) {
            sb.append("div");
        } catch (RuntimeException e) {
            sb.append(e.getMessage());
        } finally {
            counter++;
        }
        switch (n) { case 1: sb.append('a'); break; case 2: case 3: sb.append('b'); break; default: sb.append('z'); }
        List<String> l = new ArrayList<String>();
        l.add("q");
        for (Iterator<String> it = l.iterator(); it.hasNext();) sb.append(it.next());
        Plain p = new Plain(n);
        Object q = p;
        if (q instanceof Plain) sb.append(((Plain) q).f);
        double d = n / 2.0; float fl = (float) d; sb.append(d).append(fl);
        synchronized (sb) { sb.append(counter); }
        String[][] m = new String[2][2]; m[1][1] = "m"; sb.append(m[1][1]);
        return sb.toString();
    }
}
