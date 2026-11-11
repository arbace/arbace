/*
 * Arbace (overlay/jdk/javac/buildtools-world/generatenimbus, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS359
 * stages 2-3 (day)", JCS368 (b)): a tool-world variant for the JDK build's TOOL_GENERATENIMBUS
 * (make/jdk/src/classes/build/tools/generatenimbus) on javac's tool executable, which lacks these classes of StAX and Swing: only
 * what the generator calls, as the API documents it. Read with the generator by bin/javac-vendor gensrc and defined by
 * the interpreter beside it; never part of the platform or the program.
 * Written for the documented Java SE API (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package javax.xml.stream;

public class XMLStreamException extends Exception {
    private static final long serialVersionUID = 1L;

    public XMLStreamException() {
    }

    public XMLStreamException(String msg) {
        super(msg);
    }

    public XMLStreamException(Throwable th) {
        super(th);
    }

    public XMLStreamException(String msg, Throwable th) {
        super(msg, th);
    }
}
