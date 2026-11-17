/*
 * Arbace's own source-compatible subset of JUnit 5's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from the JUnit 5 user guide and API documentation for the check's framework runner
 * (org.arbace.jtreg.Runner); not JUnit's code.
 */
package org.junit.jupiter.params.provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The arguments of factory methods (the test's own name when none): a Stream, an Iterable, an Iterator or an array of Arguments, Object[] or single values. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface MethodSource {
    String[] value() default "";
}
