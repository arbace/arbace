package p;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.Supplier;

@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER}) @interface A { int value() default 0; }
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE_USE) @interface B { Color k() default Color.RED; String s() default "b"; ElementType e() default ElementType.FIELD; Class<?> c() default void.class; long[] ls() default {}; }
@Retention(RetentionPolicy.CLASS) @Target(ElementType.TYPE_USE) @interface Inv { }
@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD}) @interface Both { double d() default 1.5; float f() default 2.5f; char ch() default 'x'; boolean z() default true; byte by() default 3; short sh() default 4; }

enum Color { RED, GREEN }

public class TA<@A(1) T extends @B(s = "bound") Comparable<@A(2) T>, U> extends @A(3) AbstractList<@B(e = ElementType.METHOD) String> implements @A(4) Cloneable, @B(c = int[].class, ls = {1L, 2L}) Supplier<U> {
    public @A(5) String @A(6) [] @B [] f1;
    public Map<@A(7) ? extends @B(s = "w") Number, List<@Inv @A(8) String>> f2;
    @Both public int f3;
    public TA.@A(9) Inner f4;
    public @B(k = Color.GREEN) Object f5;

    public class Inner { }

    public <@A(10) V extends @A(11) Object> @A(12) List<V> m(@A(13) int a, @B(s = "p1") List<@A(14) ? super V> b) throws @A(15) RuntimeException, @B Exception {
        return null;
    }

    public void r(@A(16) TA<T, U> this) { }

    @Both public @A(17) int @A(18) [] arr() { return null; }

    public TA(@A(19) String x) { }

    public String get(int i) { return null; }
    public int size() { return 0; }
    public U get() { return null; }
}
