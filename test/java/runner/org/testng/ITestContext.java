/*
 * Arbace's own source-compatible subset of TestNG's API (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a)), written
 * clean-room from TestNG's documentation for the check's framework runner (org.arbace.jtreg.Runner); not TestNG's code.
 */
package org.testng;

import java.util.Map;
import java.util.Set;

/** The context of a test run (TestNG's): its name and attributes. Impl is the check's runner's. */
public interface ITestContext {
    String getName();
    Object getAttribute(String name);
    void setAttribute(String name, Object value);
    Set<String> getAttributeNames();
    Object removeAttribute(String name);

    final class Impl implements ITestContext {
        private final Map<String, Object> attributes;
        Impl(Map<String, Object> attributes) { this.attributes = attributes; }
        public String getName() { return "Default test"; }
        public Object getAttribute(String name) { return attributes.get(name); }
        public void setAttribute(String name, Object value) { attributes.put(name, value); }
        public Set<String> getAttributeNames() { return attributes.keySet(); }
        public Object removeAttribute(String name) { return attributes.remove(name); }
    }
}
