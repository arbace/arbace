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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.XMLReader;

/**
 * newInstance and createXMLStreamReader(InputStream): a reader over the document's events as java.xml's SAX parser (in
 * javac's tool world) reports them to a ContentHandler (an interface: the interpreter's classes implement the Go build's
 * interfaces but do not extend its classes, such as DefaultHandler), namespaces off (the local name is the qualified name, as for a document without
 * namespaces), the events taken first and then walked as StAX's cursor: START_DOCUMENT, START_ELEMENT and END_ELEMENT,
 * CHARACTERS (adjacent character data joined, ignorable white space included), END_DOCUMENT. Comments and processing
 * instructions are not reported (SAX's ContentHandler does not see comments; the generator skips both). The
 * reader's other methods, which the generator does not call, throw UnsupportedOperationException.
 */
public class XMLInputFactory {
    public static XMLInputFactory newInstance() {
        return new XMLInputFactory();
    }

    public XMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException {
        final List<Object[]> events = new ArrayList<>();
        final StringBuilder text = new StringBuilder();
        try {
            SAXParserFactory f = SAXParserFactory.newInstance();
            f.setNamespaceAware(false);
            XMLReader xr = f.newSAXParser().getXMLReader();
            xr.setContentHandler(new ContentHandler() {
                private void flush() {
                    if (text.length() > 0) {
                        events.add(new Object[] {XMLStreamConstants.CHARACTERS, null, null, text.toString()});
                        text.setLength(0);
                    }
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes atts) {
                    flush();
                    Map<String, String> m = new HashMap<>();
                    for (int i = 0; i < atts.getLength(); i++) {
                        m.put(atts.getQName(i), atts.getValue(i));
                    }
                    events.add(new Object[] {XMLStreamConstants.START_ELEMENT, qName, m, null});
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    flush();
                    events.add(new Object[] {XMLStreamConstants.END_ELEMENT, qName, null, null});
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    text.append(ch, start, length);
                }

                @Override
                public void ignorableWhitespace(char[] ch, int start, int length) {
                    text.append(ch, start, length);
                }

                @Override public void setDocumentLocator(Locator locator) { }
                @Override public void startDocument() { }
                @Override public void endDocument() { flush(); }
                @Override public void startPrefixMapping(String prefix, String uri) { }
                @Override public void endPrefixMapping(String prefix) { }
                @Override public void processingInstruction(String target, String data) { flush(); }
                @Override public void skippedEntity(String name) { }
            });
            xr.parse(new InputSource(stream));
        } catch (Exception e) {
            throw new XMLStreamException(e.getMessage(), e);
        }
        events.add(new Object[] {XMLStreamConstants.END_DOCUMENT, null, null, null});
        return new Cursor(events);
    }

    private static final class Cursor implements XMLStreamReader {
        private final List<Object[]> events;
        private int at = -1;

        Cursor(List<Object[]> events) {
            this.events = events;
        }

        private Object[] cur() {
            return events.get(at);
        }

        @Override
        public boolean hasNext() {
            return at < 0 || (Integer) cur()[0] != END_DOCUMENT;
        }

        @Override
        public int next() throws XMLStreamException {
            if (!hasNext()) {
                throw new XMLStreamException("no more events");
            }
            at++;
            return getEventType();
        }

        @Override
        public int getEventType() {
            return at < 0 ? START_DOCUMENT : (Integer) cur()[0];
        }

        @Override
        public String getLocalName() {
            int t = getEventType();
            if (t != START_ELEMENT && t != END_ELEMENT) {
                throw new IllegalStateException("not at an element");
            }
            return (String) cur()[1];
        }

        @Override
        @SuppressWarnings("unchecked")
        public String getAttributeValue(String namespaceURI, String localName) {
            if (getEventType() != START_ELEMENT) {
                throw new IllegalStateException("not at a start element");
            }
            return ((Map<String, String>) cur()[2]).get(localName);
        }

        @Override
        public String getElementText() throws XMLStreamException {
            if (getEventType() != START_ELEMENT) {
                throw new XMLStreamException("not at a start element");
            }
            StringBuilder sb = new StringBuilder();
            int t = next();
            while (t != END_ELEMENT) {
                if (t == CHARACTERS) {
                    sb.append((String) cur()[3]);
                } else {
                    throw new XMLStreamException("element text holds an element");
                }
                t = next();
            }
            return sb.toString();
        }

        @Override
        public String getText() {
            if (getEventType() != CHARACTERS) {
                throw new IllegalStateException("not at characters");
            }
            return (String) cur()[3];
        }

        @Override
        public void close() {
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("not in the generator's StAX (bin/javac-vendor gensrc)");
        }

        @Override public Object getProperty(String name) { throw unsupported(); }
        @Override public void require(int type, String namespaceURI, String localName) { throw unsupported(); }
        @Override public int nextTag() { throw unsupported(); }
        @Override public String getNamespaceURI(String prefix) { throw unsupported(); }
        @Override public boolean isStartElement() { return getEventType() == START_ELEMENT; }
        @Override public boolean isEndElement() { return getEventType() == END_ELEMENT; }
        @Override public boolean isCharacters() { return getEventType() == CHARACTERS; }
        @Override public boolean isWhiteSpace() { throw unsupported(); }
        @Override public String getAttributeValue(int index) { throw unsupported(); }
        @Override public int getAttributeCount() { throw unsupported(); }
        @Override public QName getAttributeName(int index) { throw unsupported(); }
        @Override public String getAttributeNamespace(int index) { throw unsupported(); }
        @Override public String getAttributeLocalName(int index) { throw unsupported(); }
        @Override public String getAttributePrefix(int index) { throw unsupported(); }
        @Override public String getAttributeType(int index) { throw unsupported(); }
        @Override public boolean isAttributeSpecified(int index) { throw unsupported(); }
        @Override public int getNamespaceCount() { throw unsupported(); }
        @Override public String getNamespacePrefix(int index) { throw unsupported(); }
        @Override public String getNamespaceURI(int index) { throw unsupported(); }
        @Override public NamespaceContext getNamespaceContext() { throw unsupported(); }
        @Override public char[] getTextCharacters() { throw unsupported(); }
        @Override public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) { throw unsupported(); }
        @Override public int getTextStart() { throw unsupported(); }
        @Override public int getTextLength() { throw unsupported(); }
        @Override public String getEncoding() { throw unsupported(); }
        @Override public boolean hasText() { throw unsupported(); }
        @Override public Location getLocation() { throw unsupported(); }
        @Override public QName getName() { throw unsupported(); }
        @Override public boolean hasName() { throw unsupported(); }
        @Override public String getNamespaceURI() { throw unsupported(); }
        @Override public String getPrefix() { throw unsupported(); }
        @Override public String getVersion() { throw unsupported(); }
        @Override public boolean isStandalone() { throw unsupported(); }
        @Override public boolean standaloneSet() { throw unsupported(); }
        @Override public String getCharacterEncodingScheme() { throw unsupported(); }
        @Override public String getPITarget() { throw unsupported(); }
        @Override public String getPIData() { throw unsupported(); }
    }
}
