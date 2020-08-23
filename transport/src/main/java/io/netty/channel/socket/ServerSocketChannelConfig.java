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
package io.netty.channel.socket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

import java.net.*;

/**
 * A {@link ChannelConfig} for a {@link ServerSocketChannel}.
 *
 * 一个ChannelConfig给ServerSocketChannel。
 *
 * <h3>Available options</h3>
 *
 * 可用选项
 *
 * In addition to the options provided by {@link ChannelConfig},
 * {@link ServerSocketChannelConfig} allows the following options in the
 * option map:
 *
 * 除了ChannelConfig提供的选项，ServerSocketChannelConfig允许以下选项，在
 * option map中：
 *
 * <table border="1" cellspacing="0" cellpadding="6">
 * <tr>
 * <th>Name</th><th>Associated setter method</th>
 *
 * 名字   关联的setter方法
 *
 * </tr><tr>
 * <td>{@code "backlog"}</td><td>{@link #setBacklog(int)}</td>
 *
 * backlog  #setBacklog(int)
 *
 * </tr><tr>
 * <td>{@code "reuseAddress"}</td><td>{@link #setReuseAddress(boolean)}</td>
 *
 * reuseAddress #setReuseAddress(boolean)
 *
 * </tr><tr>
 * <td>{@code "receiveBufferSize"}</td><td>{@link #setReceiveBufferSize(int)}</td>
 *
 * receiveBufferSize    #setReceiveBufferSize(int)
 *
 * </tr>
 * </table>
 */
public interface ServerSocketChannelConfig extends ChannelConfig {

    /**
     * Gets the backlog value to specify when the channel binds to a local
     * address.
     *
     * 获取积压值，指明当channel绑定一个本地地址的时候。
     */
    int getBacklog();

    /**
     * Sets the backlog value to specify when the channel binds to a local
     * address.
     *
     * 设置积压值，指明当channel绑定一个本地地址的时候。
     */
    ServerSocketChannelConfig setBacklog(int backlog);

    /**
     * Re-use address.
     *
     * 重用地址。
     *
     * <p> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. The exact semantics of this
     * socket option are socket type and system dependent.
     *
     * 当前socket选项的值是一个Boolean，代表了选项是否开启或关闭。这个socket选项的精确语义
     * 是socket类型和系统依赖。
     *
     * <p> In the case of stream-oriented sockets, this socket option will
     * usually determine whether the socket can be bound to a socket address
     * when a previous connection involving that socket address is in the
     * <em>TIME_WAIT</em> state. On implementations where the semantics differ,
     * and the socket option is not required to be enabled in order to bind the
     * socket when a previous connection is in this state, then the
     * implementation may choose to ignore this option.
     *
     * 如果是面向stream的sockets，这个socket option将通常决定socket是否可以绑定到一个
     * socket地址，当前一个连接涉及到socket地址在TIME_WAIT状态的时候。在实现上，语义学
     * 不同于，socket option不需要允许按顺序绑定socket，当前一个连接在这个状态，然后实现
     * 可以选择忽略这个选项。
     *
     * <p> For datagram-oriented sockets the socket option is used to allow
     * multiple programs bind to the same address. This option should be enabled
     * when the socket is to be used for Internet Protocol (IP) multicasting.
     *
     * 对于面向数据报的sockets，socket option用于允许多个程序绑定到相同的地址。这个option
     * 应该被允许，当socket将用于IP多播的时候。
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Changing the value of this socket option
     * after the socket is bound has no effect. The default value of this
     * socket option is system dependent.
     *
     * 一个实现允许这个socket option被设置，在socket被绑定或连接之前。改变这个socket option
     * 的值，在socket绑定之后将没有效果。这个socket option的默认值取决于系统。
     *
     * @see <a href="http://www.ietf.org/rfc/rfc793.txt">RFC&nbsp;793: Transmission
     * Control Protocol</a>
     * @see ServerSocket#setReuseAddress
     */
    /**
     * Gets the {@link StandardSocketOptions#SO_REUSEADDR} option.
     *
     * 获取 StandardSocketOptions#SO_REUSEADDR 选项。
     */
    boolean isReuseAddress();

    /**
     * Sets the {@link StandardSocketOptions#SO_REUSEADDR} option.
     *
     * 设置 StandardSocketOptions#SO_REUSEADDR 选项。
     */
    ServerSocketChannelConfig setReuseAddress(boolean reuseAddress);

