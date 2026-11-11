/*
 * Arbace (overlay/jdk/javac/buildtools, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS359 stages 2-3 (day)"): for
 * the JDK build's TOOL_VARHANDLEGUARDMETHODGENERATOR (make/jdk/src/classes/build/tools/methodhandle/
 * VarHandleGuardMethodGenerator.java, make/modules/java.base/gensrc/GensrcVarHandles.gmk) on javac's tool executable,
 * whose world has no java.lang.invoke.MethodType factory, no VarHandle and no java.lang.classfile: bin/javac-vendor
 * gensrc reads the build's generator with its uses of those renamed to this package's classes
 * (test/javac-platform/gensrc.bash's gensrc_patch_methodhandle), as make/CopyInterimTZDB.gmk renames a package.
 * GuardTypeKind: java.lang.classfile.TypeKind.from(type).upperBound().descriptorString() as the API documents it: a
 * primitive type's kind is its own upper bound, its descriptor the type's ("Z", "B", "C", "S", "I", "J", "F", "D", "V");
 * a reference type's kind is REFERENCE, whose upper bound is Object, "Ljava/lang/Object;".
 * Written for the documented Java SE API (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package build.tools.methodhandle;

final class GuardTypeKind {
    private GuardTypeKind() {
    }

    static String upperBoundDescriptor(Class<?> type) {
        if (!type.isPrimitive()) {
            return "Ljava/lang/Object;";
        }
        return type.descriptorString();
    }
}
