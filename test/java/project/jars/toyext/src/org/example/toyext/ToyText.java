package org.example.toyext;

/** A binary-only library's utility (as commons-io's FilenameUtils): on javac's class path alone. */
public final class ToyText {
    private ToyText() {
    }

    public static final String NAME = "toyext";

    public static String upper(String s) {
        return s.toUpperCase();
    }
}
