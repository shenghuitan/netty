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
package io.netty.example.discard;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles a server-side channel.
 *
 * 收到客户端的循环发送包，解析并扔掉，不需要返回任何内容。
 * 测试连续的大量的包接收，并观察无分隔符等协议下的粘包现象。
 *
 */
public class DiscardServerHandler extends SimpleChannelInboundHandler<Object> {

    Logger logger = LoggerFactory.getLogger(DiscardServerHandler.class);

    AtomicInteger i = new AtomicInteger(0);

    /**
     * 没有定义解析的协议，这里存在粘包的现象。
     * 这不是测试的重点就是了。
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     * @throws Exception
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // discard
        int size = -1;
        int size0 = -1;
        if (msg instanceof ByteBuf) {
            ByteBuf msg0 = (ByteBuf) msg;
            size = msg0.capacity();
            size0 = msg0.slice().capacity();
        }
        logger.info("channelRead0 i:{}, size:{}, size0:{}, msg:{}", i.getAndIncrement(), size, size0, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
