/*
 * @test
 * @summary JLS 15.18.1, 5.1.11: string concatenation of every type, constants folded, the
 *          digits Float.toString and Double.toString give
 */
public class StringConcat {
    static final String K = "k" + 1 + 2;
    static final int N = 3;
    public static void main(String[] args) {
        byte b = -1; short s = 300; char c = 'x'; int i = Integer.MIN_VALUE; long l = Long.MAX_VALUE;
        float f = 1.1f; double d = 0.1 + 0.2; boolean z = true; Object o = null; String n = null;
        System.out.println("" + b + s + c + i + l + f + d + z + o + n);
        System.out.println(1 + 2 + "3" + 4 + 5);
        System.out.println('a' + 'b' + "c" + 'd' + 'e');
        System.out.println(c + 1);
        System.out.println((char) (c + 1));
        System.out.println(K + N + (K == "k12"));
        System.out.println(1.0f + " " + 100.0 + " " + 1e7 + " " + 1e-3 + " " + 1e-4 + " " + 1e21 + " " + 123456789.0f);
        System.out.println(Float.MIN_VALUE + " " + Float.MAX_VALUE + " " + Double.MIN_VALUE + " " + Double.MAX_VALUE);
        System.out.println((0.0 / 0.0) + " " + (1.0 / 0.0) + " " + (-1.0 / 0.0) + " " + (-0.0) + " " + (0.0f == -0.0f));
        System.out.println(2.0 / 3 + " " + 2.0f / 3 + " " + (float) 0.1 + " " + (double) 0.1f);
        char[] cs = {'h', 'i'};
        System.out.println("chars " + String.valueOf(cs) + " " + new StringBuilder().append(cs).append(1.5f).append('!'));
        StringBuilder sb = new StringBuilder("ab");
        sb.insert(1, 42).append(true).reverse();
        System.out.println(sb);
        String t = "x";
        t += 1 + 1;
        t += 'c';
        System.out.println(t);
        System.out.println("abc".repeat(2) + " " + String.join("-", "a", "b") + " " + "A,B,,".split(",").length);
        System.out.println(String.format("%5d|%-5s|%05.2f|%x|%c|%b", 42, "ab", 3.14159, 255, 'q', null));
    }
}
