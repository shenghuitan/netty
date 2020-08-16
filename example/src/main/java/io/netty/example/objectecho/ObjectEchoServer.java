/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.objectecho;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.example.echo.EchoServer;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modification of {@link EchoServer} which utilizes Java object serialization.
 *
 * 利用Java对象序列化，修改EchoServer。
 * 这里的对象开始Java对象，也是用Java自带的序列化。
 * 可以改写成其他序列化，比如JSON。
 *
 */
public final class ObjectEchoServer {

    private static Logger logger = LoggerFactory.getLogger(ObjectEchoServer.class);

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // 监听的对象资源初始化，也可以称为父线程池
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        // 线程执行的资源初始化，也可以称为子线程池
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        // 以下才是开始与网络相关的服务初始化
        try {
            ServerBootstrap b = new ServerBootstrap();  // 什么都不做
             // 设置acceptor和client处理线程池
            b.group(bossGroup, workerGroup)
             // 初始化ChannelFactory，暂存NioServerSocketChannel的构造器
             .channel(NioServerSocketChannel.class)
             // 父处理器初始化（bossGroup）：日志处理器，这个处理器能做什么呢？
             .handler(new LoggingHandler(LogLevel.INFO))
             // 子处理器初始化（workerGroup）：初始化SocketChannel
             .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    p.addLast(
                            new ObjectEncoder(),
                            new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                            new ObjectEchoServerHandler());
                }
             });

            // Bind and start to accept incoming connections.
            // 绑定服务端口
            ChannelFuture bindFuture = b.bind(PORT).sync();
            logger.info("bindFuture:{}", bindFuture);

            // 获取到绑定端口，也就是acceptor的Channel
            Channel channel = bindFuture.channel();
            logger.info("channel:{}", channel);

            // 当acceptor的Channel已经被通知关闭的时候，触发closeFuture()
            // 同步等待acceptor的关闭操作。
            ChannelFuture closeFuture = channel.closeFuture().sync();
            logger.info("closeFuture:{}", closeFuture);
        } finally {
            // 接收客户请求的Channel已关闭，释放资源
            bossGroup.shutdownGracefully();
            logger.info("bossGroup.shutdownGracefully end.");

            // 释放工作线程资源
            workerGroup.shutdownGracefully();
            logger.info("workerGroup.shutdownGracefully end.");
        }
    }
}
