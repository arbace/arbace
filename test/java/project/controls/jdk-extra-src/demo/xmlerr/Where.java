package demo.xmlerr;

import org.xml.sax.SAXParseException;

/** bin/java-project test's :jdk-extra case: a class naming java.xml's SAXParseException (A15). */
public class Where {
    public static String where(SAXParseException e) {
        return e.getSystemId() + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ": " + e.getMessage();
    }

    public static void main(String[] args) {
        System.out.println(where(new SAXParseException("an undeclared entity", "-//demo//x", "demo.xml", 3, 7)));
    }
}