    /**
     * The size of the socket receive buffer.
     *
     * socket接收缓冲区的大小。
     *
     * <p> The value of this socket option is an {@code Integer} that is the
     * size of the socket receive buffer in bytes. The socket receive buffer is
     * an input buffer used by the networking implementation. It may need to be
     * increased for high-volume connections or decreased to limit the possible
     * backlog of incoming data. The value of the socket option is a
     * <em>hint</em> to the implementation to size the buffer and the actual
     * size may differ.
     *
     * 高音量、体积；积压；暗示
     * 这个 socket option 的值是一个整数，socket 接收缓冲区的字节大小。socket缓冲区
     * 是一个输入缓冲区用于网络实现。它需要被高容量的连接所增长，或者被传入的数据积压限制
     * 而缩小。socket option的值是一个暗示实现缓存区的大小和实际大小可能不同。
     *
     * <p> For datagram-oriented sockets, the size of the receive buffer may
     * limit the size of the datagrams that can be received. Whether datagrams
     * larger than the buffer size can be received is system dependent.
     * Increasing the socket receive buffer may be important for cases where
     * datagrams arrive in bursts faster than they can be processed.
     *
     * 对于面向报文的socket，接收缓冲区的大小可能限制了可能接收的数据报的大小。数据报
     * 大于缓冲区的大小，它是否可以被接收取决于系统。
     * socket接收缓冲区的增加可能重要，对于数据报接收在瞬时超过它能处理的大小的情况。
     *
     * <p> In the case of stream-oriented sockets and the TCP/IP protocol, the
     * size of the socket receive buffer may be used when advertising the size
     * of the TCP receive window to the remote peer.
     *
     * 对于面向stream的sockets，和TCP/IP协议，socket接收缓冲区的大小应该被使用到通知
     * TCP接收窗口的大小给远端。
     *
     * <p> The initial/default size of the socket receive buffer and the range
     * of allowable values is system dependent although a negative size is not
     * allowed. An attempt to set the socket receive buffer to larger than its
     * maximum size causes it to be set to its maximum size.
     *
     * socket接收缓冲区的初始或默认大小，和允许的范围值取决于系统，虽然一个负值大小是不
     * 允许的。尝试设置socket接收缓冲区大于它的最大值将会被设置成它的最大值。
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Whether an implementation allows the
     * socket receive buffer to be changed after the socket is bound is system
     * dependent.
     *
     * 一个实现允许这个socket option被设置，在socket绑定或连接之前。一个实现是否允许
     * socket接收缓冲区被改变，在socket绑定之后，是取决于不同的系统。
     *
     * @see <a href="http://www.ietf.org/rfc/rfc1323.txt">RFC&nbsp;1323: TCP
     * Extensions for High Performance</a>
     * @see Socket#setReceiveBufferSize
     * @see ServerSocket#setReceiveBufferSize
     */
    /**
     * Gets the {@link StandardSocketOptions#SO_RCVBUF} option.
     *
     * 获取 StandardSocketOptions#SO_RCVBUF option 的值。
     */
    int getReceiveBufferSize();

    /**
     * Sets a default proposed value for the
     * {@link SocketOptions#SO_RCVBUF SO_RCVBUF} option for sockets
     * accepted from this {@code ServerSocket}. The value actually set
     * in the accepted socket must be determined by calling
     * {@link Socket#getReceiveBufferSize()} after the socket
     * is returned by {@link #accept()}.
     * <p>
     * The value of {@link SocketOptions#SO_RCVBUF SO_RCVBUF} is used both to
     * set the size of the internal socket receive buffer, and to set the size
     * of the TCP receive window that is advertized to the remote peer.
     * <p>
     * It is possible to change the value subsequently, by calling
     * {@link Socket#setReceiveBufferSize(int)}. However, if the application
     * wishes to allow a receive window larger than 64K bytes, as defined by RFC1323
     * then the proposed value must be set in the ServerSocket <B>before</B>
     * it is bound to a local address. This implies, that the ServerSocket must be
     * created with the no-argument constructor, then setReceiveBufferSize() must
     * be called and lastly the ServerSocket is bound to an address by calling bind().
     * <p>
     * Failure to do this will not cause an error, and the buffer size may be set to the
     * requested value but the TCP receive window in sockets accepted from
     * this ServerSocket will be no larger than 64K bytes.
     *
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error.
     *
     * @param size the size to which to set the receive buffer
     * size. This value must be greater than 0.
     *
     * @exception IllegalArgumentException if the
     * value is 0 or is negative.
     *
     * @since 1.4
     * @see #getReceiveBufferSize
     */
    /**
     * Gets the {@link StandardSocketOptions#SO_SNDBUF} option.
     *
     * 获取 StandardSocketOptions#SO_SNDBUF option。
     *
     * NOTE 这里的注释是不是不对？注释确实不对。
     *
     * getImpl().setOption(SocketOptions.SO_RCVBUF, new Integer(size));
     */
    ServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

    /**
     * Sets the performance preferences as specified in
     * {@link ServerSocket#setPerformancePreferences(int, int, int)}.
     */
    ServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

    @Override
    ServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    @Deprecated
    ServerSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    @Override
    ServerSocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    ServerSocketChannelConfig setAllocator(ByteBufAllocator allocator);

    @Override
    ServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    @Override
    ServerSocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    ServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);

    @Override
    ServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);

    @Override
    ServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);

    @Override
    ServerSocketChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark);

}
