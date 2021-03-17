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
package io.netty.channel.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for {@link Channel} implementations which use a Selector based approach.
 */
public abstract class AbstractNioChannel extends AbstractChannel {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(AbstractNioChannel.class);

    /**
     * A channel that can be multiplexed via a {@link Selector}.
     *
     * <p> In order to be used with a selector, an instance of this class must
     * first be <i>registered</i> via the {@link #register(Selector,int,Object)
     * register} method.  This method returns a new {@link SelectionKey} object
     * that represents the channel's registration with the selector.
     *
     * <p> Once registered with a selector, a channel remains registered until it
     * is <i>deregistered</i>.  This involves deallocating whatever resources were
     * allocated to the channel by the selector.
     *
     * <p> A channel cannot be deregistered directly; instead, the key representing
     * its registration must be <i>cancelled</i>.  Cancelling a key requests that
     * the channel be deregistered during the selector's next selection operation.
     * A key may be cancelled explicitly by invoking its {@link
     * SelectionKey#cancel() cancel} method.  All of a channel's keys are cancelled
     * implicitly when the channel is closed, whether by invoking its {@link
     * java.nio.channels.Channel#close close} method or by interrupting a thread blocked in an I/O
     * operation upon the channel.
     *
     * <p> If the selector itself is closed then the channel will be deregistered,
     * and the key representing its registration will be invalidated, without
     * further delay.
     *
     * <p> A channel may be registered at most once with any particular selector.
     *
     * <p> Whether or not a channel is registered with one or more selectors may be
     * determined by invoking the {@link #isRegistered isRegistered} method.
     *
     * <p> Selectable channels are safe for use by multiple concurrent
     * threads. </p>
     *
     *
     * <a name="bm"></a>
     * <h2>Blocking mode</h2>
     *
     * A selectable channel is either in <i>blocking</i> mode or in
     * <i>non-blocking</i> mode.  In blocking mode, every I/O operation invoked
     * upon the channel will block until it completes.  In non-blocking mode an I/O
     * operation will never block and may transfer fewer bytes than were requested
     * or possibly no bytes at all.  The blocking mode of a selectable channel may
     * be determined by invoking its {@link #isBlocking isBlocking} method.
     *
     * <p> Newly-created selectable channels are always in blocking mode.
     * Non-blocking mode is most useful in conjunction with selector-based
     * multiplexing.  A channel must be placed into non-blocking mode before being
     * registered with a selector, and may not be returned to blocking mode until
     * it has been deregistered.
     *
     *
     * @author Mark Reinhold
     * @author JSR-51 Expert Group
     * @since 1.4
     *
     * @see SelectionKey
     * @see Selector
     */
    private final SelectableChannel ch;

    protected final int readInterestOp;

