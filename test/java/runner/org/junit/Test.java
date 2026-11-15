/*
 * Arbace's own source-compatible subset of JUnit 4's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), which jtreg's
 * @run junit runs too, written clean-room from JUnit 4's API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A JUnit 4 test method (public void, no parameters): it passes when it returns, or throws expected(). */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Test {
    /** No exception expected. */
    class None extends Throwable {
        private static final long serialVersionUID = 1L;
        private None() {}
    }
    Class<? extends Throwable> expected() default None.class;
    long timeout() default 0L;
}
