/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS254 (b)"): jdk26u's
 * jdk.internal.net.http.ExchangeImpl (src/java.net.http/share/classes/jdk/internal/net/http/ExchangeImpl.java
 * at https://github.com/openjdk/jdk26u baf63fb), derived by test/jrt/derive.ae (bin/jrt-convert derive): the HTTP/3 exchange's methods cut (attemptHttp3Exchange to the HTTP/3 createExchangeImpl), get's HTTP/3 branch failing as the JDK does for a client without HTTP/3, no HttpQuicConnection;
 * nothing else changed. jdk26u's text Copyright (c) Oracle and/or its affiliates, under the GNU General Public
 * License version 2 with the Classpath Exception (LICENSE.md); the derivation Copyright (c) the Arbace authors;
 * Eclipse Public License 1.0.
 */
/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.net.http;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.ResponseInfo;
import java.net.http.UnsupportedProtocolVersionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jdk.internal.net.http.Http2Connection.ALPNException;
import jdk.internal.net.http.common.HttpBodySubscriberWrapper;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;

import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.net.http.HttpClient.Version.HTTP_3;

/**
 * Splits request so that headers and body can be sent separately with optional
 * (multiple) responses in between (e.g. 100 Continue). Also request and
 * response always sent/received in different calls.
 *
 * Synchronous and asynchronous versions of each method are provided.
 *
 * Separate implementations of this class exist for HTTP/1.1 and HTTP/2
 *      Http1Exchange   (HTTP/1.1)
 *      Stream          (HTTP/2)
 *
 * These implementation classes are where work is allocated to threads.
 */
abstract class ExchangeImpl<T> {

    private static final Logger debug =
            Utils.getDebugLogger("ExchangeImpl"::toString, Utils.DEBUG);

    final Exchange<T> exchange;

    private volatile boolean expectTimeoutRaised;

    ExchangeImpl(Exchange<T> e) {
        // e == null means a http/2 pushed stream
        this.exchange = e;
    }

    final Exchange<T> getExchange() {
        return exchange;
    }

    final void setExpectTimeoutRaised() {
        expectTimeoutRaised = true;
    }

    final boolean expectTimeoutRaised() {
        return expectTimeoutRaised;
    }

    HttpClientImpl client() {
        return exchange.client();
    }

    /**
     * Returns the {@link HttpConnection} instance to which this exchange is
     * assigned.
     */
    abstract HttpConnection connection();

    /**
     * Initiates a new exchange and assigns it to a connection if one exists
     * already. connection usually null.
     */
    static <U> CompletableFuture<? extends ExchangeImpl<U>>
    get(Exchange<U> exchange, HttpConnection connection)
    {
        HttpRequestImpl request = exchange.request();
        var version = exchange.version();
        if (version == HTTP_1_1 || request.isWebSocket()) {
            if (debug.on())
                debug.log("get: HTTP/1.1: new Http1Exchange");
            return createHttp1Exchange(exchange, connection);
        } else if (!request.secure() && request.isHttp3Only(version)) {
            assert version == HTTP_3;
            assert !request.isWebSocket();
            if (debug.on())
                debug.log("get: HTTP/3: HTTP/3 is not supported on plain connections");
            return MinimalFuture.failedFuture(
                    new UnsupportedProtocolVersionException(
                            "HTTP/3 is not supported on plain connections"));
        } else if (version == HTTP_2 || isTCP(connection) || !request.secure()) {
            assert !request.isWebSocket();
            return attemptHttp2Exchange(exchange, connection);
        } else {
            assert request.secure();
            assert version == HTTP_3;
            assert !request.isWebSocket();
            // no HTTP/3 in the Go build (JCS254 (b)): MultiExchange downgrades a secure HTTP/3 request first
            return MinimalFuture.failedFuture(
                    new UnsupportedProtocolVersionException("HTTP3 is not supported"));
        }
    }

    private static boolean isTCP(HttpConnection connection) {
        if (connection == null) return false;
        // if it's not an HttpQuicConnection and it's not null it's
        // a TCP connection
        return true;
    }

    private static <U> CompletableFuture<? extends ExchangeImpl<U>>
    attemptHttp2Exchange(Exchange<U> exchange, HttpConnection connection) {
        HttpRequestImpl request = exchange.request();
        Http2ClientImpl c2 = exchange.client().client2(); // #### improve
        CompletableFuture<Http2Connection> c2f = c2.getConnectionFor(request, exchange);
        if (debug.on())
            debug.log("get: Trying to get HTTP/2 connection");
        // local variable required here; see JDK-8223553
        CompletableFuture<CompletableFuture<? extends ExchangeImpl<U>>> fxi =
                c2f.handle((h2c, t) -> createExchangeImpl(h2c, t, exchange, connection));
        return fxi.thenCompose(x -> x);
    }

    private static <U, T extends ExchangeImpl<U>> T checkCancelled(T exchangeImpl) {
        Exchange<?> e = exchangeImpl.getExchange();
        if (debug.on()) {
            debug.log("checking cancellation for: " + exchangeImpl);
        }
        if (e.multi.requestCancelled()) {
            if (debug.on()) {
                debug.log("request was cancelled");
            }
            if (!exchangeImpl.isCanceled()) {
                if (debug.on()) {
                    debug.log("cancelling exchange: " + exchangeImpl);
                }
                var cause = e.getCancelCause();
                if (cause == null) cause = new IOException("Request cancelled");
                exchangeImpl.cancel(cause);
            }
        }
        return exchangeImpl;
    }


    // Creates an HTTP/2 exchange, possibly downgrading to HTTP/1
    private static <U> CompletableFuture<? extends ExchangeImpl<U>>
    createExchangeImpl(Http2Connection c,
                       Throwable t,
                       Exchange<U> exchange,
                       HttpConnection connection)
    {
        if (debug.on())
            debug.log("handling HTTP/2 connection creation result");
        boolean secure = exchange.request().secure();
        if (t != null) {
            if (debug.on())
                debug.log("handling HTTP/2 connection creation failed: %s",
                                 (Object)t);
            t = Utils.getCompletionCause(t);
            if (t instanceof Http2Connection.ALPNException) {
                Http2Connection.ALPNException ee = (Http2Connection.ALPNException)t;
                AbstractAsyncSSLConnection as = ee.getConnection();
                if (debug.on())
                    debug.log("downgrading to HTTP/1.1 with: %s", as);
                CompletableFuture<? extends ExchangeImpl<U>> ex =
                        createHttp1Exchange(exchange, as);
                return ex;
            } else {
                if (debug.on())
                    debug.log("HTTP/2 connection creation failed "
                                     + "with unexpected exception: %s", (Object)t);
                return MinimalFuture.failedFuture(t);
            }
        }
        if (secure && c== null) {
            if (debug.on())
                debug.log("downgrading to HTTP/1.1 ");
            CompletableFuture<? extends ExchangeImpl<U>> ex =
                    createHttp1Exchange(exchange, null);
            return ex;
        }
        if (c == null) {
            // no existing connection. Send request with HTTP 1 and then
            // upgrade if successful
            if (debug.on())
                debug.log("new Http1Exchange, try to upgrade");
            return createHttp1Exchange(exchange, connection)
                    .thenApply((e) -> {
                        exchange.h2Upgrade();
                        return e;
                    });
        } else {
            if (debug.on()) debug.log("creating HTTP/2 streams");
            Stream<U> s = c.createStream(exchange);
            CompletableFuture<? extends ExchangeImpl<U>> ex = MinimalFuture.completedFuture(s);
            return ex;
        }
    }

    private static <T> CompletableFuture<Http1Exchange<T>>
    createHttp1Exchange(Exchange<T> ex, HttpConnection as)
    {
        try {
            return MinimalFuture.completedFuture(new Http1Exchange<>(ex, as));
        } catch (Throwable e) {
            return MinimalFuture.failedFuture(e);
        }
    }

    // Called for 204 response - when no body is permitted
    void nullBody(HttpResponse<T> resp, Throwable t) {
        // Needed for HTTP/1.1 to close the connection or return it to the pool
        // Needed for HTTP/2 to subscribe a dummy subscriber and close the stream
    }

    /**
     * {@return {@code true}, if it is allowed to cancel the request timer on
     * response body subscriber termination; {@code false}, otherwise}
     *
     * @param webSocket indicates if the associated request is a WebSocket handshake
     * @param statusCode the status code of the associated response
     */
    static boolean cancelTimerOnResponseBodySubscriberTermination(
            boolean webSocket, int statusCode) {
        return webSocket || statusCode < 100 || statusCode >= 200;
    }

    /* The following methods have separate HTTP/1.1 and HTTP/2 implementations */

    abstract CompletableFuture<ExchangeImpl<T>> sendHeadersAsync();

    /** Sends a request body, after request headers have been sent. */
    abstract CompletableFuture<ExchangeImpl<T>> sendBodyAsync();

    abstract CompletableFuture<T> readBodyAsync(HttpResponse.BodyHandler<T> handler,
                                                boolean returnConnectionToPool,
                                                Executor executor);

    /**
     * Creates and wraps an {@link HttpResponse.BodySubscriber} from a {@link
     * HttpResponse.BodyHandler} for the given {@link ResponseInfo}.
     * An {@code HttpBodySubscriberWrapper} wraps a response body subscriber and makes
     * sure its completed/onError methods are called only once, and that its onSusbscribe
     * is called before onError. This is useful when errors occur asynchronously, and
     * most typically when the error occurs before the {@code BodySubscriber} has
     * subscribed.
     * @param handler  a body handler
     * @param response a response info
     * @return a new {@code HttpBodySubscriberWrapper} to handle the response
     */
    HttpBodySubscriberWrapper<T> createResponseSubscriber(
            HttpResponse.BodyHandler<T> handler, ResponseInfo response) {
        return new HttpBodySubscriberWrapper<>(handler.apply(response));
    }

    /**
     * Ignore/consume the body.
     */
    abstract CompletableFuture<Void> ignoreBody();


    /** Gets the response headers. Completes before body is read. */
    abstract CompletableFuture<Response> getResponseAsync(Executor executor);


    /** Cancels a request.  Not currently exposed through API. */
    abstract void cancel();

    /**
     * Cancels a request with a cause.  Not currently exposed through API.
     */
    abstract void cancel(IOException cause);

    /**
     * Invoked whenever there is a (HTTP) protocol error when dealing with the response
     * from the server. The implementations of {@code ExchangeImpl} are then expected to
     * take necessary action that is expected by the corresponding specifications whenever
     * a protocol error happens. For example, in HTTP/1.1, such protocol error would result
     * in the connection being closed.
     * @param cause The cause of the protocol violation
     */
    abstract void onProtocolError(IOException cause);

    /**
     * Called when the exchange is released, so that cleanup actions may be
     * performed - such as deregistering callbacks.
     * Typically released is called during upgrade, when an HTTP/2 stream
     * takes over from an Http1Exchange, or when a new exchange is created
     * during a multi exchange before the final response body was received.
     */
    abstract void released();

    /**
     * Called when the exchange is completed, so that cleanup actions may be
     * performed - such as deregistering callbacks.
     * Typically, completed is called at the end of the exchange, when the
     * final response body has been received (or an error has caused the
     * completion of the exchange).
     */
    abstract void completed();

    /**
     * Returns true if this exchange was canceled.
     * @return true if this exchange was canceled.
     */
    abstract boolean isCanceled();

    /**
     * Returns the cause for which this exchange was canceled, if available.
     * @return the cause for which this exchange was canceled, if available.
     */
    abstract Throwable getCancelCause();

    // Mark the exchange as upgraded
    // Needed to handle cancellation during the upgrade from
    // Http1Exchange to Stream
    void upgraded() { }

    // Called when server returns non 100 response to
    // an Expect-Continue
    void expectContinueFailed(int rcode) { }

}