    /**
     * A token representing the registration of a {@link SelectableChannel} with a
     * {@link Selector}.
     *
     * <p> A selection key is created each time a channel is registered with a
     * selector.  A key remains valid until it is <i>cancelled</i> by invoking its
     * {@link #cancel cancel} method, by closing its channel, or by closing its
     * selector.  Cancelling a key does not immediately remove it from its
     * selector; it is instead added to the selector's <a
     * href="Selector.html#ks"><i>cancelled-key set</i></a> for removal during the
     * next selection operation.  The validity of a key may be tested by invoking
     * its {@link #isValid isValid} method.
     *
     * <a name="opsets"></a>
     *
     * <p> A selection key contains two <i>operation sets</i> represented as
     * integer values.  Each bit of an operation set denotes a category of
     * selectable operations that are supported by the key's channel.
     *
     * <ul>
     *
     *   <li><p> The <i>interest set</i> determines which operation categories will
     *   be tested for readiness the next time one of the selector's selection
     *   methods is invoked.  The interest set is initialized with the value given
     *   when the key is created; it may later be changed via the {@link
     *   #interestOps(int)} method. </p></li>
     *
     *   <li><p> The <i>ready set</i> identifies the operation categories for which
     *   the key's channel has been detected to be ready by the key's selector.
     *   The ready set is initialized to zero when the key is created; it may later
     *   be updated by the selector during a selection operation, but it cannot be
     *   updated directly. </p></li>
     *
     * </ul>
     *
     * <p> That a selection key's ready set indicates that its channel is ready for
     * some operation category is a hint, but not a guarantee, that an operation in
     * such a category may be performed by a thread without causing the thread to
     * block.  A ready set is most likely to be accurate immediately after the
     * completion of a selection operation.  It is likely to be made inaccurate by
     * external events and by I/O operations that are invoked upon the
     * corresponding channel.
     *
     * <p> This class defines all known operation-set bits, but precisely which
     * bits are supported by a given channel depends upon the type of the channel.
     * Each subclass of {@link SelectableChannel} defines an {@link
     * SelectableChannel#validOps() validOps()} method which returns a set
     * identifying just those operations that are supported by the channel.  An
     * attempt to set or test an operation-set bit that is not supported by a key's
     * channel will result in an appropriate run-time exception.
     *
     * <p> It is often necessary to associate some application-specific data with a
     * selection key, for example an object that represents the state of a
     * higher-level protocol and handles readiness notifications in order to
     * implement that protocol.  Selection keys therefore support the
     * <i>attachment</i> of a single arbitrary object to a key.  An object can be
     * attached via the {@link #attach attach} method and then later retrieved via
     * the {@link #attachment() attachment} method.
     *
     * <p> Selection keys are safe for use by multiple concurrent threads.  The
     * operations of reading and writing the interest set will, in general, be
     * synchronized with certain operations of the selector.  Exactly how this
     * synchronization is performed is implementation-dependent: In a naive
     * implementation, reading or writing the interest set may block indefinitely
     * if a selection operation is already in progress; in a high-performance
     * implementation, reading or writing the interest set may block briefly, if at
     * all.  In any case, a selection operation will always use the interest-set
     * value that was current at the moment that the operation began.  </p>
     *
     *
     * @author Mark Reinhold
     * @author JSR-51 Expert Group
     * @since 1.4
     *
     * @see SelectableChannel
     * @see Selector
     */
    volatile SelectionKey selectionKey;

    boolean readPending;
    private final Runnable clearReadPendingRunnable = new Runnable() {
        @Override
        public void run() {
            clearReadPending0();
        }
    };

    /**
     * The future of the current connection attempt.  If not null, subsequent
     * connection attempts will fail.
     */
    private ChannelPromise connectPromise;
    private ScheduledFuture<?> connectTimeoutFuture;
    private SocketAddress requestedRemoteAddress;

