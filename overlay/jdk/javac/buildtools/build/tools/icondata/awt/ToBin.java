/*
 * Arbace (overlay/jdk/javac/buildtools, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS359 stages 2-3
 * (day)", JCS368 (b)): the tool-world variant of the JDK build's TOOL_AWT_TOBIN
 * (make/jdk/src/classes/build/tools/icondata/awt/ToBin.java, make/modules/java.desktop/gensrc/GensrcIcons.gmk),
 * which reads a PNG from standard input with javax.imageio, draws it into a TYPE_INT_ARGB BufferedImage with
 * java.awt.Graphics and prints the image's width, height and pixels. javac's tool executable holds neither
 * javax.imageio nor Java2D, so this variant does what they do for the icons the build gives it (8-bit RGBA or
 * RGB, not interlaced: the four X11 java-icon*.png): the PNG's IDAT inflated and its filters undone (the PNG
 * specification, ISO/IEC 15948, section 9), and each pixel drawn as Java2D's software loop draws a
 * non-premultiplied source over a cleared IntArgb destination with SrcOver: the colour premultiplied by alpha
 * and divided back through Java2D's 8-bit tables (mul8table, div8table: their construction as
 * src/java.desktop/share/native/libawt/java2d/loops/AlphaMath.c documents it), a fully transparent pixel 0.
 * The output is printed as ToBin prints it. Another PNG kind stops with a message, not a guess.
 * Written for the JDK build's tool's documented behaviour (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package build.tools.icondata.awt;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ToBin {
    private static final int[][] MUL8 = new int[256][256];
    private static final int[][] DIV8 = new int[256][256];

    static {
        for (int i = 1; i < 256; i++) {
            int inc = (i << 16) + (i << 8) + i;
            int val = inc + (1 << 23);
            for (int j = 1; j < 256; j++) {
                MUL8[i][j] = (val >>> 24) & 0xff;
                val += inc;
            }
        }
        for (int i = 1; i < 256; i++) {
            long inc = ((0xffL << 24) + (i / 2)) / i;
            long val = 1L << 23;
            for (int j = 0; j < i; j++) {
                DIV8[i][j] = (int) ((val >>> 24) & 0xff);
                val += inc;
            }
            for (int j = i; j < 256; j++) {
                DIV8[i][j] = 255;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DataInputStream in = new DataInputStream(System.in);
        byte[] sig = new byte[8];
        in.readFully(sig);
        if ((sig[0] & 0xff) != 0x89 || sig[1] != 'P' || sig[2] != 'N' || sig[3] != 'G') {
            throw new IOException("ToBin: not a PNG");
        }
        int width = 0, height = 0, colorType = -1;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        while (true) {
            int length = in.readInt();
            byte[] type = new byte[4];
            in.readFully(type);
            byte[] data = new byte[length];
            in.readFully(data);
            in.readInt(); // CRC
            String t = new String(type, "ISO-8859-1");
            if (t.equals("IHDR")) {
                width = be(data, 0);
                height = be(data, 4);
                int depth = data[8] & 0xff;
                colorType = data[9] & 0xff;
                int interlace = data[12] & 0xff;
                if (depth != 8 || (colorType != 6 && colorType != 2) || interlace != 0) {
                    throw new IOException("ToBin: only 8-bit RGBA or RGB PNGs without interlacing (depth " + depth
                                          + ", colour type " + colorType + ", interlace " + interlace + ")");
                }
            } else if (t.equals("IDAT")) {
                idat.write(data);
            } else if (t.equals("IEND")) {
                break;
            }
        }
        int bpp = colorType == 6 ? 4 : 3;
        int stride = width * bpp;
        byte[] raw = inflate(idat.toByteArray(), height * (stride + 1));
        int[] prev = new int[stride];
        int[] line = new int[stride];
        int[] pixels = new int[width * height];
        int p = 0;
        for (int y = 0; y < height; y++) {
            int filter = raw[p++] & 0xff;
            for (int x = 0; x < stride; x++) {
                int v = raw[p++] & 0xff;
                int a = x >= bpp ? line[x - bpp] : 0;
                int b = prev[x];
                int c = x >= bpp ? prev[x - bpp] : 0;
                switch (filter) {
                    case 0: break;
                    case 1: v += a; break;
                    case 2: v += b; break;
                    case 3: v += (a + b) >> 1; break;
                    case 4: {
                        int pa = Math.abs(b - c), pb = Math.abs(a - c), pc = Math.abs(a + b - 2 * c);
                        v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
                        break;
                    }
                    default: throw new IOException("ToBin: PNG filter " + filter);
                }
                line[x] = v & 0xff;
            }
            for (int x = 0; x < width; x++) {
                int r = line[x * bpp], g = line[x * bpp + 1], bl = line[x * bpp + 2];
                int al = bpp == 4 ? line[x * bpp + 3] : 255;
                if (al == 0) {
                    r = g = bl = 0;
                } else if (al < 255) {
                    r = DIV8[al][MUL8[al][r]];
                    g = DIV8[al][MUL8[al][g]];
                    bl = DIV8[al][MUL8[al][bl]];
                }
                pixels[y * width + x] = (al << 24) | (r << 16) | (g << 8) | bl;
            }
            int[] t = prev; prev = line; line = t;
        }
        System.out.print(width + ",");
        System.out.println(height + ",");
        for (int i = 0; i < pixels.length; i++) {
            System.out.print("0x" + Integer.toHexString(pixels[i]) + ", ");
            if (i % 10 == 0) {
                System.out.println();
            }
        }
    }

    private static int be(byte[] d, int o) {
        return ((d[o] & 0xff) << 24) | ((d[o + 1] & 0xff) << 16) | ((d[o + 2] & 0xff) << 8) | (d[o + 3] & 0xff);
    }

    private static byte[] inflate(byte[] z, int size) throws IOException {
        Inflater inf = new Inflater();
        inf.setInput(z);
        byte[] out = new byte[size];
        int n = 0;
        try {
            while (n < size && !inf.finished()) {
                int k = inf.inflate(out, n, size - n);
                if (k == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    break;
                }
                n += k;
            }
        } catch (DataFormatException e) {
            throw new IOException("ToBin: " + e.getMessage());
        } finally {
            inf.end();
        }
        if (n != size) {
            throw new IOException("ToBin: the image data is " + n + " bytes, not " + size);
        }
        return out;
    }
}
