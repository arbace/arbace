/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS350, JCS351, JCS353"): jrt's
 * java.lang.invoke.StringConcatFactory, F15's next slice (JCS353 (a)), over jrt's handles (MethodHandle's
 * header). jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java) links a concatenation to a hidden class
 * it spins with the class-file API (InlineHiddenClassStrategy) or, for the trivial shapes, to StringConcatHelper's
 * methods through JavaLangAccess; the Go build has neither (javac's string concatenation in Java forms and in
 * Arbace's code is compiled directly, J2G-SPEC section 7.5, so only a program's own calls of these methods reach
 * here). The parts whose text is not the VM's are jdk26u's (Copyright (c) 2015, 2025, Oracle and/or its
 * affiliates, GPL 2 with the Classpath Exception: LICENSE.md): makeConcat's mocked recipe, makeConcatWithConstants'
 * linkage checks in their order and their StringConcatException messages (the null checks, the caller's private
 * access, the recipe parsed by parseRecipe, the return type, the 200 argument slots), parseRecipe itself (the
 * interleaved constants, argumentMismatch, constantMismatch), makeSimpleConcat's shapes (zero arguments, one, two
 * references) with their stringifiers (String.valueOf by the parameter's type; StringConcatHelper's newStringOf
 * and simpleConcat, whose stringOf maps null and a null toString to "null"), and a generator's Throwable wrapped
 * as "Generator failed". What the hidden class did: the general strategy is an adapter of the call's type that
 * stringifies its arguments left to right (JLS 5.1.11, the hidden class's order: every argument's string before
 * the result is assembled) between the recipe's constants. The result's identity is a fresh String as jdk26u's.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import java.util.Objects;

/** Methods to facilitate the creation of String concatenation methods (jrt's, over its handles). */
public final class StringConcatFactory {

    /** Tag used to demarcate an ordinary argument. */
    private static final char TAG_ARG = '\u0001';

    /** Tag used to demarcate a constant. */
    private static final char TAG_CONST = '\u0002';

    /** Maximum number of argument slots in String Concat call (jdk26u's). */
    private static final int MAX_INDY_CONCAT_ARG_SLOTS = 200;

    /** Facilitates the creation of String concatenation methods of a known number of arguments of known types. */
    public static CallSite makeConcat(MethodHandles.Lookup lookup,
                                      String name,
                                      MethodType concatType) throws StringConcatException {
        // Mock the recipe to reuse the concat generator code
        String recipe = "\u0001".repeat(concatType.parameterCount());
        return makeConcatWithConstants(lookup, name, concatType, recipe);
    }

    /** Facilitates the creation of String concatenation methods by a recipe of arguments and constants. */
    public static CallSite makeConcatWithConstants(MethodHandles.Lookup lookup,
                                                   String name,
                                                   MethodType concatType,
                                                   String recipe,
                                                   Object... constants)
        throws StringConcatException
    {
        Objects.requireNonNull(lookup, "Lookup is null");
        Objects.requireNonNull(name, "Name is null");
        Objects.requireNonNull(recipe, "Recipe is null");
        Objects.requireNonNull(concatType, "Concat type is null");
        Objects.requireNonNull(constants, "Constants are null");

        for (Object o : constants) {
            Objects.requireNonNull(o, "Cannot accept null constants");
        }

        if ((lookup.lookupModes() & MethodHandles.Lookup.PRIVATE) == 0) {
            throw new StringConcatException("Invalid caller: " +
                    lookup.lookupClass().getName());
        }

        String[] constantStrings = parseRecipe(concatType, recipe, constants);

        if (!concatType.returnType().isAssignableFrom(String.class)) {
            throw new StringConcatException(
                    "The return type should be compatible with String, but it is " +
                            concatType.returnType());
        }

        if (concatType.parameterSlotCount() > MAX_INDY_CONCAT_ARG_SLOTS) {
            throw new StringConcatException("Too many concat argument slots: " +
                    concatType.parameterSlotCount() +
                    ", can only accept " +
                    MAX_INDY_CONCAT_ARG_SLOTS);
        }

        try {
            MethodHandle mh = makeSimpleConcat(concatType, constantStrings);

            if (mh == null) {
                mh = generate(concatType, constantStrings);
            }
            // jdk26u: viewAsType(concatType, true), the handle's return type String seen as concatType's
            mh = mh.asType(concatType);

            return new ConstantCallSite(mh);
        } catch (Error e) {
            // Pass through any error
            throw e;
        } catch (Throwable t) {
            throw new StringConcatException("Generator failed", t);
        }
    }

    private static String[] parseRecipe(MethodType concatType,
                                        String recipe,
                                        Object[] constants)
        throws StringConcatException
    {

        Objects.requireNonNull(recipe, "Recipe is null");
        int paramCount = concatType.parameterCount();
        // Array containing interleaving String constants, starting with
        // the first prefix and ending with the final prefix:
        //
        //   consts[0] + arg0 + consts[1] + arg 1 + ... + consts[paramCount].
        String[] consts = new String[paramCount + 1];

        int cCount = 0;
        int oCount = 0;

        StringBuilder acc = new StringBuilder();

        for (int i = 0; i < recipe.length(); i++) {
            char c = recipe.charAt(i);

            if (c == TAG_CONST) {
                if (cCount == constants.length) {
                    // Not enough constants
                    throw constantMismatch(constants, cCount);
                }
                // Accumulate constant args along with any constants encoded
                // into the recipe
                acc.append(constants[cCount++]);
            } else if (c == TAG_ARG) {
                // Check for overflow
                if (oCount >= paramCount) {
                    throw argumentMismatch(concatType, oCount);
                }

                // Flush any accumulated characters into a constant
                consts[oCount++] = acc.length() > 0 ? acc.toString() : "";
                acc.setLength(0);
            } else {
                // Not a special character, this is a constant embedded into
                // the recipe itself.
                acc.append(c);
            }
        }
        if (oCount != concatType.parameterCount()) {
            throw argumentMismatch(concatType, oCount);
        }
        if (cCount < constants.length) {
            throw constantMismatch(constants, cCount);
        }

        // Flush the remaining characters as constant:
        consts[oCount] = acc.length() > 0 ? acc.toString() : "";
        return consts;
    }

