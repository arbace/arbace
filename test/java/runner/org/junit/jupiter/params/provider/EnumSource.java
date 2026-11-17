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

/** One argument each of an enum's constants (the parameter's type when value() is none), those of names() (INCLUDE) or the others (EXCLUDE). */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface EnumSource {
    enum NullEnum {}
    enum Mode { INCLUDE, EXCLUDE, MATCH_ALL, MATCH_ANY }
    Class<? extends Enum<?>> value() default NullEnum.class;
    String[] names() default {};
    Mode mode() default Mode.INCLUDE;
}
