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
package io.netty.channel;

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Special {@link EventExecutorGroup} which allows registering {@link Channel}s that get
 * processed for later selection during the event loop.
 *
 * 特殊的EventExecutorGroup，将允许注册Channels，在后面的事件循环中，被选择处理。
 *
 * NOTE EventExecutorGroup类似线程池，EventLoopGroup才真正关联了Channel，即EventLoopGroup是与
 * IO相关的，而EventExecutorGroup是IO无关的。
 *
 */
public interface EventLoopGroup extends EventExecutorGroup {
    /**
     * Return the next {@link EventLoop} to use
     *
     * 返回下一个可用的EventLoop
     */
    @Override
    EventLoop next();

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The returned {@link ChannelFuture}
     * will get notified once the registration was complete.
     *
     * 当前实例是一个 EventLoop，使用当前EventLoop来注册一个Channel。它返回的ChannelFuture将被通知，当注册
     * 完成时。
     *
     */
    ChannelFuture register(Channel channel);

    /**
     * Register a {@link Channel} with this {@link EventLoop} using a {@link ChannelFuture}. The passed
     * {@link ChannelFuture} will get notified once the registration was complete and also will get returned.
     *
     * 使用当前的EventLoop来注册一个Channel，Channel归属于传入的ChannelFuture中。传入的ChannelFuture将得到通知，
     * 当注册完成后，并且把传入的ChannelFuture返回。
     */
    ChannelFuture register(ChannelPromise promise);

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The passed {@link ChannelFuture}
     * will get notified once the registration was complete and also will get returned.
     *
     * 使用当前EventLoop注册一个Channel。传入的ChannelFuture将得到一个通知，在完成的时候，且被返回。
     *
     * @deprecated Use {@link #register(ChannelPromise)} instead.
     */
    @Deprecated
    ChannelFuture register(Channel channel, ChannelPromise promise);
}
