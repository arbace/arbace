package org.example.toylib;

import org.example.toyapi.Nullable;

/** The library's entry point (the closure's root). */
public final class Greeter {
    private Greeter() {
    }

    public static String greet(@Nullable String who) {
        return Helper.prefix() + who + " (" + Platform.name() + ")";
    }
}
