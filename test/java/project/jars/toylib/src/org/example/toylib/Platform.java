package org.example.toylib;

/** A multi-release class: the base, and the variants of META-INF/versions/11 and 99. */
final class Platform {
    private Platform() {
    }

    static String name() {
        return "base";
    }
}