    /**
     * Create a new instance
     *
     * 创建一个NioChannel实例
     *
     * @param parent            the parent {@link Channel} by which this instance was created. May be {@code null}
     *                          已被创建的新实例的父Channel。可能为null
     *
     * @param ch                the underlying {@link SelectableChannel} on which it operates
     *                          提供的底层SelectableChannel，可能是一个ServerSocketChannel。
     *
     * @param readInterestOp    the ops to set to receive data from the {@link SelectableChannel}
     *                          被设置的操作去接收SelectableChannel的数据
     *
     */
    protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            /**
             * Adjusts this channel's blocking mode.
             *
             * 调整channel的阻塞模式。
             *
             * <p> If this channel is registered with one or more selectors then an
             * attempt to place it into blocking mode will cause an {@link
             * IllegalBlockingModeException} to be thrown.
             *
             * <p> This method may be invoked at any time.  The new blocking mode will
             * only affect I/O operations that are initiated after this method returns.
             * For some implementations this may require blocking until all pending I/O
             * operations are complete.
             *
             * <p> If this method is invoked while another invocation of this method or
             * of the {@link #register(Selector, int) register} method is in progress
             * then it will first block until the other operation is complete. </p>
             *
             * @param  block  If <tt>true</tt> then this channel will be placed in
             *                blocking mode; if <tt>false</tt> then it will be placed
             *                non-blocking mode
             *
             * @return  This selectable channel
             *
             * @throws  ClosedChannelException
             *          If this channel is closed
             *
             * @throws  IllegalBlockingModeException
             *          If <tt>block</tt> is <tt>true</tt> and this channel is
             *          registered with one or more selectors
             *
             * @throws IOException
             *         If an I/O error occurs
             */
            // public static native void configureBlocking(FileDescriptor var0, boolean var1) throws IOException;
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                ch.close();
            } catch (IOException e2) {
                logger.warn(
                            "Failed to close a partially initialized socket.", e2);
            }

            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }

    protected SelectableChannel javaChannel() {
        return ch;
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    /**
     * Return the current {@link SelectionKey}
     */
    protected SelectionKey selectionKey() {
        assert selectionKey != null;
        return selectionKey;
    }

    /**
     * @deprecated No longer supported.
     * No longer supported.
     */
    @Deprecated
    protected boolean isReadPending() {
        return readPending;
    }

    /**
     * @deprecated Use {@link #clearReadPending()} if appropriate instead.
     * No longer supported.
     */
    @Deprecated
    protected void setReadPending(final boolean readPending) {
        if (isRegistered()) {
            EventLoop eventLoop = eventLoop();
            if (eventLoop.inEventLoop()) {
                setReadPending0(readPending);
            } else {
                eventLoop.execute(new Runnable() {
                    @Override
                    public void run() {
                        setReadPending0(readPending);
                    }
                });
            }
        } else {
            // Best effort if we are not registered yet clear readPending.
            // NB: We only set the boolean field instead of calling clearReadPending0(), because the SelectionKey is
            // not set yet so it would produce an assertion failure.
            this.readPending = readPending;
        }
    }

    /**
     * Set read pending to {@code false}.
     */
    protected final void clearReadPending() {
        if (isRegistered()) {
            EventLoop eventLoop = eventLoop();
            if (eventLoop.inEventLoop()) {
                clearReadPending0();
            } else {
                eventLoop.execute(clearReadPendingRunnable);
            }
        } else {
            // Best effort if we are not registered yet clear readPending. This happens during channel initialization.
            // NB: We only set the boolean field instead of calling clearReadPending0(), because the SelectionKey is
            // not set yet so it would produce an assertion failure.
            readPending = false;
        }
    }

    private void setReadPending0(boolean readPending) {
        this.readPending = readPending;
        if (!readPending) {
            ((AbstractNioUnsafe) unsafe()).removeReadOp();
        }
    }

    private void clearReadPending0() {
        readPending = false;
        ((AbstractNioUnsafe) unsafe()).removeReadOp();
    }

    /**
     * Special {@link Unsafe} sub-type which allows to access the underlying {@link SelectableChannel}
     */
    public interface NioUnsafe extends Unsafe {
        /**
         * Return underlying {@link SelectableChannel}
         */
        SelectableChannel ch();

        /**
         * Finish connect
         */
        void finishConnect();

        /**
         * Read from underlying {@link SelectableChannel}
         */
        void read();

        void forceFlush();
    }

    protected abstract class AbstractNioUnsafe extends AbstractUnsafe implements NioUnsafe {

        protected final void removeReadOp() {
            SelectionKey key = selectionKey();
            // Check first if the key is still valid as it may be canceled as part of the deregistration
            // from the EventLoop
            // See https://github.com/netty/netty/issues/2104
            if (!key.isValid()) {
                return;
            }
            int interestOps = key.interestOps();
            if ((interestOps & readInterestOp) != 0) {
                // only remove readInterestOp if needed
                key.interestOps(interestOps & ~readInterestOp);
            }
        }

        @Override
        public final SelectableChannel ch() {
            return javaChannel();
        }

        @Override
        public final void connect(
                final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            try {
                if (connectPromise != null) {
                    // Already a connect in process.
                    throw new ConnectionPendingException();
                }

                boolean wasActive = isActive();
                if (doConnect(remoteAddress, localAddress)) {
                    fulfillConnectPromise(promise, wasActive);
                } else {
                    connectPromise = promise;
                    requestedRemoteAddress = remoteAddress;

                    // Schedule connect timeout.
                    int connectTimeoutMillis = config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        connectTimeoutFuture = eventLoop().schedule(new Runnable() {
                            @Override
                            public void run() {
                                ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
                                ConnectTimeoutException cause =
                                        new ConnectTimeoutException("connection timed out: " + remoteAddress);
                                if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                    close(voidPromise());
                                }
                            }
                        }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
                    }

                    promise.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isCancelled()) {
                                if (connectTimeoutFuture != null) {
                                    connectTimeoutFuture.cancel(false);
                                }
                                connectPromise = null;
                                close(voidPromise());
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                promise.tryFailure(annotateConnectException(t, remoteAddress));
                closeIfClosed();
            }
        }

        private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
            if (promise == null) {
                // Closed via cancellation and the promise has been notified already.
                return;
            }

            // Get the state as trySuccess() may trigger an ChannelFutureListener that will close the Channel.
            // We still need to ensure we call fireChannelActive() in this case.
            boolean active = isActive();

            // trySuccess() will return false if a user cancelled the connection attempt.
            boolean promiseSet = promise.trySuccess();

            // Regardless if the connection attempt was cancelled, channelActive() event should be triggered,
            // because what happened is what happened.
            if (!wasActive && active) {
                pipeline().fireChannelActive();
            }

            // If a user cancelled the connection attempt, close the channel, which is followed by channelInactive().
            if (!promiseSet) {
                close(voidPromise());
            }
        }

        private void fulfillConnectPromise(ChannelPromise promise, Throwable cause) {
            if (promise == null) {
                // Closed via cancellation and the promise has been notified already.
                return;
            }

            // Use tryFailure() instead of setFailure() to avoid the race against cancel().
            promise.tryFailure(cause);
            closeIfClosed();
        }

        @Override
        public final void finishConnect() {
            // Note this method is invoked by the event loop only if the connection attempt was
            // neither cancelled nor timed out.

            assert eventLoop().inEventLoop();

            try {
                boolean wasActive = isActive();
                doFinishConnect();
                fulfillConnectPromise(connectPromise, wasActive);
            } catch (Throwable t) {
                fulfillConnectPromise(connectPromise, annotateConnectException(t, requestedRemoteAddress));
            } finally {
                // Check for null as the connectTimeoutFuture is only created if a connectTimeoutMillis > 0 is used
                // See https://github.com/netty/netty/issues/1770
                if (connectTimeoutFuture != null) {
                    connectTimeoutFuture.cancel(false);
                }
                connectPromise = null;
            }
        }

        @Override
        protected final void flush0() {
            // Flush immediately only when there's no pending flush.
            // If there's a pending flush operation, event loop will call forceFlush() later,
            // and thus there's no need to call it now.
            if (!isFlushPending()) {
                super.flush0();
            }
        }

        @Override
        public final void forceFlush() {
            // directly call super.flush0() to force a flush now
            super.flush0();
        }

        private boolean isFlushPending() {
            SelectionKey selectionKey = selectionKey();
            return selectionKey.isValid() && (selectionKey.interestOps() & SelectionKey.OP_WRITE) != 0;
        }
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof NioEventLoop;
    }

    /**
     * 把Selector注册到SelectableChannel
     *
     * @throws Exception
     */
    @Override
    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {

                /**
                 * Registers this channel with the given selector, returning a selection
                 * key.
                 *
                 * 注册当前channel，使用给定的selector，返回一个selection key。
                 *
                 * <p> If this channel is currently registered with the given selector then
                 * the selection key representing that registration is returned.  The key's
                 * interest set will have been changed to <tt>ops</tt>, as if by invoking
                 * the {@link SelectionKey#interestOps(int) interestOps(int)} method.  If
                 * the <tt>att</tt> argument is not <tt>null</tt> then the key's attachment
                 * will have been set to that value.  A {@link CancelledKeyException} will
                 * be thrown if the key has already been cancelled.
                 *
                 * 若channel目前注册，用给定的selector，然后selection key表示注册的返回。selection key
                 * 的interest set将转变成ops，如果通过调用SelectionKey#interestOps(int)方法。
                 * 如果att参数非null，key的attachment将已经设置为这个值。CancelledKeyException
                 * 将被抛出，若key已经被取消。
                 *
                 * <p> Otherwise this channel has not yet been registered with the given
                 * selector, so it is registered and the resulting new key is returned.
                 * The key's initial interest set will be <tt>ops</tt> and its attachment
                 * will be <tt>att</tt>.
                 *
                 * 除非channel没有被使用给定的selector注册，它将被注册，返回新的结果key。key的初始化
                 * interest set将是ops，且它的attachment将是att。
                 *
                 * <p> This method may be invoked at any time.  If this method is invoked
                 * while another invocation of this method or of the {@link
                 * #configureBlocking(boolean) configureBlocking} method is in progress
                 * then it will first block until the other operation is complete.  This
                 * method will then synchronize on the selector's key set and therefore may
                 * block if invoked concurrently with another registration or selection
                 * operation involving the same selector. </p>
                 *
                 * 这个方法可以在任何时候被调用。如果此方法被调用，当另一个此方法或#configureBlocking(boolean)
                 * 方法的进行中的调用，它将首先阻塞，直到其它操作结束。此方法将根据selector的key set同步，因此
                 * 可能阻塞，如果同时使用另一个注册或selection操作调用相同的selector。
                 *
                 * <p> If this channel is closed while this operation is in progress then
                 * the key returned by this method will have been cancelled and will
                 * therefore be invalid. </p>
                 *
                 * 如果此channel已关闭，当此操作进行中，key将通过此方法返回，将被取消而成为无效。
                 *
                 * @param  sel
                 *         The selector with which this channel is to be registered
                 *         selector，将被注册到channel
                 *
                 * @param  ops
                 *         The interest set for the resulting key
                 *         结果key的interest set
                 *
                 * @param  att
                 *         The attachment for the resulting key; may be <tt>null</tt>
                 *         结果key的attachment；可能为null
                 *
                 * @throws  ClosedChannelException
                 *          If this channel is closed
                 *
                 * @throws  ClosedSelectorException
                 *          If the selector is closed
                 *
                 * @throws  IllegalBlockingModeException
                 *          If this channel is in blocking mode
                 *
                 * @throws  IllegalSelectorException
                 *          If this channel was not created by the same provider
                 *          as the given selector
                 *
                 * @throws  CancelledKeyException
                 *          If this channel is currently registered with the given selector
                 *          but the corresponding key has already been cancelled
                 *
                 * @throws  IllegalArgumentException
                 *          If a bit in the <tt>ops</tt> set does not correspond to an
                 *          operation that is supported by this channel, that is, if
                 *          {@code set & ~validOps() != 0}
                 *
                 * @return  A key representing the registration of this channel with
                 *          the given selector
                 *
                 *          返回一个key，表示使用给定的channel的selector的注册结果
                 */
                // NOTE 第一阶段：这里把NioServerSocketChannel注册到了Selector的attachment
                // 后续从selectedKey获取到的，将是NioServerSocketChannel
                selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
                return;
            } catch (CancelledKeyException e) {
                if (!selected) {
                    // Force the Selector to select now as the "canceled" SelectionKey may still be
                    // cached and not removed because no Select.select(..) operation was called yet.

                    // 强制Selector selectNow，因为canceled SelectionKey可能被缓存，并且没有被删除，因为
                    // 没有Select.select()操作会再被调用。

                    // 意在清空可能的Selector取消事件
                    eventLoop().selectNow();
                    selected = true;
                } else {
                    // We forced a select operation on the selector before but the SelectionKey is still cached
                    // for whatever reason. JDK bug ?
                    throw e;
                }
            }
        }
    }

    @Override
    protected void doDeregister() throws Exception {
        eventLoop().cancel(selectionKey());
    }

    @Override
    protected void doBeginRead() throws Exception {
        // Channel.read() or ChannelHandlerContext.read() was called
        final SelectionKey selectionKey = this.selectionKey;
        if (!selectionKey.isValid()) {
            return;
        }

        readPending = true;

        final int interestOps = selectionKey.interestOps();
        if ((interestOps & readInterestOp) == 0) {
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }

    /**
     * Connect to the remote peer
     */
    protected abstract boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;

    /**
     * Finish the connect
     */
    protected abstract void doFinishConnect() throws Exception;

    /**
     * Returns an off-heap copy of the specified {@link ByteBuf}, and releases the original one.
     * Note that this method does not create an off-heap copy if the allocation / deallocation cost is too high,
     * but just returns the original {@link ByteBuf}..
     */
    protected final ByteBuf newDirectBuffer(ByteBuf buf) {
        final int readableBytes = buf.readableBytes();
        if (readableBytes == 0) {
            ReferenceCountUtil.safeRelease(buf);
            return Unpooled.EMPTY_BUFFER;
        }

        final ByteBufAllocator alloc = alloc();
        if (alloc.isDirectBufferPooled()) {
            ByteBuf directBuf = alloc.directBuffer(readableBytes);
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(buf);
            return directBuf;
        }

        final ByteBuf directBuf = ByteBufUtil.threadLocalDirectBuffer();
        if (directBuf != null) {
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(buf);
            return directBuf;
        }

        // Allocating and deallocating an unpooled direct buffer is very expensive; give up.
        return buf;
    }

    /**
     * Returns an off-heap copy of the specified {@link ByteBuf}, and releases the specified holder.
     * The caller must ensure that the holder releases the original {@link ByteBuf} when the holder is released by
     * this method.  Note that this method does not create an off-heap copy if the allocation / deallocation cost is
     * too high, but just returns the original {@link ByteBuf}..
     */
    protected final ByteBuf newDirectBuffer(ReferenceCounted holder, ByteBuf buf) {
        final int readableBytes = buf.readableBytes();
        if (readableBytes == 0) {
            ReferenceCountUtil.safeRelease(holder);
            return Unpooled.EMPTY_BUFFER;
        }

        final ByteBufAllocator alloc = alloc();
        if (alloc.isDirectBufferPooled()) {
            ByteBuf directBuf = alloc.directBuffer(readableBytes);
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(holder);
            return directBuf;
        }

        final ByteBuf directBuf = ByteBufUtil.threadLocalDirectBuffer();
        if (directBuf != null) {
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(holder);
            return directBuf;
        }

        // Allocating and deallocating an unpooled direct buffer is very expensive; give up.
        if (holder != buf) {
            // Ensure to call holder.release() to give the holder a chance to release other resources than its content.
            buf.retain();
            ReferenceCountUtil.safeRelease(holder);
        }

        return buf;
    }

    @Override
    protected void doClose() throws Exception {
        ChannelPromise promise = connectPromise;
        if (promise != null) {
            // Use tryFailure() instead of setFailure() to avoid the race against cancel().
            promise.tryFailure(new ClosedChannelException());
            connectPromise = null;
        }

        ScheduledFuture<?> future = connectTimeoutFuture;
        if (future != null) {
            future.cancel(false);
            connectTimeoutFuture = null;
        }
    }
}
