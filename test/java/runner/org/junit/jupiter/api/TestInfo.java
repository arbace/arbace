/*
 * Arbace's own source-compatible subset of JUnit 5's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from the JUnit 5 user guide and API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.jupiter.api;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

/** The current test's names, a parameter of a test or a lifecycle method. Impl is the check's runner's. */
public interface TestInfo {
    String getDisplayName();
    Set<String> getTags();
    Optional<Class<?>> getTestClass();
    Optional<Method> getTestMethod();

    final class Impl implements TestInfo {
        private final String displayName;
        private final Class<?> testClass;
        private final Method testMethod;
        Impl(String displayName, Class<?> testClass, Method testMethod) {
            this.displayName = displayName; this.testClass = testClass; this.testMethod = testMethod;
        }
        public String getDisplayName() { return displayName; }
        public Set<String> getTags() { return Set.of(); }
        public Optional<Class<?>> getTestClass() { return Optional.ofNullable(testClass); }
        public Optional<Method> getTestMethod() { return Optional.ofNullable(testMethod); }
    }
}
