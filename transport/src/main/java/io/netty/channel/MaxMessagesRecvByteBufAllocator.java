/*
 * Copyright 2015 The Netty Project
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
 * {@link RecvByteBufAllocator} that limits the number of read operations that will be attempted when a read operation
 * is attempted by the event loop.
 *
 * RecvByteBufAllocator将被尝试限制读操作的数量，当一个读操作被event loop尝试的时候。
 */
public interface MaxMessagesRecvByteBufAllocator extends RecvByteBufAllocator {
    /**
     * Returns the maximum number of messages to read per read loop.
     * a {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object) channelRead()} event.
     * If this value is greater than 1, an event loop might attempt to read multiple times to procure multiple messages.
     *
     * 返回每一个read loop的消息的最大值。一个channelRead事件。
     * 如果这个值大于1，一个事件循环可能尝试读取多次拉取多个消息。
     */
    int maxMessagesPerRead();

    /**
     * Sets the maximum number of messages to read per read loop.
     * If this value is greater than 1, an event loop might attempt to read multiple times to procure multiple messages.
     *
     * 设置单个读循环的消息的最大数量。
     * 如果这个值大于1，event loop可能尝试多次读取，去拉取多个消息。
     */
    MaxMessagesRecvByteBufAllocator maxMessagesPerRead(int maxMessagesPerRead);
}
