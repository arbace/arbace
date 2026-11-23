/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"): jrt's
 * java.lang.invoke.MethodHandles and its Lookup, over jrt's handles (MethodHandle's header) and
 * java.lang.reflect. The API is jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/invoke/MethodHandles.java); the parts whose text is not the VM's
 * are jdk26u's (Copyright (c) 2008, 2025, Oracle and/or its affiliates, GPL 2 with the Classpath Exception:
 * LICENSE.md): the lookup modes and their constants, in, dropLookupMode, lookupModes, toString,
 * hasFullPrivilegeAccess, the access checks by sun.invoke.util.VerifyAccess (derived: no module boundary),
 * findSpecial's special caller rule, the protected receiver's restriction, the combinators' argument checks.
 * What the VM did: a member's resolution is the JVM's (JVMS 5.4.3.2-5.4.3.4: the class, its superclasses,
 * then its superinterfaces; an interface's Object methods) over the classes' declared members by reflection,
 * the member's descriptor matched exactly; a handle is a DirectMethodHandle; the combinators are
 * MethodHandleImpl's adapters. The caller-sensitive lookup() is resolved where it is called: the Java forms'
 * analyzer writes MethodHandles.lookup() as jdk.internal.jrt.Invokers.lookup(C.class), whose access is
 * registered below; lookup() itself, reached only by reflection, answers the public lookup.
 * Not in the Go build: VarHandles (j2g compiles their constant handles statically, J2G-SPEC section 8.5, and
 * VarHandle stays outside the world: findVarHandle and its kin throw UnsupportedOperationException), class
 * definition by defineHiddenClass, classData, the loops and tableSwitch (defineClass defines through arbace.lang.Bytecode since
 * java-bytecode-1122: doc/classes/BYTECODE-SURVEY.md).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang.invoke;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import sun.invoke.util.VerifyAccess;

import static java.lang.invoke.MethodHandleImpl.adapter;
import static java.lang.invoke.MethodHandleImpl.convert;
import static java.lang.invoke.MethodHandleNatives.Constants.*;
import static java.lang.invoke.MethodHandleStatics.*;

/** Static methods that operate on or return method handles. */
public class MethodHandles {

    private MethodHandles() { }  // do not instantiate

    static {
        jdk.internal.jrt.Invokers.setAccess(new jdk.internal.jrt.Invokers.Access() {
            @Override
            public Lookup lookup(Class<?> caller) {
                return new Lookup(caller);
            }
            @Override
            public Object invokeExact(MethodHandle mh, MethodType site, Object[] args) throws Throwable {
                return Objects.requireNonNull(mh).invokeExactFrom(site, args);
            }
            @Override
            public Object invoke(MethodHandle mh, MethodType site, Object[] args) throws Throwable {
                return Objects.requireNonNull(mh).invokeFrom(site, args);
            }
        });
    }

    /**
     * Returns a lookup object with full capabilities to emulate all supported bytecode behaviors of the
     * caller. jrt: a call of it in Java forms is the caller's (jdk.internal.jrt.Invokers.lookup); reached
     * by reflection it has no caller frame to read, and answers the public lookup.
     */
    public static Lookup lookup() {
        return Lookup.PUBLIC_LOOKUP;
    }

    /** Returns a lookup object which is trusted minimally. */
    public static Lookup publicLookup() {
        return Lookup.PUBLIC_LOOKUP;
    }

    /** Returns a lookup object on a target class to emulate all supported bytecode behaviors, including private access. */
    public static Lookup privateLookupIn(Class<?> targetClass, Lookup caller) throws IllegalAccessException {
        Objects.requireNonNull(targetClass);
        Objects.requireNonNull(caller);
        if (targetClass.isPrimitive())
            throw new IllegalArgumentException(targetClass + " is a primitive class");
        if (targetClass.isArray())
            throw new IllegalArgumentException(targetClass + " is an array class");
        if (!caller.hasFullPrivilegeAccess())
            throw new IllegalAccessException("caller does not have PRIVATE and MODULE lookup mode");
        // the Go build: no module boundary (every package is open to every module)
        return new Lookup(targetClass, null, Lookup.FULL_POWER_MODES & ~Lookup.ORIGINAL);
    }

    /** Performs an unchecked "crack" of a direct method handle. */
    public static <T extends Member> T reflectAs(Class<T> expected, MethodHandle target) {
        Objects.requireNonNull(expected);
        if (!(target instanceof DirectMethodHandle d))
            throw newIllegalArgumentException("not a direct method handle");
        return expected.cast(d.member);
    }

    /** A lookup object is a factory for creating method handles, when the creation requires access checking. */
    public static final class Lookup {
        private final Class<?> lookupClass;
        private final Class<?> prevLookupClass;
        private final int allowedModes;

        /** A single-bit mask representing {@code public} access. */
        public static final int PUBLIC = Modifier.PUBLIC;
        /** A single-bit mask representing {@code private} access. */
        public static final int PRIVATE = Modifier.PRIVATE;
        /** A single-bit mask representing {@code protected} access. */
        public static final int PROTECTED = Modifier.PROTECTED;
        /** A single-bit mask representing {@code package} access (default access). */
        public static final int PACKAGE = Modifier.STATIC;
        /** A single-bit mask representing {@code module} access. */
        public static final int MODULE = PACKAGE << 1;
        /** A single-bit mask representing {@code unconditional} access. */
        public static final int UNCONDITIONAL = PACKAGE << 2;
        /** A single-bit mask representing {@code original} access. */
        public static final int ORIGINAL = PACKAGE << 3;

        private static final int ALL_MODES = (PUBLIC | PRIVATE | PROTECTED | PACKAGE | MODULE | UNCONDITIONAL | ORIGINAL);
        static final int FULL_POWER_MODES = (ALL_MODES & ~UNCONDITIONAL);   // with original access
        private static final int TRUSTED   = -1;

        static final Lookup IMPL_LOOKUP = new Lookup(Object.class, null, TRUSTED);
        static final Lookup PUBLIC_LOOKUP = new Lookup(Object.class, null, UNCONDITIONAL);

        /** Tells which class is performing the lookup. */
        public Class<?> lookupClass() {
            return lookupClass;
        }

        /** Reports a lookup class in another module that this lookup object was previously teleported from, or null. */
        public Class<?> previousLookupClass() {
            return prevLookupClass;
        }

        /** Tells which access-protection classes of members this lookup object can produce. */
        public int lookupModes() {
            return allowedModes & ALL_MODES;
        }

        /** Embody the current class (the lookupClass) as a lookup class for method handle creation. */
        Lookup(Class<?> lookupClass) {
            this(lookupClass, null, FULL_POWER_MODES);
        }

        private Lookup(Class<?> lookupClass, Class<?> prevLookupClass, int allowedModes) {
            this.lookupClass = lookupClass;
            this.prevLookupClass = prevLookupClass;
            this.allowedModes = allowedModes;
        }

        private static Lookup newLookup(Class<?> lookupClass, Class<?> prevLookupClass, int allowedModes) {
            return new Lookup(lookupClass, prevLookupClass, allowedModes);
        }

        /** Creates a lookup on the specified new lookup class (jdk26u's rules). */
        public Lookup in(Class<?> requestedLookupClass) {
            Objects.requireNonNull(requestedLookupClass);
            if (requestedLookupClass.isPrimitive())
                throw new IllegalArgumentException(requestedLookupClass + " is a primitive class");
            if (requestedLookupClass.isArray())
                throw new IllegalArgumentException(requestedLookupClass + " is an array class");

            if (allowedModes == TRUSTED)  // IMPL_LOOKUP can make any lookup at all
                return new Lookup(requestedLookupClass, null, FULL_POWER_MODES);
            if (requestedLookupClass == this.lookupClass)
                return this;  // keep same capabilities
            int newModes = (allowedModes & FULL_POWER_MODES) & ~ORIGINAL;
            Class<?> plc = this.previousLookupClass();
            if ((this.allowedModes & UNCONDITIONAL) != 0) {
                newModes = UNCONDITIONAL;
            }
            // the Go build: one module (no teleporting between modules)
            if ((newModes & PACKAGE) != 0
                && !VerifyAccess.isSamePackage(this.lookupClass, requestedLookupClass)) {
                newModes &= ~(PACKAGE|PRIVATE|PROTECTED);
            }
            // Allow nestmate lookups to be created without special privilege:
            if ((newModes & PRIVATE) != 0
                    && !VerifyAccess.isSamePackageMember(this.lookupClass, requestedLookupClass)) {
                newModes &= ~(PRIVATE|PROTECTED);
            }
            if ((newModes & (PUBLIC|UNCONDITIONAL)) != 0
                && !VerifyAccess.isClassAccessible(requestedLookupClass, this.lookupClass, this.prevLookupClass, allowedModes)) {
                // The requested class it not accessible from the lookup class.
                // No permissions.
                newModes = 0;
            }
            return newLookup(requestedLookupClass, plc, newModes);
        }

        /** Creates a lookup on the same lookup class which loses the given mode (jdk26u's rules). */
        public Lookup dropLookupMode(int modeToDrop) {
            int oldModes = lookupModes();
            int newModes = oldModes & ~(modeToDrop | PROTECTED | ORIGINAL);
            switch (modeToDrop) {
                case PUBLIC: newModes &= ~(FULL_POWER_MODES); break;
                case MODULE: newModes &= ~(PACKAGE | PRIVATE); break;
                case PACKAGE: newModes &= ~(PRIVATE); break;
                case PROTECTED:
                case PRIVATE:
                case ORIGINAL:
                case UNCONDITIONAL: break;
                default: throw new IllegalArgumentException(modeToDrop + " is not a valid mode to drop");
            }
            if (newModes == oldModes) return this;  // return self if no change
            return newLookup(lookupClass(), previousLookupClass(), newModes);
        }

        /** Displays the name of the class from which lookups are to be made, with its modes (jdk26u's). */
        @Override
        public String toString() {
            String cname = lookupClass.getName();
            if (prevLookupClass != null)
                cname += "/" + prevLookupClass.getName();
            switch (allowedModes) {
            case 0:  // no privileges
                return cname + "/noaccess";
            case UNCONDITIONAL:
                return cname + "/publicLookup";
            case PUBLIC:
                return cname + "/public";
            case PUBLIC|MODULE:
                return cname + "/module";
            case PUBLIC|PACKAGE:
            case PUBLIC|MODULE|PACKAGE:
                return cname + "/package";
            case PUBLIC|PACKAGE|PRIVATE:
            case PUBLIC|MODULE|PACKAGE|PRIVATE:
                    return cname + "/private";
            case PUBLIC|PACKAGE|PRIVATE|PROTECTED:
            case PUBLIC|MODULE|PACKAGE|PRIVATE|PROTECTED:
            case FULL_POWER_MODES:
                    return cname;
            case TRUSTED:
                return "/trusted";  // internal only; not exported
            default:  // Should not happen, but it's a bitfield...
                cname = cname + "/" + Integer.toHexString(allowedModes);
                assert(false) : cname;
                return cname;
            }
        }

        /** Returns true if this lookup has PRIVATE and MODULE access. */
        public boolean hasFullPrivilegeAccess() {
            return (allowedModes & (PRIVATE|MODULE)) == (PRIVATE|MODULE);
        }

        /** @deprecated jdk26u's: hasFullPrivilegeAccess. */
        @Deprecated(since="14")
        public boolean hasPrivateAccess() {
            return hasFullPrivilegeAccess();
        }

        // -----------------------------------------------------------------------------------
        // resolution (JVMS 5.4.3), over the declared members by reflection

        private static String typeString(MethodType t) {
            return t.toString();
        }

        private static NoSuchMethodException noSuchMethod(Class<?> refc, String name, MethodType type, byte kind) {
            return new NoSuchMethodException("no such method: " + refc.getName() + "." + name + typeString(type)
                                             + "/" + MethodHandleNatives.refKindName(kind));
        }

        private static NoSuchMethodException noSuchConstructor(Class<?> refc, MethodType type) {
            return new NoSuchMethodException("no such constructor: " + refc.getName() + ".<init>" + typeString(type)
                                             + "/newInvokeSpecial");
        }

        private static NoSuchFieldException noSuchField(Class<?> refc, String name, Class<?> type, byte kind) {
            return new NoSuchFieldException("no such field: " + refc.getName() + "." + name + "/" + type.getName()
                                            + "/" + MethodHandleNatives.refKindName(kind));
        }

        private IllegalAccessException accessException(String message, Member m, byte kind) {
            String what = (m instanceof Field f)
                ? f.getDeclaringClass().getName() + "." + f.getName() + "/" + f.getType().getName()
                : m.getDeclaringClass().getName() + "." + m.getName()
                  + typeString(memberType(m)) ;
            return new IllegalAccessException(message + ": " + what + "/" + MethodHandleNatives.refKindName(kind)
                                              + ", from " + this);
        }

        private static Method declaredMethod(Class<?> c, String name, MethodType type) {
            Class<?>[] ps = type.ptypes();
            Method found = null;
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getReturnType() == type.returnType()
                        && Arrays.equals(m.getParameterTypes(), ps)) {
                    // a bridge loses to the method it bridges to, if both match (they cannot: their return types differ)
                    if (found == null || found.isBridge()) found = m;
                }
            }
            return found;
        }

        /** JVMS 5.4.3.3 and 5.4.3.4: the method name type of refc, or null. */
        static Method resolveMethod(Class<?> refc, String name, MethodType type) {
            for (Class<?> c = refc; c != null; c = c.getSuperclass()) {
                Method m = declaredMethod(c, name, type);
                if (m != null) return m;
            }
            // the superinterfaces, nearest first; a static or private interface method is not inherited
            ArrayDeque<Class<?>> todo = new ArrayDeque<>();
            HashSet<Class<?>> seen = new HashSet<>();
            for (Class<?> c = refc; c != null; c = c.getSuperclass()) todo.addAll(Arrays.asList(c.getInterfaces()));
            while (!todo.isEmpty()) {
                Class<?> i = todo.poll();
                if (!seen.add(i)) continue;
                Method m = declaredMethod(i, name, type);
                if (m != null && (i == refc || !Modifier.isStatic(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers())))
                    return m;
                todo.addAll(Arrays.asList(i.getInterfaces()));
            }
            if (refc.isInterface()) {
                Method m = declaredMethod(Object.class, name, type);
                if (m != null && Modifier.isPublic(m.getModifiers())) return m;
            }
            return null;
        }

        /** JVMS 5.4.3.2: the field name of type of refc, or null. */
        static Field resolveField(Class<?> refc, String name, Class<?> type) {
            for (Field f : refc.getDeclaredFields()) {
                if (f.getName().equals(name) && f.getType() == type) return f;
            }
            for (Class<?> i : refc.getInterfaces()) {
                Field f = resolveField(i, name, type);
                if (f != null) return f;
            }
            Class<?> s = refc.getSuperclass();
            return (s == null) ? null : resolveField(s, name, type);
        }

        static MethodType memberType(Member m) {
            if (m instanceof Method mm) return MethodType.methodType(mm.getReturnType(), mm.getParameterTypes());
            if (m instanceof Constructor<?> c) return MethodType.methodType(void.class, c.getParameterTypes());
            Field f = (Field) m;
            return MethodType.methodType(f.getType());
        }

        // -----------------------------------------------------------------------------------
        // access checks (jdk26u's checkAccess, over VerifyAccess)

        private void checkSymbolicClass(Class<?> refc) throws IllegalAccessException {
            Objects.requireNonNull(refc);
            if (allowedModes == TRUSTED) return;
            if (!VerifyAccess.isClassAccessible(refc, lookupClass, prevLookupClass, allowedModes))
                throw new IllegalAccessException("symbolic reference class is not accessible: class "
                                                 + refc.getName() + ", from " + this);
        }

        private void checkAccess(byte refKind, Class<?> refc, Member m) throws IllegalAccessException {
            if (allowedModes == TRUSTED) return;
            int mods = m.getModifiers();
            if (Modifier.isProtected(mods) && refKind == REF_newInvokeSpecial) {
                // cannot "new" a protected ctor in a different package
                if (!VerifyAccess.isSamePackage(m.getDeclaringClass(), lookupClass))
                    mods ^= Modifier.PROTECTED;
            }
            if (VerifyAccess.isMemberAccessible(refc, m.getDeclaringClass(), mods, lookupClass, prevLookupClass, allowedModes))
                return;
            String message;
            if (Modifier.isPrivate(mods)) message = "member is private";
            else if (Modifier.isProtected(mods)) message = "member is protected";
            else if (Modifier.isPublic(mods)) message = "class is not public";
            else message = "member is private to package";
            throw accessException(message, m, refKind);
        }

        private boolean restrictProtectedReceiver(Member m) {
            // The accessing class only has the right to use a protected member
            // on itself or a subclass.  Enforce that restriction, from JVMS 5.4.4, etc.
            int mods = m.getModifiers();
            if (!Modifier.isProtected(mods) || Modifier.isStatic(mods)
                || allowedModes == TRUSTED
                || m.getDeclaringClass() == lookupClass()
                || VerifyAccess.isSamePackage(m.getDeclaringClass(), lookupClass()))
                return false;
            return true;
        }

        private MethodHandle restrictReceiver(DirectMethodHandle mh, Member m, byte kind) throws IllegalAccessException {
            Class<?> caller = lookupClass();
            if (!m.getDeclaringClass().isAssignableFrom(caller))
                throw accessException("caller class must be a subclass below the method", m, kind);
            MethodType rawType = mh.type();
            if (caller.isAssignableFrom(rawType.parameterType(0))) return mh; // no need to restrict; already narrow
            return mh.withType(rawType.changeParameterType(0, caller));
        }

        // -----------------------------------------------------------------------------------
        // the factories

        /** Produces a method handle for a static method. */
        public MethodHandle findStatic(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            checkSymbolicClass(refc);
            Method m = resolveMethod(refc, Objects.requireNonNull(name), Objects.requireNonNull(type));
            if (m == null) throw noSuchMethod(refc, name, type, REF_invokeStatic);
            if (!Modifier.isStatic(m.getModifiers()))
                throw accessException("expected a static field or method", m, REF_invokeStatic);
            checkAccess(REF_invokeStatic, refc, m);
            return direct(type, REF_invokeStatic, m);
        }

        private static MethodHandle direct(MethodType type, byte kind, Method m) {
            MethodHandle mh = new DirectMethodHandle(type, kind, m.getDeclaringClass(), m.getName(), memberType(m), m);
            return m.isVarArgs() ? mh.asVarargsCollector(type.lastParameterType()) : mh;
        }

        /** Produces a method handle for a virtual method. */
        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            checkSymbolicClass(refc);
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            if (refc == MethodHandle.class) {
                // the invokers of the signature polymorphic methods (findVirtualForMH)
                if ("invoke".equals(name)) return invoker(type);
                if ("invokeExact".equals(name)) return exactInvoker(type);
            }
            byte kind = refc.isInterface() ? REF_invokeInterface : REF_invokeVirtual;
            Method m = resolveMethod(refc, name, type);
            if (m == null) throw noSuchMethod(refc, name, type, kind);
            if (Modifier.isStatic(m.getModifiers()))
                throw accessException("expected a non-static field or method", m, kind);
            checkAccess(kind, refc, m);
            // a private method of a nestmate is invoked non-virtually (jdk26u: invokeSpecial, revealed as
            // invokeVirtual or invokeInterface)
            DirectMethodHandle mh = new DirectMethodHandle(type.insertParameterTypes(0, refc), kind,
                                                           m.getDeclaringClass(), m.getName(), memberType(m), m);
            MethodHandle h = restrictProtectedReceiver(m) ? restrictReceiver(mh, m, kind) : mh;
            return m.isVarArgs() ? h.asVarargsCollector(type.lastParameterType()) : h;
        }

        /** Produces a method handle which creates an object and initializes it, using the constructor of the specified type. */
        public MethodHandle findConstructor(Class<?> refc, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            if (refc.isArray()) {
                throw new NoSuchMethodException("no constructor for array class: " + refc.getName());
            }
            checkSymbolicClass(refc);
            if (type.returnType() != void.class) throw noSuchConstructor(refc, type);
            Constructor<?> found = null;
            for (Constructor<?> c : refc.getDeclaredConstructors()) {
                if (Arrays.equals(c.getParameterTypes(), type.ptypes())) found = c;
            }
            if (found == null || Modifier.isAbstract(refc.getModifiers()) || refc.isInterface())
                throw noSuchConstructor(refc, type);
            checkAccess(REF_newInvokeSpecial, refc, found);
            return constructor(refc, type, found);
        }

        private static MethodHandle constructor(Class<?> refc, MethodType type, Constructor<?> c) {
            MethodHandle mh = new DirectMethodHandle(type.changeReturnType(refc), REF_newInvokeSpecial, refc,
                                                     "<init>", memberType(c), c);
            return c.isVarArgs() ? mh.asVarargsCollector(type.lastParameterType()) : mh;
        }

        /** Looks up a class by name from the lookup context defined by this Lookup object. */
        public Class<?> findClass(String targetName) throws ClassNotFoundException, IllegalAccessException {
            Class<?> targetClass = Class.forName(targetName, false, lookupClass.getClassLoader());
            return accessClass(targetClass);
        }

        /** Ensures that targetClass has been initialized. */
        public Class<?> ensureInitialized(Class<?> targetClass) throws IllegalAccessException {
            if (targetClass.isPrimitive())
                throw new IllegalArgumentException(targetClass + " is a primitive class");
            if (targetClass.isArray())
                throw new IllegalArgumentException(targetClass + " is an array class");
            if (!VerifyAccess.isClassAccessible(targetClass, lookupClass, prevLookupClass, allowedModes)) {
                throw new IllegalAccessException("class is not accessible: " + targetClass.getName() + ", from " + this);
            }
            try {
                Class.forName(targetClass.getName(), true, targetClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new InternalError(e);
            }
            return targetClass;
        }

        /** Determines if a class can be accessed from the lookup context defined by this Lookup object. */
        public Class<?> accessClass(Class<?> targetClass) throws IllegalAccessException {
            Class<?> c = targetClass;
            while (c.isArray()) c = c.getComponentType();
            if (!c.isPrimitive() && allowedModes != TRUSTED
                    && !VerifyAccess.isClassAccessible(c, lookupClass, prevLookupClass, allowedModes)) {
                throw new IllegalAccessException("access violation: " + targetClass + ", from " + this);
            }
            return targetClass;
        }

        /** Produces an early-bound method handle for a virtual method, as if called from an invokespecial in specialCaller. */
        public MethodHandle findSpecial(Class<?> refc, String name, MethodType type,
                                        Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
            checkSpecialCaller(specialCaller, refc);
            checkSymbolicClass(refc);
            Method m = resolveMethod(refc, Objects.requireNonNull(name), Objects.requireNonNull(type));
            if (m == null) throw noSuchMethod(refc, name, type, REF_invokeSpecial);
            if (Modifier.isStatic(m.getModifiers()))
                throw accessException("expected a non-static field or method", m, REF_invokeSpecial);
            checkAccess(REF_invokeSpecial, refc, m);
            checkSpecialReceiver(m, specialCaller);
            return special(type, m, specialCaller);
        }

        /** jdk26u's restrictReceiver for invokeSpecial: the method's class a supertype of the special caller. */
        private void checkSpecialReceiver(Method m, Class<?> specialCaller) throws IllegalAccessException {
            if (allowedModes == TRUSTED) return;
            if (!m.getDeclaringClass().isAssignableFrom(specialCaller))
                throw accessException("caller class must be a subclass below the method", m, REF_invokeSpecial);
        }

        private static MethodHandle special(MethodType type, Method m, Class<?> specialCaller) {
            MethodHandle mh = new DirectMethodHandle(type.insertParameterTypes(0, specialCaller), REF_invokeSpecial,
                                                     m.getDeclaringClass(), m.getName(), memberType(m), m);
            return m.isVarArgs() ? mh.asVarargsCollector(type.lastParameterType()) : mh;
        }

        private void checkSpecialCaller(Class<?> specialCaller, Class<?> refc) throws IllegalAccessException {
            if (allowedModes == TRUSTED)  return;
            if ((refc != null && refc.isInterface()) ? !refc.isAssignableFrom(specialCaller) : false)
                throw new IllegalAccessException("no private access for invokespecial: " + specialCaller.getName() + ", from " + this);
            if (!hasFullPrivilegeAccess()
                || (specialCaller != lookupClass()
                       // ensure non-abstract methods in superinterfaces can be special-invoked
                    && !(refc != null && refc.isInterface() && refc.isAssignableFrom(specialCaller))))
                throw new IllegalAccessException("no private access for invokespecial: " + specialCaller.getName() + ", from " + this);
        }

        private MethodHandle fieldHandle(Class<?> refc, String name, Class<?> type, byte kind)
                throws NoSuchFieldException, IllegalAccessException {
            checkSymbolicClass(refc);
            Field f = resolveField(refc, Objects.requireNonNull(name), Objects.requireNonNull(type));
            if (f == null) throw noSuchField(refc, name, type, kind);
            return fieldHandle(refc, f, kind, true);
        }

        private MethodHandle fieldHandle(Class<?> refc, Field f, byte kind, boolean check) throws IllegalAccessException {
            boolean isStatic = Modifier.isStatic(f.getModifiers());
            boolean wantStatic = (kind == REF_getStatic || kind == REF_putStatic);
            if (isStatic != wantStatic)
                throw accessException(isStatic ? "expected a non-static field or method" : "expected a static field or method", f, kind);
            if (check) checkAccess(kind, refc, f);
            if ((kind == REF_putField || kind == REF_putStatic) && Modifier.isFinal(f.getModifiers()))
                throw accessException("member is final", f, kind);
            Class<?> t = f.getType();
            MethodType mt = switch (kind) {
                case REF_getField -> MethodType.methodType(t, refc);
                case REF_getStatic -> MethodType.methodType(t);
                case REF_putField -> MethodType.methodType(void.class, refc, t);
                default -> MethodType.methodType(void.class, t);
            };
            // MethodHandleInfo's type of a field: ()T for a getter, (T)void for a setter
            MethodType info = (kind == REF_getField || kind == REF_getStatic) ? MethodType.methodType(t)
                                                                           : MethodType.methodType(void.class, t);
            DirectMethodHandle mh = new DirectMethodHandle(mt, kind, f.getDeclaringClass(), f.getName(), info, f);
            if (!isStatic && restrictProtectedReceiver(f)) return restrictReceiver(mh, f, kind);
            return mh;
        }

        /** Produces an early-bound method handle for a non-static method: findVirtual on the receiver's class, bound to it. */
        public MethodHandle bind(Object receiver, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
            Class<? extends Object> refc = receiver.getClass(); // may get NPE
            MethodHandle mh = findVirtual(refc, name, type);
            boolean varargs = mh.isVarargsCollector();
            MethodHandle fixed = mh.asFixedArity();
            if (!fixed.type().parameterType(0).isAssignableFrom(receiver.getClass())) {
                throw new IllegalAccessException("The restricted defining class " +
                                                 fixed.type().parameterType(0).getName() +
                                                 " is not assignable from receiver class " +
                                                 receiver.getClass().getName());
            }
            MethodHandle bound = fixed.bindTo(receiver);
            return varargs ? bound.asVarargsCollector(type.lastParameterType()) : bound;
        }

        /** Produces a method handle giving read access to a non-static field. */
        public MethodHandle findGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return fieldHandle(refc, name, type, REF_getField);
        }

        /** Produces a method handle giving write access to a non-static field. */
        public MethodHandle findSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return fieldHandle(refc, name, type, REF_putField);
        }

        /** Produces a method handle giving read access to a static field. */
        public MethodHandle findStaticGetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return fieldHandle(refc, name, type, REF_getStatic);
        }

        /** Produces a method handle giving write access to a static field. */
        public MethodHandle findStaticSetter(Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            return fieldHandle(refc, name, type, REF_putStatic);
        }

        /** Not in the Go build: VarHandle (J2G-SPEC section 8.5 compiles constant handles statically). */
        public VarHandle findVarHandle(Class<?> recv, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("jrt: VarHandle is not in the Go build (J2G-SPEC 8.5)");
        }

        /** Not in the Go build: VarHandle. */
        public VarHandle findStaticVarHandle(Class<?> decl, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("jrt: VarHandle is not in the Go build (J2G-SPEC 8.5)");
        }

        /** Not in the Go build: VarHandle. */
        public VarHandle unreflectVarHandle(Field f) throws IllegalAccessException {
            throw new UnsupportedOperationException("jrt: VarHandle is not in the Go build (J2G-SPEC 8.5)");
        }

        /**
         * jdk26u's Lookup.defineClass (java-bytecode-1122, doc/classes/BYTECODE-SURVEY.md): the class of the bytes, in the
         * lookup class's package, defined by its loader (arbace.lang.Bytecode.define, by reflection: the Java forms' class-file
         * loader of the program, which the JDK's world cannot name);
         * IllegalAccessException without PACKAGE access, IllegalArgumentException for another package or bytes that are
         * no class file (jdk26u's ClassFile.of().parse failing), as jdk26u's ClassDefiner.
         */
        public Class<?> defineClass(byte[] bytes) throws IllegalAccessException {
            Objects.requireNonNull(bytes);
            if ((lookupModes() & PACKAGE) == 0)
                throw new IllegalAccessException("Lookup does not have PACKAGE access");
            String name = classFileName(bytes).replace('/', '.');
            int i = name.lastIndexOf('.');
            String pn = i < 0 ? "" : name.substring(0, i);
            if (!pn.equals(lookupClass.getPackageName()))
                throw new IllegalArgumentException(name + " not in same package as lookup class");
            try {
                Method define = Class.forName("arbace.lang.Bytecode").getMethod("define", ClassLoader.class, String.class,
                    byte[].class, int.class, int.class, Object.class);
                return (Class<?>) define.invoke(null, lookupClass.getClassLoader(), name, bytes, 0, bytes.length, null);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable t = e.getCause();
                if (t instanceof RuntimeException r) throw r;
                if (t instanceof Error r) throw r;
                throw new IllegalStateException(t);
            } catch (ReflectiveOperationException e) {
                throw new UnsupportedOperationException("Lookup.defineClass: this program has no class-file loader (arbace.lang.Bytecode)", e);
            }
        }

        /** The internal name of a class file's this_class (JVMS 4.1, 4.4), IllegalArgumentException when it has none. */
        private static String classFileName(byte[] b) {
            try {
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(b);
                if (buf.getInt() != 0xCAFEBABE)
                    throw new IllegalArgumentException("Bad magic number");
                buf.getShort();
                buf.getShort();
                int n = buf.getShort() & 0xFFFF;
                int[] offsets = new int[n];
                int[] classes = new int[n];
                for (int k = 1; k < n; k++) {
                    int tag = buf.get() & 0xFF;
                    offsets[k] = buf.position();
                    switch (tag) {
                        case 1 -> buf.position(buf.position() + 2 + (buf.getShort(buf.position()) & 0xFFFF));
                        case 7 -> classes[k] = buf.getShort() & 0xFFFF;
                        case 8, 16, 19, 20 -> buf.getShort();
                        case 15 -> { buf.get(); buf.getShort(); }
                        case 3, 4, 9, 10, 11, 12, 17, 18 -> buf.getInt();
                        case 5, 6 -> { buf.getLong(); k++; }
                        default -> throw new IllegalArgumentException("Bad constant pool tag " + tag);
                    }
                }
                buf.getShort();
                int ti = buf.getShort() & 0xFFFF;
                if (ti <= 0 || ti >= n || classes[ti] <= 0 || classes[ti] >= n)
                    throw new IllegalArgumentException("Bad this_class index " + ti);
                int at = offsets[classes[ti]];
                int len = buf.getShort(at) & 0xFFFF;
                return new String(b, at + 2, len, java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.nio.BufferUnderflowException | IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Truncated class file", e);
            }
        }

        /** Produces a method handle for a reflected method (jdk26u's reference kinds; an accessible one unchecked). */
        public MethodHandle unreflect(Method m) throws IllegalAccessException {
            if (m.getDeclaringClass() == MethodHandle.class) {
                MethodType t = memberType(m);
                if (m.getName().equals("invoke")) return invoker(t).asVarargsCollector(Object[].class);
                if (m.getName().equals("invokeExact")) return exactInvoker(t).asVarargsCollector(Object[].class);
            }
            int mods = m.getModifiers();
            Class<?> refc = m.getDeclaringClass();
            byte kind = Modifier.isStatic(mods) ? REF_invokeStatic
                      : refc.isInterface() ? REF_invokeInterface : REF_invokeVirtual;
            if (!m.isAccessible()) {
                checkSymbolicClass(refc);
                checkAccess(kind, refc, m);
            }
            MethodType type = memberType(m);
            if (kind == REF_invokeStatic) return direct(type, kind, m);
            DirectMethodHandle mh = new DirectMethodHandle(type.insertParameterTypes(0, refc), kind, refc,
                                                           m.getName(), type, m);
            MethodHandle h = (!m.isAccessible() && restrictProtectedReceiver(m)) ? restrictReceiver(mh, m, kind) : mh;
            return m.isVarArgs() ? h.asVarargsCollector(type.lastParameterType()) : h;
        }

        /** Produces a method handle for a reflected method, bypassing an override check in the receiver. */
        public MethodHandle unreflectSpecial(Method m, Class<?> specialCaller) throws IllegalAccessException {
            checkSpecialCaller(specialCaller, m.getDeclaringClass());
            if (Modifier.isStatic(m.getModifiers()))
                throw accessException("expected a non-static field or method", m, REF_invokeSpecial);
            if (!m.isAccessible()) checkAccess(REF_invokeSpecial, m.getDeclaringClass(), m);
            checkSpecialReceiver(m, specialCaller);
            return special(memberType(m), m, specialCaller);
        }

        /** Produces a method handle for a reflected constructor. */
        public MethodHandle unreflectConstructor(Constructor<?> c) throws IllegalAccessException {
            Class<?> refc = c.getDeclaringClass();
            if (!c.isAccessible()) {
                checkSymbolicClass(refc);
                checkAccess(REF_newInvokeSpecial, refc, c);
            }
            return constructor(refc, memberType(c), c);
        }

        /** Produces a method handle giving read access to a reflected field. */
        public MethodHandle unreflectGetter(Field f) throws IllegalAccessException {
            byte kind = Modifier.isStatic(f.getModifiers()) ? REF_getStatic : REF_getField;
            if (!f.isAccessible()) checkSymbolicClass(f.getDeclaringClass());
            return fieldHandle(f.getDeclaringClass(), f, kind, !f.isAccessible());
        }

        /** Produces a method handle giving write access to a reflected field. */
        public MethodHandle unreflectSetter(Field f) throws IllegalAccessException {
            byte kind = Modifier.isStatic(f.getModifiers()) ? REF_putStatic : REF_putField;
            if (!f.isAccessible()) checkSymbolicClass(f.getDeclaringClass());
            return fieldHandle(f.getDeclaringClass(), f, kind, !f.isAccessible());
        }

        /** Cracks a direct method handle created by this lookup object or a similar one. */
        public MethodHandleInfo revealDirect(MethodHandle target) {
            MethodHandle t = target;
            if (t instanceof MethodHandleImpl.AsVarargsCollector v) t = v.asFixedArity();
            if (!(t instanceof DirectMethodHandle d))
                throw newIllegalArgumentException("not a direct method handle");
            byte kind = d.refKind;
            if (kind == REF_invokeSpecial && !hasFullPrivilegeAccess() && allowedModes != TRUSTED)
                throw newIllegalArgumentException("not a direct method handle");
            if (kind == REF_invokeVirtual && d.declaringClass.isInterface()) kind = REF_invokeInterface;
            return new Info(kind, d.declaringClass, d.name, d.memberType, d.member, d.varargs);
        }
    }

    /** jdk26u's InfoFromMemberName: a direct handle's symbolic reference. */
    static final class Info implements MethodHandleInfo {
        private final int kind;
        private final Class<?> declaringClass;
        private final String name;
        private final MethodType type;
        private final Member member;
        private final boolean varargs;

        Info(int kind, Class<?> declaringClass, String name, MethodType type, Member member, boolean varargs) {
            this.kind = kind;
            this.declaringClass = declaringClass;
            this.name = name;
            this.type = type;
            this.member = member;
            this.varargs = varargs;
        }

        @Override public Class<?> getDeclaringClass() { return declaringClass; }
        @Override public String getName() { return name; }
        @Override public MethodType getMethodType() { return type; }
        @Override public int getModifiers() {
            int m = member.getModifiers();
            return varargs ? (m | Modifier.TRANSIENT) : m;
        }
        @Override public int getReferenceKind() { return kind; }

        @Override
        public <T extends Member> T reflectAs(Class<T> expected, Lookup lookup) {
            return expected.cast(member);
        }

        @Override
        public String toString() {
            return MethodHandleInfo.toString(getReferenceKind(), getDeclaringClass(), getName(), getMethodType());
        }
    }

    // ---------------------------------------------------------------------------------------
    // invokers

    /** A method handle to invoke any method handle of type, as by invoke, its trailing arguments from an array. */
    public static MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        if (leadingArgCount < 0 || leadingArgCount > type.parameterCount())
            throw newIllegalArgumentException("bad argument count", leadingArgCount);
        type = type.asSpreaderType(Object[].class, leadingArgCount, type.parameterCount() - leadingArgCount);
        return invoker(type).asSpreader(Object[].class, type.parameterCount() - leadingArgCount);
    }

    /** A method handle which invokes any method handle of type exactly: (MethodHandle, A...)R. */
    public static MethodHandle exactInvoker(MethodType type) {
        MethodType t = type.insertParameterTypes(0, MethodHandle.class);
        int n = type.parameterCount();
        return adapter(t, a -> {
            MethodHandle mh = (MethodHandle) Objects.requireNonNull(a[0]);
            return mh.invokeExactFrom(type, Arrays.copyOfRange(a, 1, n + 1));
        });
    }

    /** A method handle which invokes any method handle of type as by invoke: (MethodHandle, A...)R. */
    public static MethodHandle invoker(MethodType type) {
        MethodType t = type.insertParameterTypes(0, MethodHandle.class);
        int n = type.parameterCount();
        return adapter(t, a -> {
            MethodHandle mh = (MethodHandle) Objects.requireNonNull(a[0]);
            return mh.invokeFrom(type, Arrays.copyOfRange(a, 1, n + 1));
        });
    }

    // ---------------------------------------------------------------------------------------
    // arrays

    /** A method handle constructing arrays of arrayClass: (int)T[]. */
    public static MethodHandle arrayConstructor(Class<?> arrayClass) throws IllegalArgumentException {
        if (!arrayClass.isArray()) throw newIllegalArgumentException("not an array class: " + arrayClass.getName());
        Class<?> elem = arrayClass.getComponentType();
        return adapter(MethodType.methodType(arrayClass, int.class), a -> Array.newInstance(elem, (Integer) a[0]));
    }

    /** A method handle returning the length of an array of arrayClass: (T[])int. */
    public static MethodHandle arrayLength(Class<?> arrayClass) throws IllegalArgumentException {
        if (!arrayClass.isArray()) throw newIllegalArgumentException("not an array class: " + arrayClass.getName());
        return adapter(MethodType.methodType(int.class, arrayClass), a -> Array.getLength(Objects.requireNonNull(a[0])));
    }

    /** A method handle giving read access to elements of an array of arrayClass: (T[], int)T. */
    public static MethodHandle arrayElementGetter(Class<?> arrayClass) throws IllegalArgumentException {
        if (!arrayClass.isArray()) throw newIllegalArgumentException("not an array class: " + arrayClass.getName());
        return adapter(MethodType.methodType(arrayClass.getComponentType(), arrayClass, int.class),
                       a -> Array.get(Objects.requireNonNull(a[0]), (Integer) a[1]));
    }

    /** A method handle giving write access to elements of an array of arrayClass: (T[], int, T)void. */
    public static MethodHandle arrayElementSetter(Class<?> arrayClass) throws IllegalArgumentException {
        if (!arrayClass.isArray()) throw newIllegalArgumentException("not an array class: " + arrayClass.getName());
        return adapter(MethodType.methodType(void.class, arrayClass, int.class, arrayClass.getComponentType()),
                       a -> { Array.set(Objects.requireNonNull(a[0]), (Integer) a[1], a[2]); return null; });
    }

    /** Not in the Go build: VarHandle. */
    public static VarHandle arrayElementVarHandle(Class<?> arrayClass) throws IllegalArgumentException {
        throw new UnsupportedOperationException("jrt: VarHandle is not in the Go build (J2G-SPEC 8.5)");
    }

    /** Not in the Go build: VarHandle. */
    public static VarHandle byteArrayViewVarHandle(Class<?> viewArrayClass, ByteOrder byteOrder) throws IllegalArgumentException {
        throw new UnsupportedOperationException("jrt: VarHandle is not in the Go build (J2G-SPEC 8.5)");
    }

    // ---------------------------------------------------------------------------------------
    // the combinators

    private static RuntimeException misMatchedTypes(String what, Object t1, Object t2) {
        return newIllegalArgumentException(what + " must match: " + t1 + " != " + t2);
    }

    /** Produces a method handle which adapts the type of target to newType by pairwise explicit casts. */
    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        MethodType oldType = target.type();
        if (oldType == newType || oldType.equals(newType)) return target;
        if (oldType.parameterCount() != newType.parameterCount())
            throw new WrongMethodTypeException("cannot explicitly cast " + target + " to " + newType);
        return MethodHandleImpl.makePairwiseConvert(target.asFixedArity(), newType, true);
    }

    /** Produces a method handle which adapts the calling sequence of target to newType by reordering the arguments. */
    public static MethodHandle permuteArguments(MethodHandle target, MethodType newType, int... reorder) {
        reorder = reorder.clone();  // get a private copy
        MethodType oldType = target.type();
        if (reorder.length != oldType.parameterCount())
            throw newIllegalArgumentException("bad reorder array: " + Arrays.toString(reorder));
        if (newType.returnType() != oldType.returnType())
            throw newIllegalArgumentException("return types do not match", oldType, newType);
        for (int j = 0; j < reorder.length; j++) {
            int i = reorder[j];
            if (i < 0 || i >= newType.parameterCount())
                throw newIllegalArgumentException("index is out of bounds for new type", i, newType);
            if (newType.parameterType(i) != oldType.parameterType(j))
                throw newIllegalArgumentException("parameter types do not match after reorder", oldType, newType);
        }
        int[] r = reorder;
        MethodHandle t = target.asFixedArity();
        return adapter(newType, a -> {
            Object[] b = new Object[r.length];
            for (int j = 0; j < r.length; j++) b[j] = a[r[j]];
            return t.invokeImpl(b);
        });
    }

    /** Produces a method handle of the requested return type which returns the given constant value every time. */
    public static MethodHandle constant(Class<?> type, Object value) {
        if (Objects.requireNonNull(type) == void.class)
            throw newIllegalArgumentException("void type");
        Object v;
        if (type.isPrimitive()) {
            // jdk26u: Wrapper.convert, a widening of the value's own primitive
            Class<?> vp = MethodHandleImpl.unwrapped(Objects.requireNonNull(value).getClass());
            if (vp == null || vp == void.class) throw new ClassCastException("Cannot cast " + value.getClass().getName() + " to " + type.getName());
            v = convert(value, Object.class, type, false);
        } else {
            v = type.cast(value);
        }
        return adapter(MethodType.methodType(type), a -> v);
    }

    /** Produces a method handle which returns its sole argument when invoked. */
    public static MethodHandle identity(Class<?> type) {
        if (Objects.requireNonNull(type) == void.class)
            throw newIllegalArgumentException("void type");
        return adapter(MethodType.methodType(type, type), a -> a[0]);
    }

    /** Produces a constant method handle of the requested return type which returns the default value for that type. */
    public static MethodHandle zero(Class<?> type) {
        Objects.requireNonNull(type);
        Object z = MethodHandleImpl.zero(type);
        return adapter(MethodType.methodType(type), a -> z);
    }

    /** Produces a method handle of the requested type which ignores any arguments, does nothing, and returns a suitable default. */
    public static MethodHandle empty(MethodType type) {
        Objects.requireNonNull(type);
        Object z = MethodHandleImpl.zero(type.returnType());
        return adapter(type, a -> z);
    }

    /** Provides a target method handle with one or more bound arguments in advance of the method handle's invocation. */
    public static MethodHandle insertArguments(MethodHandle target, int pos, Object... values) {
        int insCount = values.length;
        MethodType oldType = target.type();
        int outargs = oldType.parameterCount();
        int inargs  = outargs - insCount;
        if (inargs < 0)
            throw newIllegalArgumentException("too many values to insert");
        if (pos < 0 || pos > inargs)
            throw newIllegalArgumentException("no argument type to append");
        Object[] vs = new Object[insCount];
        for (int i = 0; i < insCount; i++) {
            Class<?> ptype = oldType.parameterType(pos + i);
            Object value = values[i];
            if (ptype.isPrimitive()) {
                vs[i] = convert(Objects.requireNonNull(value), Object.class, ptype, false);
            } else {
                vs[i] = ptype.cast(value);
            }
        }
        return MethodHandleImpl.insert(target.asFixedArity(), pos, vs);
    }

    /** Produces a method handle which will discard some dummy arguments before calling target. */
    public static MethodHandle dropArguments(MethodHandle target, int pos, List<Class<?>> valueTypes) {
        return dropArguments(target, pos, valueTypes.toArray(new Class<?>[0]));
    }

    /** Produces a method handle which will discard some dummy arguments before calling target. */
    public static MethodHandle dropArguments(MethodHandle target, int pos, Class<?>... valueTypes) {
        MethodType oldType = target.type();  // get NPE
        int dropped = valueTypes.length;
        for (Class<?> c : valueTypes) {
            if (Objects.requireNonNull(c) == void.class)
                throw newIllegalArgumentException("parameter type cannot be void");
        }
        if (dropped == 0) return target;
        int outargs = oldType.parameterCount();
        int inargs  = outargs + dropped;
        if (pos < 0 || pos > outargs)
            throw newIllegalArgumentException("no argument type to remove"
                    + Arrays.asList(oldType, pos, valueTypes, inargs, outargs));
        MethodType newType = oldType.insertParameterTypes(pos, valueTypes);
        MethodHandle t = target.asFixedArity();
        return adapter(newType, a -> {
            Object[] b = new Object[outargs];
            System.arraycopy(a, 0, b, 0, pos);
            System.arraycopy(a, pos + dropped, b, pos, outargs - pos);
            return t.invokeImpl(b);
        });
    }

    /** Drops the return value of the target handle (if any). */
    public static MethodHandle dropReturn(MethodHandle target) {
        Objects.requireNonNull(target);
        MethodType oldType = target.type();
        Class<?> oldReturnType = oldType.returnType();
        if (oldReturnType == void.class)
            return target;
        MethodType newType = oldType.changeReturnType(void.class);
        MethodHandle t = target.asFixedArity();
        return adapter(newType, a -> { t.invokeImpl(a); return null; });
    }

    /** Adapts a target method handle by pre-processing one or more of its arguments, each with its own unary filter function. */
    public static MethodHandle filterArguments(MethodHandle target, int pos, MethodHandle... filters) {
        MethodType targetType = target.type();
        int maxPos = targetType.parameterCount();
        if (pos + filters.length > maxPos)
            throw newIllegalArgumentException("too many filters");
        MethodType newType = targetType;
        for (int i = 0; i < filters.length; i++) {
            MethodHandle f = filters[i];
            if (f == null) continue;
            MethodType ft = f.type();
            if (ft.parameterCount() != 1 || ft.returnType() != targetType.parameterType(pos + i))
                throw newIllegalArgumentException("target and filter types do not match", targetType, ft);
            newType = newType.changeParameterType(pos + i, ft.parameterType(0));
        }
        MethodHandle[] fs = filters.clone();
        MethodHandle t = target.asFixedArity();
        return adapter(newType, a -> {
            Object[] b = MethodHandleImpl.copy(a);
            for (int i = 0; i < fs.length; i++) {
                if (fs[i] != null) b[pos + i] = fs[i].invokeImpl(new Object[] { a[pos + i] });
            }
            return t.invokeImpl(b);
        });
    }

    /** Adapts a target method handle by pre-processing a sub-sequence of its arguments with a filter. */
    public static MethodHandle collectArguments(MethodHandle target, int pos, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        Class<?> rtype = filterType.returnType();
        int k = filterType.parameterCount();
        if (rtype == void.class) {
            if (pos < 0 || pos > targetType.parameterCount())
                throw newIllegalArgumentException("position is out of range for target", target, pos);
        } else if (pos < 0 || pos >= targetType.parameterCount() || targetType.parameterType(pos) != rtype) {
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
        }
        boolean voidF = (rtype == void.class);
        MethodType newType = voidF ? targetType.insertParameterTypes(pos, filterType.parameterArray())
                                   : targetType.dropParameterTypes(pos, pos + 1).insertParameterTypes(pos, filterType.parameterArray());
        int n = targetType.parameterCount();
        MethodHandle t = target.asFixedArity();
        return adapter(newType, a -> {
            Object r = filter.invokeImpl(Arrays.copyOfRange(a, pos, pos + k));
            Object[] b = new Object[n];
            System.arraycopy(a, 0, b, 0, pos);
            int after = voidF ? pos : pos + 1;
            if (!voidF) b[pos] = r;
            System.arraycopy(a, pos + k, b, after, n - after);
            return t.invokeImpl(b);
        });
    }

    /** Adapts a target method handle by post-processing its return value (if any) with a filter. */
    public static MethodHandle filterReturnValue(MethodHandle target, MethodHandle filter) {
        MethodType targetType = target.type();
        MethodType filterType = filter.type();
        Class<?> rtype = targetType.returnType();
        int filterValues = filterType.parameterCount();
        if (filterValues == 0
                ? (rtype != void.class)
                : (rtype != filterType.parameterType(0) || filterValues != 1))
            throw newIllegalArgumentException("target and filter types do not match", targetType, filterType);
        MethodType newType = targetType.changeReturnType(filterType.returnType());
        MethodHandle t = target.asFixedArity();
        return adapter(newType, a -> {
            Object r = t.invokeImpl(a);
            return filter.invokeImpl(filterValues == 0 ? new Object[0] : new Object[] { r });
        });
    }

    /** Adapts a target method handle by pre-processing some of its arguments, and then calling the target with the result. */
    public static MethodHandle foldArguments(MethodHandle target, MethodHandle combiner) {
        return foldArguments(target, 0, combiner);
    }

    /** Adapts a target method handle by pre-processing some of its arguments, starting at a given position. */
    public static MethodHandle foldArguments(MethodHandle target, int pos, MethodHandle combiner) {
        MethodType targetType  = target.type();
        MethodType combinerType = combiner.type();
        Class<?> rtype = combinerType.returnType();
        boolean voidC = (rtype == void.class);
        int foldVals = voidC ? 0 : 1;
        int afterInsertPos = pos + foldVals;
        int k = combinerType.parameterCount();
        boolean ok = (targetType.parameterCount() >= afterInsertPos + k);
        if (ok) {
            for (int i = 0; i < k; i++) {
                if (combinerType.parameterType(i) != targetType.parameterType(afterInsertPos + i)) { ok = false; break; }
            }
        }
        if (ok && !voidC && rtype != targetType.parameterType(pos))
            ok = false;
        if (!ok)
            throw misMatchedTypes("target and combiner types", targetType, combinerType);
        MethodType newType = voidC ? targetType : targetType.dropParameterTypes(pos, pos + 1);
        int n = targetType.parameterCount();
        MethodHandle t = target.asFixedArity();
        return adapter(newType, a -> {
            Object r = combiner.invokeImpl(Arrays.copyOfRange(a, pos, pos + k));
            if (voidC) return t.invokeImpl(a);
            Object[] b = new Object[n];
            System.arraycopy(a, 0, b, 0, pos);
            b[pos] = r;
            System.arraycopy(a, pos, b, pos + 1, a.length - pos);
            return t.invokeImpl(b);
        });
    }

    /** The first k of a's arguments, k of the handle's arity: a handle that ignores the trailing arguments. */
    private static Object[] prefix(Object[] a, int k) {
        return (k == a.length) ? a : Arrays.copyOf(a, k);
    }

    /** Do types' first k parameters (from skip) equal the target's? (dropArgumentsToMatch's precondition). */
    private static boolean prefixMatches(MethodType h, int skip, MethodType t) {
        int k = h.parameterCount() - skip;
        if (k < 0 || k > t.parameterCount()) return false;
        for (int i = 0; i < k; i++) if (h.parameterType(skip + i) != t.parameterType(i)) return false;
        return true;
    }

    /** Makes a method handle which adapts a target method handle, by guarding it with a test, a boolean-valued method handle. */
    public static MethodHandle guardWithTest(MethodHandle test,
                               MethodHandle target,
                               MethodHandle fallback) {
        MethodType gtype = test.type();
        MethodType ttype = target.type();
        MethodType ftype = fallback.type();
        if (!ttype.equals(ftype))
            throw misMatchedTypes("target and fallback types", ttype, ftype);
        if (gtype.returnType() != boolean.class)
            throw newIllegalArgumentException("guard type is not a predicate "+gtype);
        if (!prefixMatches(gtype, 0, ttype))
            throw misMatchedTypes("target and test types", ttype, gtype);
        int k = gtype.parameterCount();
        MethodHandle t = target.asFixedArity(), f = fallback.asFixedArity();
        return adapter(ttype, a -> (Boolean) test.invokeImpl(prefix(a, k)) ? t.invokeImpl(a) : f.invokeImpl(a));
    }

    /** Makes a method handle which adapts a target method handle, by running it inside an exception handler. */
    public static MethodHandle catchException(MethodHandle target,
                                Class<? extends Throwable> exType,
                                MethodHandle handler) {
        MethodType ttype = target.type();
        MethodType htype = handler.type();
        if (!Throwable.class.isAssignableFrom(exType))
            throw new ClassCastException(exType.getName());
        if (htype.parameterCount() < 1 ||
            !htype.parameterType(0).isAssignableFrom(exType))
            throw newIllegalArgumentException("handler does not accept exception type "+exType);
        if (htype.returnType() != ttype.returnType())
            throw misMatchedTypes("target and handler return types", ttype, htype);
        if (!prefixMatches(htype, 1, ttype))
            throw misMatchedTypes("target and handler types", ttype, htype);
        int k = htype.parameterCount() - 1;
        MethodHandle t = target.asFixedArity(), h = handler.asFixedArity();
        return adapter(ttype, a -> {
            try {
                return t.invokeImpl(a);
            } catch (Throwable e) {
                if (!exType.isInstance(e)) throw e;
                Object[] b = new Object[k + 1];
                b[0] = e;
                System.arraycopy(a, 0, b, 1, k);
                return h.invokeImpl(b);
            }
        });
    }

    /** Produces a method handle which will throw exceptions of the given exType. */
    public static MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exType) {
        if (!Throwable.class.isAssignableFrom(exType))
            throw new ClassCastException(exType.getName());
        return adapter(MethodType.methodType(returnType, exType), a -> {
            throw (Throwable) Objects.requireNonNull(a[0]);
        });
    }

    /** Makes a method handle that adapts a target method handle by wrapping it in a try-finally block. */
    public static MethodHandle tryFinally(MethodHandle target, MethodHandle cleanup) {
        MethodType ttype = target.type();
        Class<?> rtype = ttype.returnType();
        MethodType cleanupType = cleanup.type();
        if (rtype != cleanupType.returnType()) {
            throw misMatchedTypes("target and return types", cleanupType.returnType(), rtype);
        }
        if (!Throwable.class.isAssignableFrom(cleanupType.parameterType(0))) {
            throw misMatchedTypes("cleanup first argument and Throwable", cleanupType, Throwable.class);
        }
        if (rtype != void.class && cleanupType.parameterType(1) != rtype) {
            throw misMatchedTypes("cleanup second argument and target return type", cleanupType, rtype);
        }
        int cleanupArgIndex = rtype == void.class ? 1 : 2;
        if (!prefixMatches(cleanupType, cleanupArgIndex, ttype)) {
            throw misMatchedTypes("cleanup parameters after (Throwable,result) and target parameter list prefix",
                    cleanupType, ttype);
        }
        int k = cleanupType.parameterCount() - cleanupArgIndex;
        Class<?> tt = cleanupType.parameterType(0);
        MethodHandle t = target.asFixedArity(), c = cleanup.asFixedArity();
        return adapter(ttype, a -> {
            Object result = MethodHandleImpl.zero(rtype);
            Throwable thrown = null;
            try {
                result = t.invokeImpl(a);
            } catch (Throwable e) {
                thrown = e;
            }
            if (thrown != null && !tt.isInstance(thrown)) throw new ClassCastException(thrown.getClass().getName());
            Object[] b = new Object[cleanupArgIndex + k];
            b[0] = thrown;
            if (cleanupArgIndex == 2) b[1] = result;
            System.arraycopy(a, 0, b, cleanupArgIndex, k);
            Object r = c.invokeImpl(b);
            if (thrown != null) throw thrown;
            return r;
        });
    }
}
