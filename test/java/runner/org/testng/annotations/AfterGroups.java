/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
public @interface AfterGroups {
    String[] value() default {};
    boolean alwaysRun() default false;
    String[] dependsOnGroups() default {};
    String[] dependsOnMethods() default {};
    String description() default "";
    boolean enabled() default true;
    String[] groups() default {};
    boolean inheritGroups() default true;
    long timeOut() default 0;
}