    private static StringConcatException argumentMismatch(MethodType concatType,
                                                          int oCount) {
        return new StringConcatException(
                "Mismatched number of concat arguments: recipe wants " +
                oCount +
                " arguments, but signature provides " +
                concatType.parameterCount());
    }

    private static StringConcatException constantMismatch(Object[] constants,
            int cCount) {
        return new StringConcatException(
                "Mismatched number of concat constants: recipe wants " +
                        cCount +
                        " constants, but only " +
                        constants.length +
                        " are passed");
    }

    private static MethodHandle makeSimpleConcat(MethodType mt, String[] constants) {
        int paramCount = mt.parameterCount();
        String suffix = constants[paramCount];

        // Fast-path trivial concatenations
        if (paramCount == 0) {
            return MethodHandles.insertArguments(newStringifier(), 0, suffix == null ? "" : suffix);
        }
        if (paramCount == 1) {
            String prefix = constants[0];
            // Empty constants will be
            if (prefix.isEmpty()) {
                if (suffix.isEmpty()) {
                    return unaryConcat(mt.parameterType(0));
                } else if (!mt.hasPrimitives()) {
                    return MethodHandles.insertArguments(simpleConcat(), 1, suffix);
                } // else fall-through
            } else if (suffix.isEmpty() && !mt.hasPrimitives()) {
                // Non-primitive argument
                return MethodHandles.insertArguments(simpleConcat(), 0, prefix);
            } // fall-through if there's both a prefix and suffix
        } else if (paramCount == 2 && !mt.hasPrimitives() && suffix.isEmpty()
                && constants[0].isEmpty() && constants[1].isEmpty()) {
            // Two reference arguments, no surrounding constants
            return simpleConcat();
        }

        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // jrt: the handles jdk26u finds (String.valueOf by publicLookup, StringConcatHelper's by JavaLangAccess), as
    // adapters (a JDK class's MethodHandles.lookup() is the Go build's nil, J2G-SPEC section 8.5)

    /** jdk26u's StringConcatHelper.stringOf: null, and a toString that returns null, as "null". */
    private static String stringOf(Object value) {
        String s;
        return (value == null || (s = value.toString()) == null) ? "null" : s;
    }

    /** jdk26u's StringConcatHelper.simpleConcat(Object, Object): the two strings concatenated, a new String. */
    private static MethodHandle simpleConcat() {
        return MethodHandleImpl.adapter(MethodType.methodType(String.class, Object.class, Object.class),
                a -> new String(stringOf(a[0]).concat(stringOf(a[1]))));
    }

    /** jdk26u's StringConcatHelper.newStringOf(Object): a new String of stringOf's. */
    private static MethodHandle newStringifier() {
        return MethodHandleImpl.adapter(MethodType.methodType(String.class, Object.class),
                a -> new String(stringOf(a[0])));
    }

    /** jdk26u's unaryConcat: a reference by newStringOf, a primitive by String.valueOf of its type. */
    private static MethodHandle unaryConcat(Class<?> cl) {
        if (!cl.isPrimitive()) {
            return newStringifier();
        } else if (cl == int.class || cl == short.class || cl == byte.class) {
            return stringValueOf(int.class);
        } else if (cl == long.class) {
            return stringValueOf(long.class);
        } else if (cl == char.class) {
            return stringValueOf(char.class);
        } else if (cl == boolean.class) {
            return stringValueOf(boolean.class);
        } else if (cl == float.class) {
            return stringValueOf(float.class);
        } else if (cl == double.class) {
            return stringValueOf(double.class);
        } else {
            throw new InternalError("Unhandled type for unary concatenation: " + cl);
        }
    }

    /** String.valueOf(ptype): its argument boxed (a byte or short widened to int by asType), its text. */
    private static MethodHandle stringValueOf(Class<?> ptype) {
        return MethodHandleImpl.adapter(MethodType.methodType(String.class, ptype), a -> String.valueOf(a[0]));
    }

    /**
     * The general strategy (jdk26u's InlineHiddenClassStrategy.generate, its result): a handle of type args with
     * String's return, each argument stringified as JLS 5.1.11 does (a reference by stringOf, a primitive by its
     * box's text, which is String.valueOf's of its type), left to right, between constants[0..n].
     */
    private static MethodHandle generate(MethodType args, String[] constants) {
        int n = args.parameterCount();
        String[] cs = constants.clone();
        return MethodHandleImpl.adapter(args.changeReturnType(String.class), a -> {
            String[] strs = new String[n];
            for (int i = 0; i < n; i++) {
                strs[i] = stringOf(a[i]);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (cs[i] != null) sb.append(cs[i]);
                sb.append(strs[i]);
            }
            if (cs[n] != null) sb.append(cs[n]);
            return sb.toString();
        });
    }

    private StringConcatFactory() {
        // no instantiation
    }
}
