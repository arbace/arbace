package demo.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A15's fixture (the Ghidra demo's G2, GH13 (a)): java.xml as the program's world extension
 * (program.edn's :jdk-extra), JAXP's factories by default: a DOM parse and a SAX parse of the
 * same document, as log4j-core's XmlConfiguration and Ghidra's SAX pull parsers use them.
 */
public class Main {
    static final String DOC =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<Configuration status=\"warn\" packages=\"log\">\n"
        + "  <Appenders><Console name=\"console\" target=\"SYSTEM_OUT\"/></Appenders>\n"
        + "  <Loggers><Root level=\"info\"><AppenderRef ref=\"console\"/></Root></Loggers>\n"
        + "</Configuration>\n";

    static void dom(Node n, String indent, StringBuilder out) {
        if (n.getNodeType() != Node.ELEMENT_NODE) return;
        Element e = (Element) n;
        out.append(indent).append(e.getTagName());
        for (int i = 0; i < e.getAttributes().getLength(); i++) {
            Node a = e.getAttributes().item(i);
            out.append(' ').append(a.getNodeName()).append('=').append(a.getNodeValue());
        }
        out.append('\n');
        NodeList cs = e.getChildNodes();
        for (int i = 0; i < cs.getLength(); i++) dom(cs.item(i), indent + "  ", out);
    }

    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(new ByteArrayInputStream(DOC.getBytes(StandardCharsets.UTF_8)));
        StringBuilder out = new StringBuilder();
        dom(d.getDocumentElement(), "", out);
        System.out.print("dom:\n" + out);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        final int[] count = {0};
        final StringBuilder names = new StringBuilder();
        sp.parse(new ByteArrayInputStream(DOC.getBytes(StandardCharsets.UTF_8)), new DefaultHandler() {
            @Override
            public void startElement(String uri, String local, String qName, Attributes atts) {
                count[0]++;
                names.append(names.length() == 0 ? "" : " ").append(qName);
            }
        });
        System.out.println("sax: " + count[0] + " elements: " + names);
    }
}
