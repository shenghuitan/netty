/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.netty.example.http2.helloworld.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.http2.Http2Consts;
import io.netty.example.util.Counter;
import io.netty.example.util.Timer;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * An HTTP2 client that allows you to send HTTP2 frames to a server using HTTP1-style approaches
 * (via {@link io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter}). Inbound and outbound
 * frames are logged.
 * When run from the command-line, sends a single HEADERS frame to the server and gets back
 * a "Hello World" response.
 * See the ./http2/helloworld/frame/client/ example for a HTTP2 client example which does not use
 * HTTP1-style objects and patterns.
 */
public final class Http2Client {

    private static Logger logger = LoggerFactory.getLogger(Http2Client.class);

    static {
        Http2Consts.setSystemProperties();
    }

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));
    static final String URL = System.getProperty("url", "/whatever");
    static final String URL2 = System.getProperty("url2");
    static final String URL2DATA = System.getProperty("url2data", "test data!");

    public static void main(String[] args) throws Exception {
        new Http2Client().main0();
    }

    private Counter counter = new Counter();

    final int limit = 100_000;
    final int streams = 128 * 2;
    int streamId = 3;

    LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();
    AtomicLong totalCost = new AtomicLong(0L);
    Timer successTimer = Timer.init();

    public void main0() throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
            sslCtx = SslContextBuilder.forClient()
                .sslProvider(provider)
                /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                 * Please refer to the HTTP/2 specification for cipher requirements. */
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                    Protocol.ALPN,
                    // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                    SelectorFailureBehavior.NO_ADVERTISE,
                    // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                    SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1))
                .build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Http2ClientInitializer initializer = new Http2ClientInitializer(sslCtx, Integer.MAX_VALUE);

        try {
            // Configure the client.
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.remoteAddress(HOST, PORT);
            b.handler(initializer);

            // Start the client.
            final Channel channel = b.connect().syncUninterruptibly().channel();
            System.out.println("Connected to [" + HOST + ':' + PORT + ']');

            // Wait for the HTTP/2 upgrade to occur.
            Http2SettingsHandler http2SettingsHandler = initializer.settingsHandler();
            http2SettingsHandler.awaitSettings(5, TimeUnit.SECONDS);

            final HttpResponseHandler responseHandler = initializer.responseHandler();

            final HttpScheme scheme = SSL ? HttpScheme.HTTPS : HttpScheme.HTTP;
            final AsciiString hostName = new AsciiString(HOST + ':' + PORT);
            System.err.println("Sending request(s)...");

            for (int i = 0; i < streams; i++) {
                send(scheme, hostName, responseHandler, channel, new PromiseListener());
            }

            channel.flush();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (counter.sent.get() < limit) {
                        try {
                            queue.take();
                            send(scheme, hostName, responseHandler, channel, new PromiseListener());
                        } catch (InterruptedException e) {
                            logger.error("", e);
                        }
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        responseHandler.awaitResponses(5, TimeUnit.SECONDS);
                        logger.info("Finished HTTP/2 request(s), streams:{}, totalCost:{}, averageCost:{}, counter:{}, successTimer:{}",
                                streams, totalCost, totalCost.get() / counter.succeed.get(), counter, successTimer);

                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            logger.error("", e);
                            return;
                        }
                    }
                }
            }).start();

            // Wait until the connection is closed.
            channel.closeFuture().syncUninterruptibly();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public class FutureListener implements GenericFutureListener<Future<? super Void>> {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
            counter.sent.getAndIncrement();
        }
    }

    public class PromiseListener implements GenericFutureListener<Future<? super Void>> {
        Timer t0 = Timer.init();
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
            t0.mark();
            successTimer.mark();
            counter.succeed.getAndIncrement();
            totalCost.getAndAdd(t0.cost());
            queue.offer(1L);
        }
    }

    public void send(HttpScheme scheme, AsciiString hostName, HttpResponseHandler responseHandler,
                     Channel channel, GenericFutureListener listener) {
        counter.received.getAndIncrement();

        if (counter.received.get() > limit) {
            logger.info("send end...");
            return;
        }

        counter.read.getAndIncrement();

        if (URL != null) {
            // Create a simple GET request.
            FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, getUrl(), Unpooled.EMPTY_BUFFER);
            request.headers().add(HttpHeaderNames.HOST, hostName);
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), scheme.name());
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);

            ChannelPromise promise = channel.newPromise();
            promise.addListener(new PromiseListener());

            ChannelFuture future = channel.write(request);
            future.addListener(new FutureListener());

            responseHandler.put(streamId, future, promise);
            streamId = (streamId + 2);
        }
        if (URL2 != null) {
            // Create a simple POST request with a body.
            FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, POST, URL2,
                    wrappedBuffer(URL2DATA.getBytes(CharsetUtil.UTF_8)));
            request.headers().add(HttpHeaderNames.HOST, hostName);
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), scheme.name());
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);

            responseHandler.put(streamId, channel.write(request), channel.newPromise());
        }
        channel.flush();
    }

    private String getUrl() {
        return URL + "?=" + System.currentTimeMillis();
    }

}
