/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng;

/** A test method's invocation and its result (TestNG's): status, parameters, throwable. Impl is the check's runner's. */
public interface ITestResult {
    int CREATED = -1;
    int SUCCESS = 1;
    int FAILURE = 2;
    int SKIP = 3;
    int SUCCESS_PERCENTAGE_FAILURE = 4;
    int STARTED = 16;

    String getName();
    int getStatus();
    void setStatus(int status);
    boolean isSuccess();
    Throwable getThrowable();
    void setThrowable(Throwable throwable);
    Object[] getParameters();
    Object getInstance();

    final class Impl implements ITestResult {
        private final String name;
        private final Object[] parameters;
        private final Object instance;
        private int status = STARTED;
        private Throwable throwable;
        Impl(String name, Object[] parameters, Object instance) { this.name = name; this.parameters = parameters; this.instance = instance; }
        public String getName() { return name; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        void setStatus(int status, Throwable throwable) { this.status = status; this.throwable = throwable; }
        public boolean isSuccess() { return status == SUCCESS; }
        public Throwable getThrowable() { return throwable; }
        public void setThrowable(Throwable throwable) { this.throwable = throwable; }
        public Object[] getParameters() { return parameters; }
        public Object getInstance() { return instance; }
    }
}
