/*
 * Arbace (overlay/jdk/javac/buildtools, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS359 stages 2-3 (day)"): for
 * the JDK build's TOOL_VARHANDLEGUARDMETHODGENERATOR (make/jdk/src/classes/build/tools/methodhandle/
 * VarHandleGuardMethodGenerator.java, make/modules/java.base/gensrc/GensrcVarHandles.gmk) on javac's tool executable,
 * whose world has no java.lang.invoke.MethodType factory, no VarHandle and no java.lang.classfile: bin/javac-vendor
 * gensrc reads the build's generator with its uses of those renamed to this package's classes
 * (test/javac-platform/gensrc.bash's gensrc_patch_methodhandle), as make/CopyInterimTZDB.gmk renames a package.
 * GuardMethodType: the part of java.lang.invoke.MethodType the generator calls, a return type and parameter types as
 * the API documents them, equal when both are (the generator's distinct()).
 * Written for the documented Java SE API (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package build.tools.methodhandle;

import java.util.Arrays;
import java.util.List;

final class GuardMethodType {
    private final Class<?> rtype;
    private final Class<?>[] ptypes;

    private GuardMethodType(Class<?> rtype, Class<?>[] ptypes) {
        this.rtype = rtype;
        this.ptypes = ptypes;
    }

    static GuardMethodType methodType(Class<?> rtype, List<Class<?>> ptypes) {
        return new GuardMethodType(rtype, ptypes.toArray(new Class<?>[0]));
    }

    Class<?> returnType() {
        return rtype;
    }

    int parameterCount() {
        return ptypes.length;
    }

    Class<?> parameterType(int num) {
        return ptypes[num];
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GuardMethodType t && rtype == t.rtype && Arrays.equals(ptypes, t.ptypes);
    }

    @Override
    public int hashCode() {
        return 31 * rtype.hashCode() + Arrays.hashCode(ptypes);
    }
}
