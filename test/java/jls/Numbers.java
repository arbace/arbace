/*
 * @test
 * @summary JLS 5.1.2, 5.1.3, 15.15-15.22: primitive conversions, overflow, division and remainder,
 *          shifts and their counts, char and byte arithmetic, Math's rounding
 */
public class Numbers {
    public static void main(String[] args) {
        double[] ds = {Double.NaN, 1e20, -1e20, -0.0, 2.5, -2.5, 0.49999999999999994, 3.999999999, Double.MIN_VALUE};
        for (double d : ds) {
            System.out.println(d + " -> int " + (int) d + ", long " + (long) d + ", float " + (float) d
                               + ", char " + (int) (char) d + ", short " + (short) d + ", byte " + (byte) d
                               + ", round " + Math.round(d) + ", rint " + Math.rint(d) + ", floor " + Math.floor(d));
        }
        System.out.println(Integer.MAX_VALUE + 1);
        System.out.println(Long.MIN_VALUE - 1);
        System.out.println(Integer.MIN_VALUE / -1);
        System.out.println(Integer.MIN_VALUE % -1);
        System.out.println(-7 / 2 + " " + -7 % 2 + " " + 7 % -2 + " " + Math.floorMod(-7, 2) + " " + Math.floorDiv(-7, 2));
        System.out.println(-7.5 % 2 + " " + 7.5 % -2 + " " + 5.0 % 0 + " " + 1.0 / 0);
        System.out.println((1 << 33) + " " + (1L << 33) + " " + (1 << -1) + " " + (-8 >> 1) + " " + (-8 >>> 28) + " " + (-8L >>> 60));
        byte b = (byte) 200;
        b += 100;
        short s = (short) 40000;
        char c = 'A';
        c += 2;
        c++;
        System.out.println(b + " " + s + " " + c + " " + (int) c + " " + (char) 65 + (char) -1 + "|" + (int) (char) -1);
        int x = 'a' * 'b';
        long big = 1_000_000 * 1_000_000;
        long bigL = 1_000_000L * 1_000_000;
        System.out.println(x + " " + big + " " + bigL);
        float f = 16777217;
        System.out.println(f + " " + (int) f + " " + (0.1f + 0.2f) + " " + (0.1 + 0.2 == 0.3) + " " + (0.1f + 0.2f == 0.3f));
        System.out.println(Integer.toBinaryString(-1 >>> 1) + " " + Integer.toHexString(Integer.MIN_VALUE) + " " + Long.toOctalString(8));
        System.out.println(Integer.compare(3, 5) + " " + Integer.signum(-9) + " " + Integer.bitCount(255) + " " + Integer.reverse(1) + " " + Long.numberOfTrailingZeros(64));
        System.out.println(Math.abs(Integer.MIN_VALUE) + " " + Math.max(-0.0, 0.0) + " " + Math.min(-0.0, 0.0) + " " + Math.max(Double.NaN, 1));
        System.out.println(Double.compare(0.0, -0.0) + " " + Double.valueOf(Double.NaN).equals(Double.NaN) + " " + (Double.NaN == Double.NaN));
        Integer i1 = 127, i2 = 127, i3 = 128, i4 = 128;
        System.out.println((i1 == i2) + " " + i3.equals(i4) + " " + (i3 < i4 + 1));
        long sum = 0;
        for (int i = 0; i < 100000; i++) sum += (long) i * i % 7;
        System.out.println("sum " + sum);
    }
}
