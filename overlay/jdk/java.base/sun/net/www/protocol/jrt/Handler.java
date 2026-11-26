/*
 * Arbace (overlay/jdk, java-tail2-1126): the jrt: URL's protocol handler. jdk26u's
 * sun.net.www.protocol.jrt.Handler opens a JavaRuntimeURLConnection over the run-time image (jdk.internal.jimage),
 * which a Go build has not (the jrt: file system's tests are n/a, test/java/divergences.edn's :jrt-filesystem): this
 * handler parses jrt: URLs as the JDK's (URLStreamHandler's syntax: URItoURLTest, t05019) and its connection fails on
 * connect with the JDK's message for a resource the image lacks. Copyright (c) the Arbace authors; Eclipse Public
 * License 1.0.
 */
package sun.net.www.protocol.jrt;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    public Handler() { }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new URLConnection(url) {
            @Override
            public void connect() throws IOException {
                throw new IOException(url + " not found");
            }
        };
    }
}
