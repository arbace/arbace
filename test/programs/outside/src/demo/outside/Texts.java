package demo.outside;

import org.example.toyext.Marker;
import org.example.toyext.ToyException;
import org.example.toyext.ToySource;
import org.example.toyext.ToyText;

/** Names a binary-only jar's classes (as jdom2, commons-io, OSGi and jspecify are Ghidra's and log4j's). */
@Marker("texts")
public class Texts {
    public static String plain(String s) {
        return s;
    }

    /** ToyText.NAME is a constant: javac folds it, so the main run needs no outside class. */
    public static String library() {
        return ToyText.NAME;
    }

    public static String fancy(String s) {
        return ToyText.upper(s);
    }

    public static String readOr(ToySource src, String dflt) {
        try {
            return src.read();
        } catch (ToyException e) {
            return dflt;
        }
    }
}
