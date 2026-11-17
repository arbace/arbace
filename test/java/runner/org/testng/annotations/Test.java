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
public @interface Test {
    boolean alwaysRun() default false;
    String dataProvider() default "";
    Class<?> dataProviderClass() default Object.class;
    String[] dependsOnGroups() default {};
    String[] dependsOnMethods() default {};
    String description() default "";
    boolean enabled() default true;
    Class[] expectedExceptions() default {};
    String expectedExceptionsMessageRegExp() default ".*";
    String[] groups() default {};
    int invocationCount() default 1;
    long invocationTimeOut() default 0;
    int priority() default 0;
    Class<? extends org.testng.IRetryAnalyzer> retryAnalyzer() default org.testng.IRetryAnalyzer.class;
    boolean singleThreaded() default false;
    int successPercentage() default 100;
    int threadPoolSize() default 0;
    long timeOut() default 0;
}
