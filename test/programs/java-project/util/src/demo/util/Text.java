package demo.util;

/** A module's class: the app module compiles against it. */
public final class Text {
    private Text() {
    }

    public static String shout(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) {
            if (b.length() > 0) b.append(' ');
            b.append(w.toUpperCase()).append('!');
        }
        return b.toString();
    }
}
