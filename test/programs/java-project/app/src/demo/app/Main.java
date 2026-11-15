package demo.app;

import demo.common.Other;
import demo.common.Shared;
import demo.util.Text;
import org.example.toyapi.Nullable;
import org.example.toylib.Greeter;

/** The entry: a module of sources, a library from its sources jar, an annotation from a binary-only jar. */
public class Main {
    static @Nullable String first(String[] args) {
        return args.length > 0 ? args[0] : null;
    }

    public static void main(String[] args) {
        String who = first(args);
        System.out.println(Greeter.greet(who == null ? "world" : who));
        System.out.println(Text.shout("java by modules") + " " + Shared.tag() + " " + Other.tag());
    }
}
