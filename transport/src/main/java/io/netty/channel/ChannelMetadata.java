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

import static io.netty.util.internal.ObjectUtil.checkPositive;

import java.net.SocketAddress;

/**
 * Represents the properties of a {@link Channel} implementation.
 *
 * 代表Channel实现的属性
 */
public final class ChannelMetadata {

    /**
     * 是否已断开连接
     */
    private final boolean hasDisconnect;

    /**
     * 默认单次read loop的最大消息拉取数量
     */
    private final int defaultMaxMessagesPerRead;

    /**
     * Create a new instance
     *
     * @param hasDisconnect     {@code true} if and only if the channel has the {@code disconnect()} operation
     *                          that allows a user to disconnect and then call {@link Channel#connect(SocketAddress)}
     *                          again, such as UDP/IP.
     */
    public ChannelMetadata(boolean hasDisconnect) {
        this(hasDisconnect, 1);
    }

    /**
     * Create a new instance
     *
     * 创建一个新实例
     *
     * @param hasDisconnect     {@code true} if and only if the channel has the {@code disconnect()} operation
     *                          that allows a user to disconnect and then call {@link Channel#connect(SocketAddress)}
     *                          again, such as UDP/IP.
     *
     *                          返回true，当且仅当channel已经调用了断开连接操作，允许用户disconnect，然后再次调用
     *                          Channel#connect(SocketAddress)，比如UDP/IP。
     *
     * @param defaultMaxMessagesPerRead If a {@link MaxMessagesRecvByteBufAllocator} is in use, then this value will be
     * set for {@link MaxMessagesRecvByteBufAllocator#maxMessagesPerRead()}. Must be {@code > 0}.
     *
     *                                  如果一个MaxMessagesRecvByteBufAllocator被使用，则这个值将被设置为
     *                                  MaxMessagesRecvByteBufAllocator#maxMessagesPerRead()。必须 > 0。
     */
    public ChannelMetadata(boolean hasDisconnect, int defaultMaxMessagesPerRead) {
        checkPositive(defaultMaxMessagesPerRead, "defaultMaxMessagesPerRead");
        this.hasDisconnect = hasDisconnect;
        this.defaultMaxMessagesPerRead = defaultMaxMessagesPerRead;
    }

    /**
     * Returns {@code true} if and only if the channel has the {@code disconnect()} operation
     * that allows a user to disconnect and then call {@link Channel#connect(SocketAddress)} again,
     * such as UDP/IP.
     */
    public boolean hasDisconnect() {
        return hasDisconnect;
    }

    /**
     * If a {@link MaxMessagesRecvByteBufAllocator} is in use, then this is the default value for
     * {@link MaxMessagesRecvByteBufAllocator#maxMessagesPerRead()}.
     */
    public int defaultMaxMessagesPerRead() {
        return defaultMaxMessagesPerRead;
    }
}
