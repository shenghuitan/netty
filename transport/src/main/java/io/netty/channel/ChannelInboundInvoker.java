/*
 * Copyright 2016 The Netty Project
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
package io.netty.channel;

/**
 * Channel.ChannelPipeline.ChannelInboundHandlers.next()对应的方法的触发器。
 */
public interface ChannelInboundInvoker {

    /**
     * A {@link Channel} was registered to its {@link EventLoop}.
     *
     * 注册Channel到它的EventLoop。
     * 称从上到下，或从下到上为一个事件循环。
     *
     * This will result in having the  {@link ChannelInboundHandler#channelRegistered(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     * 这会使得当前的Channel的，包含在ChannelPipeline的下一个ChannelInboundHandler的
     * ChannelInboundHandler#channelRegistered(ChannelHandlerContext)方法被调用
     */
    ChannelInboundInvoker fireChannelRegistered();

    /**
     * A {@link Channel} was unregistered from its {@link EventLoop}.
     *
     * 注销Channel，从它的EventLoop。
     *
     * This will result in having the  {@link ChannelInboundHandler#channelUnregistered(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     * 这会使得当前Channel的ChannelPipeline的下一个ChannelInboundHandler的
     * ChannelInboundHandler#channelUnregistered(ChannelHandlerContext)方法被调用。
     */
    ChannelInboundInvoker fireChannelUnregistered();

    /**
     * A {@link Channel} is active now, which means it is connected.
     *
     * Channel正在激活，这意味着它被连接。
     *
     * This will result in having the  {@link ChannelInboundHandler#channelActive(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     * 这会导致Channel的ChannelPipeline的下一个ChannelInboundHandler的
     * ChannelInboundHandler#channelActive(ChannelHandlerContext)方法被调用。
     */
    ChannelInboundInvoker fireChannelActive();

    /**
     * A {@link Channel} is inactive now, which means it is closed.
     *
     * Channel现在被失效，这表示它被关闭。
     *
     * This will result in having the  {@link ChannelInboundHandler#channelInactive(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     * 方法Channel -> ChannelPipeline -> next.ChannelInboundHandler -> channelInactive被调用。
     *
     */
    ChannelInboundInvoker fireChannelInactive();

    /**
     * A {@link Channel} received an {@link Throwable} in one of its inbound operations.
     *
     * Channel收到一个异常，在它的其中一个inbound操作中。
     *
     * This will result in having the  {@link ChannelInboundHandler#exceptionCaught(ChannelHandlerContext, Throwable)}
     * method  called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     * Channel -> ChannelPipeline -> next.ChannelInboundHandler -> ChannelInboundHandler 方法被调用。
     */
    ChannelInboundInvoker fireExceptionCaught(Throwable cause);

    /**
     * A {@link Channel} received an user defined event.
     *
     * Channel收到一个用户定义事件。
     *
     * This will result in having the  {@link ChannelInboundHandler#userEventTriggered(ChannelHandlerContext, Object)}
     * method  called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     * Channel -> ChannelPipeline -> next.ChannelInboundHandler -> userEventTriggered 方法被调用。
     */
    ChannelInboundInvoker fireUserEventTriggered(Object event);

    /**
     * A {@link Channel} received a message.
     *
     * Channel收到一个消息。
     *
     * This will result in having the {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object)}
     * method  called of the next {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     *
     * Channel.ChannelPipeline.ChannelInboundHandlers.nextChannelInboundHandler.channelRead 方法被调用。
     */
    ChannelInboundInvoker fireChannelRead(Object msg);

    /**
     * Triggers an {@link ChannelInboundHandler#channelReadComplete(ChannelHandlerContext)}
     * event to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * 触发方法ChannelPipeline.ChannelInboundHandlers.next().channelReadComplete()
     */
    ChannelInboundInvoker fireChannelReadComplete();

    /**
     * Triggers an {@link ChannelInboundHandler#channelWritabilityChanged(ChannelHandlerContext)}
     * event to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * 触发方法ChannelPipeline.ChannelInboundHandlers.next().channelWritabilityChanged()
     */
    ChannelInboundInvoker fireChannelWritabilityChanged();
}
