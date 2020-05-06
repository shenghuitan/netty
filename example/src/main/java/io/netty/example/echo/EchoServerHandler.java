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
package io.netty.example.echo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    Logger logger = LoggerFactory.getLogger(EchoServerHandler.class);

    AtomicInteger i = new AtomicInteger(0);
    int total = 0;
    long time = System.currentTimeMillis();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.debug("channelRead i:{}", i.getAndIncrement());
        long cost = System.currentTimeMillis() - time;
        if (cost > 1000L) {
            logger.info("channelRead cost:{}ms, tps:{}", cost, (i.get() - total) / cost * 1000);
            time = System.currentTimeMillis();
            total = i.get();
        }
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        logger.debug("channelReadComplete i:{}", i.getAndIncrement());
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
