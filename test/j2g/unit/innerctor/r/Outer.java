// test/j2g/unit.ae's fixture (JCS351 (a)): constructors with implicit parameters and annotated parameters
package r;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Outer {
    @Retention(RetentionPolicy.RUNTIME) public @interface Tag { int value(); }

    public class In {
        @Tag(1) public In(@Tag(2) String s) { }
    }

    public static class Nested {
        @Tag(3) public Nested(@Tag(4) String s) { }
    }
}
