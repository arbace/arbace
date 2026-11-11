/*
 * Arbace's own runner of jtreg's framework actions (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a),
 * "As built: JCS330-JCS333"): `@run testng C` and `@run junit C` run as `Runner testng C` and `Runner junit C`,
 * on the JDK (bin/java-check --record-ref) and on Arbace alike. Written clean-room from the frameworks'
 * documented behaviour (TestNG's and JUnit 5's user guides), not from their sources: it finds the class's test
 * methods and lifecycle methods by their annotations' names (the annotation types are the test's own source
 * set's, test/java/runner/org/testng and org/junit, so that this class names none of them and a test compiles
 * only what it imports), runs them in the frameworks' orders, and fails (an exception out of main, status 1)
 * when a test method or a configuration method failed or no test ran: jtreg's rule, the test's own verdict.
 */
package org.arbace.jtreg;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class Runner {
    private int run, passed, failed, skipped, configFailed;

    public static void main(String[] args) throws Throwable {
        if (args.length != 2 || !(args[0].equals("testng") || args[0].equals("junit")))
            throw new IllegalArgumentException("usage: Runner testng|junit CLASS");
        Class<?> c = Class.forName(args[1]);
        Runner r = new Runner();
        if (args[0].equals("testng")) r.testng(c); else r.junit(c);
        System.out.println("===============================================");
        System.out.println(args[0] + ": " + c.getName());
        System.out.println("Total tests run: " + r.run + ", Passes: " + r.passed + ", Failures: " + r.failed
                           + ", Skips: " + r.skipped + ", Configuration failures: " + r.configFailed);
        System.out.println("===============================================");
        if (r.failed > 0 || r.configFailed > 0)
            throw new AssertionError("framework run failed: " + r.failed + " test failures, " + r.configFailed + " configuration failures");
        if (r.run == 0)
            throw new AssertionError("framework run failed: no test ran");
    }

    // ------------------------------------------------------------------------------------------
    // annotations by name, their elements by reflection

    // (Method's and Class's apart, as the check's world has no AnnotatedElement; a method's annotations are its
    // declared ones, which the world answers)
    static Annotation ann(Method e, String name) { return named(e.getDeclaredAnnotations(), name); }
    static Annotation ann(Class<?> e, String name) { return named(e.getAnnotations(), name); }

    static Annotation named(Annotation[] as, String name) {
        for (Annotation a : as)
            if (a.annotationType().getName().equals(name)) return a;
        return null;
    }

    static Object attr(Annotation a, String element) {
        try {
            Method m = a.annotationType().getMethod(element);
            return m.invoke(a);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String str(Annotation a, String element) { Object v = attr(a, element); return v == null ? "" : (String) v; }
    static int integer(Annotation a, String element, int dflt) { Object v = attr(a, element); return v == null ? dflt : ((Integer) v).intValue(); }
    static boolean bool(Annotation a, String element, boolean dflt) { Object v = attr(a, element); return v == null ? dflt : ((Boolean) v).booleanValue(); }
    static String[] strs(Annotation a, String element) { Object v = attr(a, element); return v == null ? new String[0] : (String[]) v; }

    static Throwable cause(Throwable t) {
        while (t instanceof InvocationTargetException && t.getCause() != null) t = t.getCause();
        return t;
    }

    static boolean isNamed(Throwable t, String name) {
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass())
            if (c.getName().equals(name)) return true;
        return false;
    }

    // the class and its superclasses, the class's own first (Object left out)
    static List<Class<?>> hierarchy(Class<?> c) {
        List<Class<?>> l = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) l.add(k);
        return l;
    }

    static String sig(Method m) { return m.getName() + Arrays.toString(m.getParameterTypes()); }

    // the methods of C's hierarchy annotated NAME, an overridden one once (the subclass's); superclass's first
    // when topDown, else the class's own first
    static List<Method> annotated(Class<?> c, String name, boolean topDown) {
        List<Class<?>> h = hierarchy(c);
        Set<String> seen = new HashSet<>();
        List<List<Method>> per = new ArrayList<>();
        for (Class<?> k : h) {
            List<Method> ms = new ArrayList<>();
            Method[] ds = k.getDeclaredMethods();
            Arrays.sort(ds, Comparator.comparing(Runner::sig));
            for (Method m : ds) {
                if (m.isSynthetic() || m.isBridge()) continue;
                String s = sig(m);
                if (!Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && seen.contains(s)) continue;
                seen.add(s);
                if (ann(m, name) != null) ms.add(m);
            }
            per.add(ms);
        }
        if (topDown) Collections.reverse(per);
        List<Method> out = new ArrayList<>();
        for (List<Method> ms : per) out.addAll(ms);
        return out;
    }

    static Object invoke(Method m, Object target, Object... args) throws Throwable {
        m.setAccessible(true);
        try {
            return m.invoke(Modifier.isStatic(m.getModifiers()) ? null : target, args);
        } catch (InvocationTargetException e) {
            throw cause(e);
        }
    }

    static Object newInstance(Class<?> c) throws Throwable {
        java.lang.reflect.Constructor<?> k = c.getDeclaredConstructor();
        k.setAccessible(true);
        try {
            return k.newInstance();
        } catch (InvocationTargetException e) {
            throw cause(e);
        }
    }

    // an implementation class of the API's (a nested Impl of the interface NAME), when the test's source set has it
    static Object impl(String name, Class<?>[] types, Object... args) {
        try {
            java.lang.reflect.Constructor<?> k = Class.forName(name + "$Impl").getDeclaredConstructor(types);
            k.setAccessible(true);
            return k.newInstance(args);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void report(String what, String result, Throwable t) {
        System.out.println("test " + what + ": " + result);
        if (t != null) {
            System.out.println("   " + t);
            t.printStackTrace(System.out);
        }
    }

    static String describe(Method m, Object[] args) {
        StringBuilder b = new StringBuilder(m.getDeclaringClass().getName()).append('.').append(m.getName()).append('(');
        if (args != null)
            for (int i = 0; i < args.length; i++) {
                if (i > 0) b.append(", ");
                Object a = args[i];
                String s = a == null ? "null" : a.getClass().isArray() ? a.getClass().getSimpleName() : String.valueOf(a);
                b.append(s.length() > 80 ? s.substring(0, 80) + "..." : s);
            }
        return b.append(')').toString();
    }

    // ------------------------------------------------------------------------------------------
    // TestNG (its documentation: the annotations, the configuration methods' order, data providers,
    // expectedExceptions, priority and dependsOnMethods; methods otherwise by name)

    static final String NG = "org.testng.annotations.";
    final Map<String, Object> ngAttributes = new HashMap<>();

    Object ngContext() {
        return impl("org.testng.ITestContext", new Class<?>[] {Map.class}, ngAttributes);
    }

    // a configuration or test method's arguments by its parameters' types (TestNG's native injection)
    Object[] ngInject(Method m, Method test, Object[] params, Object result) {
        Class<?>[] ts = m.getParameterTypes();
        Object[] out = new Object[ts.length];
        for (int i = 0; i < ts.length; i++) {
            String n = ts[i].getName();
            if (ts[i] == Method.class) out[i] = test;
            else if (ts[i] == Object[].class) out[i] = params == null ? new Object[0] : params;
            else if (n.equals("org.testng.ITestContext")) out[i] = ngContext();
            else if (n.equals("org.testng.ITestResult")) out[i] = result;
            else throw new IllegalArgumentException("TestNG's injection: no value for the parameter " + n + " of " + m);
        }
        return out;
    }

    boolean ngConfig(List<Method> ms, Object inst, Method test, Object[] params, Object result) {
        for (Method m : ms) {
            if (!bool(ngConfigAnn(m), "enabled", true)) continue;
            try {
                invoke(m, inst, ngInject(m, test, params, result));
            } catch (Throwable t) {
                if (isNamed(t, "org.testng.SkipException")) {
                    report("configuration " + m.getName(), "skipped", t);
                } else {
                    configFailed++;
                    report("configuration " + m.getName(), "failure", t);
                }
                return false;
            }
        }
        return true;
    }

    // the test's @Test: its own, else its class's (a class-level @Test makes the public methods tests)
    static Annotation ngTest(Method m) {
        Annotation a = ann(m, NG + "Test");
        if (a == null) a = ann(m.getDeclaringClass(), NG + "Test");
        return a;
    }

    static Annotation ngConfigAnn(Method m) {
        for (Annotation a : m.getDeclaredAnnotations())
            if (a.annotationType().getName().startsWith(NG)) return a;
        return null;
    }

    static boolean isNgConfig(Method m) {
        for (String n : new String[] {"BeforeSuite", "AfterSuite", "BeforeTest", "AfterTest", "BeforeClass", "AfterClass",
                                      "BeforeMethod", "AfterMethod", "BeforeGroups", "AfterGroups", "DataProvider", "Factory"})
            if (ann(m, NG + n) != null) return true;
        return false;
    }

    List<Method> ngTests(Class<?> c) {
        Map<String, Method> found = new LinkedHashMap<>();
        for (Class<?> k : hierarchy(c)) {
            boolean classLevel = ann(k, NG + "Test") != null;
            for (Method m : k.getDeclaredMethods()) {
                if (m.isSynthetic() || m.isBridge() || found.containsKey(sig(m))) continue;
                boolean t = ann(m, NG + "Test") != null
                    || (classLevel && Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !isNgConfig(m));
                if (t) found.put(sig(m), m);
                else if (!Modifier.isPrivate(m.getModifiers())) found.put(sig(m), null);
            }
        }
        List<Method> l = new ArrayList<>();
        for (Method m : found.values())
            if (m != null && bool(ngTest(m), "enabled", true)) l.add(m);
        // priority, then the name (TestNG's order of the methods of one class without dependencies)
        l.sort(Comparator.<Method>comparingInt(m -> integer(ngTest(m), "priority", 0)).thenComparing(Method::getName).thenComparing(Runner::sig));
        return l;
    }

    // a data provider's rows: Object[][], Object[] (one parameter), Iterator<Object[]> or Iterator<Object>
    List<Object[]> ngRows(Method test, Object inst, Annotation t) throws Throwable {
        String name = str(t, "dataProvider");
        Object pc = attr(t, "dataProviderClass");
        Class<?> c = pc == null || pc == Object.class ? inst.getClass() : (Class<?>) pc;
        Method dp = null;
        for (Method m : annotated(c, NG + "DataProvider", false)) {
            String n = str(ann(m, NG + "DataProvider"), "name");
            if ((n.isEmpty() ? m.getName() : n).equals(name)) { dp = m; break; }
        }
        if (dp == null) throw new IllegalArgumentException("no data provider " + name + " for " + test);
        Object target = Modifier.isStatic(dp.getModifiers()) ? null : c.isInstance(inst) ? inst : newInstance(c);
        Object v = invoke(dp, target, ngInject(dp, test, null, null));
        List<Object[]> rows = new ArrayList<>();
        if (v instanceof Object[][]) for (Object[] r : (Object[][]) v) rows.add(r);
        else if (v instanceof Object[]) for (Object r : (Object[]) v) rows.add(new Object[] {r});
        else if (v instanceof Iterator) {
            for (Iterator<?> i = (Iterator<?>) v; i.hasNext();) {
                Object r = i.next();
                rows.add(r instanceof Object[] ? (Object[]) r : new Object[] {r});
            }
        } else if (v instanceof Iterable) {
            for (Object r : (Iterable<?>) v) rows.add(r instanceof Object[] ? (Object[]) r : new Object[] {r});
        } else throw new IllegalArgumentException("data provider " + dp + " returned " + v);
        return rows;
    }

    void testng(Class<?> c) throws Throwable {
        if (!annotated(c, NG + "Factory", false).isEmpty()) {
            configFailed++;
            report(c.getName(), "failure", new UnsupportedOperationException("TestNG's @Factory (not in the check's runner)"));
            return;
        }
        Object inst;
        try {
            inst = newInstance(c);
        } catch (Throwable t) {
            configFailed++;
            report(c.getName() + ".<init>", "failure", t);
            return;
        }
        List<Method> tests = ngTests(c);
        List<Method> beforeMethod = annotated(c, NG + "BeforeMethod", true), afterMethod = annotated(c, NG + "AfterMethod", false);
        List<Method> afterAll = new ArrayList<>();
        afterAll.addAll(annotated(c, NG + "AfterClass", false));
        afterAll.addAll(annotated(c, NG + "AfterTest", false));
        afterAll.addAll(annotated(c, NG + "AfterSuite", false));
        boolean ok = ngConfig(annotated(c, NG + "BeforeSuite", true), inst, null, null, null)
            && ngConfig(annotated(c, NG + "BeforeTest", true), inst, null, null, null)
            && ngConfig(annotated(c, NG + "BeforeClass", true), inst, null, null, null);
        Map<String, Boolean> done = new HashMap<>(); // method name -> passed
        List<Method> left = new ArrayList<>(tests);
        while (!left.isEmpty()) {
            // the first in order whose dependencies ran (dependsOnMethods, dependsOnGroups)
            Method next = null;
            for (Method m : left) {
                boolean ready = true;
                for (String d : strs(ngTest(m), "dependsOnMethods"))
                    for (Method o : left) if (o != m && o.getName().equals(d.substring(d.lastIndexOf('.') + 1))) ready = false;
                for (String g : strs(ngTest(m), "dependsOnGroups"))
                    for (Method o : left) if (o != m && Arrays.asList(strs(ngTest(o), "groups")).contains(g)) ready = false;
                if (ready) { next = m; break; }
            }
            if (next == null) next = left.get(0); // a cycle: TestNG refuses it; run in order
            left.remove(next);
            Annotation t = ngTest(next);
            boolean depsOk = true;
            for (String d : strs(t, "dependsOnMethods")) {
                Boolean p = done.get(d.substring(d.lastIndexOf('.') + 1));
                if (p == null || !p) depsOk = false;
            }
            for (String g : strs(t, "dependsOnGroups"))
                for (Method o : tests)
                    if (Arrays.asList(strs(ngTest(o), "groups")).contains(g) && !Boolean.TRUE.equals(done.get(o.getName()))) depsOk = false;
            if (!ok || (!depsOk && !bool(t, "alwaysRun", false))) {
                run++; skipped++;
                report(describe(next, null), "skipped", null);
                done.put(next.getName(), false);
                continue;
            }
            List<Object[]> rows;
            if (str(t, "dataProvider").isEmpty()) rows = Collections.singletonList(new Object[0]);
            else {
                try {
                    rows = ngRows(next, inst, t);
                } catch (Throwable e) {
                    run++; failed++;
                    report(describe(next, null) + " (its data provider)", "failure", e);
                    done.put(next.getName(), false);
                    continue;
                }
            }
            boolean all = true;
            int count = Math.max(1, integer(t, "invocationCount", 1));
            for (Object[] row : rows)
                for (int i = 0; i < count; i++)
                    all &= ngRun(next, inst, t, row, beforeMethod, afterMethod);
            done.put(next.getName(), all);
        }
        ngConfig(afterAll, inst, null, null, null);
    }

    boolean ngRun(Method m, Object inst, Annotation t, Object[] row, List<Method> before, List<Method> after) {
        run++;
        String what = describe(m, row);
        Object result = impl("org.testng.ITestResult", new Class<?>[] {String.class, Object[].class, Object.class}, m.getName(), row, inst);
        if (!ngConfig(before, inst, m, row, result)) {
            skipped++;
            report(what, "skipped", null);
            return false;
        }
        Throwable thrown = null;
        try {
            invoke(m, inst, ngArgs(m, row, result));
        } catch (Throwable e) {
            thrown = e;
        }
        Object[] ex = (Object[]) attr(t, "expectedExceptions");
        Throwable failure = thrown;
        if (ex != null && ex.length > 0) {
            failure = null;
            if (thrown == null) failure = new AssertionError("expected one of " + Arrays.toString(ex) + " but nothing was thrown");
            else {
                boolean match = false;
                for (Object k : ex) if (((Class<?>) k).isInstance(thrown)) match = true;
                String re = str(t, "expectedExceptionsMessageRegExp");
                if (match && !re.isEmpty() && !re.equals(".*")) {
                    String msg = thrown.getMessage() == null ? "" : thrown.getMessage();
                    match = Pattern.compile(re, Pattern.DOTALL).matcher(msg).matches();
                }
                if (!match) failure = thrown;
            }
        }
        boolean pass;
        if (failure != null && isNamed(failure, "org.testng.SkipException")) {
            skipped++; pass = false;
            report(what, "skipped", failure);
        } else if (failure != null) {
            failed++; pass = false;
            report(what, "failure", failure);
        } else {
            passed++; pass = true;
            report(what, "success", null);
        }
        setStatus(result, pass ? 1 : failure != null && isNamed(failure, "org.testng.SkipException") ? 3 : 2, failure);
        ngConfig(after, inst, m, row, result);
        return pass;
    }

    // a test's arguments: its data provider's row, the injected parameters (Method, ITestContext, ITestResult)
    // at their places
    Object[] ngArgs(Method m, Object[] row, Object result) {
        Class<?>[] ts = m.getParameterTypes();
        Object[] out = new Object[ts.length];
        int j = 0;
        for (int i = 0; i < ts.length; i++) {
            String n = ts[i].getName();
            if (ts[i] == Method.class) out[i] = m;
            else if (n.equals("org.testng.ITestContext")) out[i] = ngContext();
            else if (n.equals("org.testng.ITestResult")) out[i] = result;
            else if (j < row.length) out[i] = row[j++];
            else throw new IllegalArgumentException("the data provider gave " + row.length + " values for " + m);
        }
        if (j < row.length) throw new IllegalArgumentException("the data provider gave " + row.length + " values for " + m);
        return out;
    }

    static void setStatus(Object result, int status, Throwable t) {
        if (result == null) return;
        try {
            Method s = result.getClass().getDeclaredMethod("setStatus", int.class, Throwable.class);
            s.setAccessible(true);
            s.invoke(result, status, t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // JUnit 5 (its user guide: @Test, @ParameterizedTest with its sources, @RepeatedTest, the lifecycle
    // methods' order, @TestInstance, @TestMethodOrder, @Disabled, assumptions)

    static final String JU = "org.junit.jupiter.api.";
    static final String JP = "org.junit.jupiter.params.";

    static boolean isJuTest(Method m) {
        return ann(m, JU + "Test") != null || ann(m, JP + "ParameterizedTest") != null || ann(m, JU + "RepeatedTest") != null
            || ann(m, JU + "TestFactory") != null || ann(m, JU + "TestTemplate") != null;
    }

    List<Method> juTests(Class<?> c) {
        Map<String, Method> found = new LinkedHashMap<>();
        for (Class<?> k : hierarchy(c))
            for (Method m : k.getDeclaredMethods()) {
                if (m.isSynthetic() || m.isBridge() || found.containsKey(sig(m))) continue;
                if (isJuTest(m) && !Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) found.put(sig(m), m);
                else if (!Modifier.isPrivate(m.getModifiers())) found.put(sig(m), null);
            }
        List<Method> l = new ArrayList<>();
        for (Method m : found.values()) if (m != null) l.add(m);
        // the default order: deterministic, by the name's hash code, then the signature (JUnit's documented
        // "deterministic but intentionally nonobvious"); @TestMethodOrder's MethodName and OrderAnnotation
        l.sort(Comparator.<Method>comparingInt(m -> m.getName().hashCode()).thenComparing(Runner::sig));
        Annotation ord = null;
        for (Class<?> k : hierarchy(c)) if ((ord = ann(k, JU + "TestMethodOrder")) != null) break;
        if (ord != null) {
            String o = ((Class<?>) attr(ord, "value")).getName();
            if (o.endsWith("$MethodName")) l.sort(Comparator.comparing(Method::getName).thenComparing(Runner::sig));
            else if (o.endsWith("$OrderAnnotation"))
                l.sort(Comparator.comparingInt(m -> { Annotation a = ann(m, JU + "Order"); return a == null ? 1073741823 : integer(a, "value", 1073741823); }));
            else if (o.endsWith("$DisplayName"))
                l.sort(Comparator.comparing(m -> { Annotation a = ann(m, JU + "DisplayName"); return a == null ? m.getName() + "(" + ")" : str(a, "value"); }));
        }
        return l;
    }

    // a lifecycle method's or a test's extra arguments (TestInfo; the rest JUnit's extensions resolve, not here)
    Object juParam(Class<?> t, Method test, Class<?> c, String display) {
        if (t.getName().equals(JU + "TestInfo"))
            return impl(JU + "TestInfo", new Class<?>[] {String.class, Class.class, Method.class}, display, c, test);
        throw new IllegalArgumentException("no parameter resolver for " + t.getName() + " (JUnit's extensions are not in the check's runner)");
    }

    boolean juLifecycle(List<Method> ms, Object inst, Method test, Class<?> c, String display, boolean container) {
        for (Method m : ms) {
            try {
                Class<?>[] ts = m.getParameterTypes();
                Object[] args = new Object[ts.length];
                for (int i = 0; i < ts.length; i++) args[i] = juParam(ts[i], test, c, display);
                invoke(m, inst, args);
            } catch (Throwable t) {
                if (isNamed(t, "org.opentest4j.TestAbortedException")) {
                    report("lifecycle " + m.getName(), "aborted", t);
                } else {
                    if (container) configFailed++;
                    report("lifecycle " + m.getName(), "failure", t);
                }
                return false;
            }
        }
        return true;
    }

    void junit(Class<?> c) throws Throwable {
        // JUnit 4's tests (org.junit.Test), which jtreg's @run junit runs too
        for (Class<?> k : hierarchy(c))
            for (Method m : k.getDeclaredMethods())
                if (ann(m, J4 + "Test") != null) { junit4(c); return; }
        Annotation ti = null;
        for (Class<?> k : hierarchy(c)) if ((ti = ann(k, JU + "TestInstance")) != null) break;
        boolean perClass = ti != null && String.valueOf(attr(ti, "value")).equals("PER_CLASS");
        if (ann(c, JU + "Disabled") != null) {
            report(c.getName(), "disabled", null);
            run++; skipped++;
            return;
        }
        Object shared = null;
        if (perClass) {
            try {
                shared = newInstance(c);
            } catch (Throwable t) {
                configFailed++;
                report(c.getName() + ".<init>", "failure", t);
                return;
            }
        }
        List<Method> tests = juTests(c);
        List<Method> beforeEach = annotated(c, JU + "BeforeEach", true), afterEach = annotated(c, JU + "AfterEach", false);
        boolean ok = juLifecycle(annotated(c, JU + "BeforeAll", true), shared, null, c, c.getSimpleName(), true);
        for (Method m : tests) {
            if (!ok || ann(m, JU + "Disabled") != null) {
                run++; skipped++;
                report(describe(m, null), ok ? "disabled" : "skipped", null);
                continue;
            }
            List<Object[]> rows;
            try {
                rows = juRows(m, c, shared);
            } catch (Throwable t) {
                run++; failed++;
                report(describe(m, null) + " (its arguments)", "failure", t);
                continue;
            }
            for (Object[] row : rows) juRun(m, c, shared, row, beforeEach, afterEach);
        }
        juLifecycle(annotated(c, JU + "AfterAll", false), shared, null, c, c.getSimpleName(), true);
    }

    void juRun(Method m, Class<?> c, Object shared, Object[] row, List<Method> before, List<Method> after) {
        run++;
        String what = describe(m, row);
        String display = m.getName() + "()";
        Object inst = shared;
        if (inst == null) {
            try {
                inst = newInstance(c);
            } catch (Throwable t) {
                failed++;
                report(what, "failure", t);
                return;
            }
        }
        Throwable failure = null;
        if (!juLifecycle(before, inst, m, c, display, false)) failure = new AssertionError("a @BeforeEach method failed");
        if (failure == null) {
            try {
                Class<?>[] ts = m.getParameterTypes();
                Object[] args = new Object[ts.length];
                for (int i = 0; i < ts.length; i++)
                    args[i] = i < row.length ? convert(row[i], ts[i]) : juParam(ts[i], m, c, display);
                invoke(m, inst, args);
            } catch (Throwable t) {
                failure = t;
            }
        }
        if (!juLifecycle(after, inst, m, c, display, false) && failure == null) failure = new AssertionError("an @AfterEach method failed");
        if (failure != null && isNamed(failure, "org.opentest4j.TestAbortedException")) {
            skipped++;
            report(what, "aborted", failure);
        } else if (failure != null) {
            failed++;
            report(what, "failure", failure);
        } else {
            passed++;
            report(what, "success", null);
        }
    }

    // ------------------------------------------------------------------------------------------
    // JUnit 4 (its API documentation: public void @Test methods, an instance each, @Before/@After, the static
    // @BeforeClass/@AfterClass, expected, @Ignore; JUnit 4's default order, deterministic, by the name's hash code)

    static final String J4 = "org.junit.";

    void junit4(Class<?> c) throws Throwable {
        if (ann(c, J4 + "Ignore") != null) {
            run++; skipped++;
            report(c.getName(), "ignored", null);
            return;
        }
        List<Method> tests = new ArrayList<>();
        for (Method m : c.getMethods()) if (ann(m, J4 + "Test") != null) tests.add(m);
        tests.sort(Comparator.<Method>comparingInt(m -> m.getName().hashCode()).thenComparing(Runner::sig));
        List<Method> before = annotated(c, J4 + "Before", true), after = annotated(c, J4 + "After", false);
        boolean ok = juLifecycle(annotated(c, J4 + "BeforeClass", true), null, null, c, c.getSimpleName(), true);
        for (Method m : tests) {
            run++;
            String what = describe(m, null);
            if (!ok || ann(m, J4 + "Ignore") != null) {
                skipped++;
                report(what, ok ? "ignored" : "skipped", null);
                continue;
            }
            Throwable failure = null;
            Object inst = null;
            try {
                inst = newInstance(c);
            } catch (Throwable t) {
                failure = t;
            }
            if (failure == null && !juLifecycle(before, inst, m, c, m.getName(), false)) failure = new AssertionError("a @Before method failed");
            if (failure == null) {
                Class<?> ex = (Class<?>) attr(ann(m, J4 + "Test"), "expected");
                boolean none = ex == null || ex.getName().equals(J4 + "Test$None");
                try {
                    invoke(m, inst);
                    if (!none) failure = new AssertionError("Expected exception: " + ex.getName());
                } catch (Throwable t) {
                    if (none || !ex.isInstance(t)) failure = t;
                }
            }
            if (inst != null && !juLifecycle(after, inst, m, c, m.getName(), false) && failure == null) failure = new AssertionError("an @After method failed");
            if (failure != null) {
                failed++;
                report(what, "failure", failure);
            } else {
                passed++;
                report(what, "success", null);
            }
        }
        juLifecycle(annotated(c, J4 + "AfterClass", false), null, null, c, c.getSimpleName(), true);
    }

    // a test's invocations' arguments: none (@Test), N empty ones (@RepeatedTest), its sources' (@ParameterizedTest)
    List<Object[]> juRows(Method m, Class<?> c, Object shared) throws Throwable {
        List<Object[]> rows = new ArrayList<>();
        Annotation rep = ann(m, JU + "RepeatedTest");
        if (rep != null) {
            for (int i = 0; i < integer(rep, "value", 1); i++) rows.add(new Object[0]);
            return rows;
        }
        if (ann(m, JU + "TestFactory") != null || ann(m, JU + "TestTemplate") != null)
            throw new UnsupportedOperationException("JUnit's @TestFactory and @TestTemplate (not in the check's runner)");
        if (ann(m, JP + "ParameterizedTest") == null) {
            rows.add(new Object[0]);
            return rows;
        }
        Annotation a;
        if ((a = ann(m, JP + "provider.NullSource")) != null || ann(m, JP + "provider.NullAndEmptySource") != null)
            rows.add(new Object[] {null});
        if ((a = ann(m, JP + "provider.EmptySource")) != null || ann(m, JP + "provider.NullAndEmptySource") != null)
            rows.add(new Object[] {empty(m.getParameterTypes()[0])});
        if ((a = ann(m, JP + "provider.ValueSource")) != null) {
            for (String e : new String[] {"shorts", "bytes", "ints", "longs", "floats", "doubles", "chars", "booleans", "strings", "classes"}) {
                Object v = attr(a, e);
                if (v != null)
                    for (int i = 0; i < Array.getLength(v); i++) rows.add(new Object[] {Array.get(v, i)});
            }
        }
        if ((a = ann(m, JP + "provider.CsvSource")) != null) {
            String d = String.valueOf(attr(a, "delimiterString") == null ? "" : attr(a, "delimiterString"));
            if (d.isEmpty()) d = String.valueOf(attr(a, "delimiter") == null ? ',' : attr(a, "delimiter"));
            if (d.equals("\0")) d = ",";
            for (String line : strs(a, "value")) rows.add(csv(line, d));
            for (String line : str(a, "textBlock").split("\n"))
                if (!line.isBlank() && !line.strip().startsWith("#")) rows.add(csv(line, d));
        }
        if ((a = ann(m, JP + "provider.EnumSource")) != null) {
            Class<?> e = (Class<?>) attr(a, "value");
            if (e == null || e.getName().endsWith("$NullEnum")) e = m.getParameterTypes()[0];
            List<String> names = Arrays.asList(strs(a, "names"));
            String mode = String.valueOf(attr(a, "mode"));
            for (Object k : e.getEnumConstants()) {
                boolean in = names.isEmpty() || names.contains(((Enum<?>) k).name());
                if (mode.equals("EXCLUDE")) in = names.isEmpty() || !names.contains(((Enum<?>) k).name());
                if (in) rows.add(new Object[] {k});
            }
        }
        if ((a = ann(m, JP + "provider.MethodSource")) != null) {
            String[] names = strs(a, "value");
            if (names.length == 0) names = new String[] {""};
            for (String n : names) {
                Class<?> k = c;
                String mn = n.isEmpty() ? m.getName() : n;
                if (mn.contains("#")) {
                    k = Class.forName(mn.substring(0, mn.indexOf('#')));
                    mn = mn.substring(mn.indexOf('#') + 1);
                }
                if (mn.endsWith("()")) mn = mn.substring(0, mn.length() - 2);
                Method src = null;
                for (Class<?> h : hierarchy(k)) {
                    for (Method x : h.getDeclaredMethods()) if (x.getName().equals(mn) && x.getParameterCount() == 0) { src = x; break; }
                    if (src != null) break;
                }
                if (src == null) throw new IllegalArgumentException("no factory method " + n + " for " + m);
                Object inst = Modifier.isStatic(src.getModifiers()) ? null : shared != null ? shared : newInstance(c);
                for (Object e : elements(invoke(src, inst))) rows.add(arguments(e, m.getParameterCount()));
            }
        }
        if ((a = ann(m, JP + "provider.FieldSource")) != null) {
            String[] names = strs(a, "value");
            if (names.length == 0) names = new String[] {""};
            for (String n : names) {
                Class<?> k = c;
                String fn = n.isEmpty() ? m.getName() : n;
                if (fn.contains("#")) {
                    k = Class.forName(fn.substring(0, fn.indexOf('#')));
                    fn = fn.substring(fn.indexOf('#') + 1);
                }
                java.lang.reflect.Field f = null;
                for (Class<?> h : hierarchy(k)) {
                    try { f = h.getDeclaredField(fn); break; } catch (NoSuchFieldException e) { }
                }
                if (f == null) throw new IllegalArgumentException("no field " + n + " for " + m);
                f.setAccessible(true);
                Object v = f.get(Modifier.isStatic(f.getModifiers()) ? null : shared != null ? shared : newInstance(c));
                if (v instanceof java.util.function.Supplier) v = ((java.util.function.Supplier<?>) v).get();
                for (Object e : elements(v)) rows.add(arguments(e, m.getParameterCount()));
            }
        }
        if (rows.isEmpty())
            throw new IllegalArgumentException("a @ParameterizedTest with no source of arguments the check's runner knows: " + Arrays.toString(m.getDeclaredAnnotations()));
        return rows;
    }

    static Object empty(Class<?> t) {
        if (t == String.class) return "";
        if (t.isArray()) return Array.newInstance(t.getComponentType(), 0);
        if (t == List.class || t == java.util.Collection.class) return new ArrayList<>();
        if (t == Set.class) return new HashSet<>();
        if (t == Map.class) return new HashMap<>();
        throw new IllegalArgumentException("@EmptySource for " + t);
    }

    // a factory method's elements: a Stream (or a primitive one), an Iterable, an Iterator or an array
    static List<Object> elements(Object v) {
        List<Object> out = new ArrayList<>();
        if (v instanceof java.util.stream.BaseStream) {
            for (Iterator<?> i = ((java.util.stream.BaseStream<?, ?>) v).iterator(); i.hasNext();) out.add(i.next());
        } else if (v instanceof Iterable) {
            for (Object e : (Iterable<?>) v) out.add(e);
        } else if (v instanceof Iterator) {
            for (Iterator<?> i = (Iterator<?>) v; i.hasNext();) out.add(i.next());
        } else if (v != null && v.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(v); i++) out.add(Array.get(v, i));
        } else throw new IllegalArgumentException("a factory method returned " + v);
        return out;
    }

    // an element as arguments: Arguments' get(), an Object[] when the test takes more than one, else itself
    static Object[] arguments(Object e, int params) {
        if (e != null) {
            for (Class<?> k = e.getClass(); k != null; k = k.getSuperclass())
                for (Class<?> i : allInterfaces(k))
                    if (i.getName().equals(JP + "provider.Arguments")) {
                        try {
                            Method g = i.getMethod("get");
                            return (Object[]) g.invoke(e);
                        } catch (Exception x) {
                            throw new RuntimeException(x);
                        }
                    }
            // JUnit 5's MethodArgumentsProvider (the user guide's "Factory method arguments"): a one-dimensional
            // reference array is the arguments, whatever the parameters' count; a multidimensional array is one argument
            if (e instanceof Object[] && !e.getClass().getComponentType().isArray()) return (Object[]) e;
        }
        return new Object[] {e};
    }

    static List<Class<?>> allInterfaces(Class<?> k) {
        List<Class<?>> l = new ArrayList<>();
        for (Class<?> i : k.getInterfaces()) { l.add(i); l.addAll(allInterfaces(i)); }
        return l;
    }

    // a @CsvSource line: the delimiter, ' quotes, white space trimmed, an unquoted empty value null, '' the empty string
    static Object[] csv(String line, String d) {
        List<Object> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false, wasQuoted = false;
        for (int i = 0; i <= line.length(); i++) {
            if (i == line.length() || (!quoted && line.startsWith(d, i))) {
                String s = wasQuoted ? b.toString() : b.toString().trim();
                out.add(!wasQuoted && s.isEmpty() ? null : s);
                b.setLength(0); wasQuoted = false;
                if (i < line.length()) i += d.length() - 1;
                continue;
            }
            char ch = line.charAt(i);
            if (ch == '\'') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '\'') { b.append('\''); i++; continue; }
                if (!quoted) b.setLength(0);
                quoted = !quoted; wasQuoted = true;
                continue;
            }
            if (wasQuoted && !quoted) continue; // white space after a closing quote
            b.append(ch);
        }
        return out.toArray();
    }

    // JUnit's implicit conversion of a String argument to the parameter's type
    static Object convert(Object v, Class<?> t) {
        if (!(v instanceof String) || t == String.class || t == Object.class || t == CharSequence.class) return v;
        String s = (String) v;
        if (t == int.class || t == Integer.class) return Integer.valueOf(s);
        if (t == long.class || t == Long.class) return Long.valueOf(s);
        if (t == short.class || t == Short.class) return Short.valueOf(s);
        if (t == byte.class || t == Byte.class) return Byte.valueOf(s);
        if (t == double.class || t == Double.class) return Double.valueOf(s);
        if (t == float.class || t == Float.class) return Float.valueOf(s);
        if (t == boolean.class || t == Boolean.class) return Boolean.valueOf(s);
        if (t == char.class || t == Character.class) {
            if (s.length() != 1) throw new IllegalArgumentException("not a char: " + s);
            return Character.valueOf(s.charAt(0));
        }
        if (t.isEnum()) {
            for (Object k : t.getEnumConstants()) if (((Enum<?>) k).name().equals(s)) return k;
            throw new IllegalArgumentException("no constant " + s + " of " + t);
        }
        if (t == Class.class) {
            try { return Class.forName(s); } catch (ClassNotFoundException e) { throw new IllegalArgumentException(e); }
        }
        if (t == java.math.BigInteger.class) return new java.math.BigInteger(s);
        if (t == java.math.BigDecimal.class) return new java.math.BigDecimal(s);
        if (t == java.time.Duration.class) return java.time.Duration.parse(s);
        if (t == java.nio.file.Path.class) return java.nio.file.Path.of(s);
        if (t == java.io.File.class) return new java.io.File(s);
        if (t == java.net.URI.class) return java.net.URI.create(s);
        throw new IllegalArgumentException("no implicit conversion of a String to " + t.getName());
    }
}
