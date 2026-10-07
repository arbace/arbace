package sample;

import java.util.*;
import java.util.function.*;

/**
 * Type annotations (SPEC §4.4) in every position javac writes them: class header, type
 * parameters and bounds, fields (type arguments, wildcards, arrays, inner types), receiver,
 * parameters, return, throws, and in code: locals, casts, instanceof, patterns, new, arrays,
 * catch parameters, method and constructor references, explicit type arguments, for-each.
 */
public class TypeAnns<@TypeUseA T extends @TypeUseB Comparable<T>> extends @TypeUseA Object
    implements @TypeUseB Comparable<@TypeUseA TypeAnns<T>> {
  List<@TypeUseA String> names;
  @TypeUseA String @TypeUseB [] array;
  Map<@TypeUseA ? extends @TypeUseB Number, List<@TypeUseB(3) ?>> map;
  @TypeUseC String both;
  TypeAnns<T>.@TypeUseA Inner inner;

  class Inner {}

  public int compareTo(@TypeUseA TypeAnns<T> this, @TypeUseB TypeAnns<T> o) { return 0; }

  @TypeUseA String m(@TypeUseB int x, List<@TypeUseA ? super T> ys) throws @TypeUseA RuntimeException {
    @TypeUseB String s = (@TypeUseA String) (Object) "x";
    Object o = s;
    if (o instanceof @TypeUseA String t) { s = t; }
    boolean b = o instanceof @TypeUseB CharSequence;
    List<@TypeUseA String> l = new @TypeUseB ArrayList<@TypeUseA String>();
    String[] a = new @TypeUseA String @TypeUseB [3];
    try { s.length(); } catch (@TypeUseA IllegalStateException | @TypeUseB IllegalArgumentException e) { s = null; }
    Supplier<List<String>> sup = ArrayList<@TypeUseA String>::new;
    Function<String[], List<String>> f = Arrays::<@TypeUseA String>asList;
    List<String> e = Collections.<@TypeUseB String>emptyList();
    for (@TypeUseA String z : l) { s = z; }
    return s;
  }

  <@TypeUseB U extends @TypeUseA Object> U id(U u) { return u; }
}
