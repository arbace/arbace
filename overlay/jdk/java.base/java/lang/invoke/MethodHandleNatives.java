/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.MethodHandleNatives, the part with no VM in it: the constant pool's reference kinds and
 * their predicates, which MethodHandleInfo (jdk26u's, unpatched) and jrt's handles use, the constants and
 * bodies jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/MethodHandleNatives.java; Copyright (c) 2008, 2025, Oracle
 * and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md). The natives (resolution, linkage,
 * call sites' up-calls) are left out: jrt's handles are made over java.lang.reflect.
 * The selection Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import static java.lang.invoke.MethodHandleNatives.Constants.*;

/** The reference kinds of the constant pool (jdk26u's MethodHandleNatives without its natives). */
class MethodHandleNatives {

    private MethodHandleNatives() { } // static only

    static class Constants {
        Constants() { } // static only

        /**
         * Constant pool reference-kind codes, as used by CONSTANT_MethodHandle CP entries.
         */
        static final byte
            REF_NONE                    = 0,  // null value
            REF_getField                = 1,
            REF_getStatic               = 2,
            REF_putField                = 3,
            REF_putStatic               = 4,
            REF_invokeVirtual           = 5,
            REF_invokeStatic            = 6,
            REF_invokeSpecial           = 7,
            REF_newInvokeSpecial        = 8,
            REF_invokeInterface         = 9,
            REF_LIMIT                  = 10;
    }

    static boolean refKindIsValid(int refKind) {
        return (refKind > REF_NONE && refKind < REF_LIMIT);
    }
    static boolean refKindIsField(byte refKind) {
        assert(refKindIsValid(refKind));
        return (refKind <= REF_putStatic);
    }
    static boolean refKindIsGetter(byte refKind) {
        assert(refKindIsValid(refKind));
        return (refKind <= REF_getStatic);
    }
    static boolean refKindIsSetter(byte refKind) {
        return refKindIsField(refKind) && !refKindIsGetter(refKind);
    }
    static boolean refKindIsMethod(byte refKind) {
        return !refKindIsField(refKind) && (refKind != REF_newInvokeSpecial);
    }
    static boolean refKindIsConstructor(byte refKind) {
        return (refKind == REF_newInvokeSpecial);
    }
    static boolean refKindHasReceiver(byte refKind) {
        assert(refKindIsValid(refKind));
        return (refKind & 1) != 0;
    }
    static boolean refKindIsStatic(byte refKind) {
        return !refKindHasReceiver(refKind) && (refKind != REF_newInvokeSpecial);
    }
    static boolean refKindDoesDispatch(byte refKind) {
        assert(refKindIsValid(refKind));
        return (refKind == REF_invokeVirtual ||
                refKind == REF_invokeInterface);
    }
    static String refKindName(byte refKind) {
        assert(refKindIsValid(refKind));
        return switch (refKind) {
            case REF_getField         -> "getField";
            case REF_getStatic        -> "getStatic";
            case REF_putField         -> "putField";
            case REF_putStatic        -> "putStatic";
            case REF_invokeVirtual    -> "invokeVirtual";
            case REF_invokeStatic     -> "invokeStatic";
            case REF_invokeSpecial    -> "invokeSpecial";
            case REF_newInvokeSpecial -> "newInvokeSpecial";
            case REF_invokeInterface  -> "invokeInterface";
            default -> "REF_???";
        };
    }
}
