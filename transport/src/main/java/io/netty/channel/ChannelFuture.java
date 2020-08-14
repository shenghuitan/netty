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

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.BlockingOperationException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.TimeUnit;


/**
 * The result of an asynchronous {@link Channel} I/O operation.
 *
 * Channel IO操作的异步结果。
 *
 * <p>
 * All I/O operations in Netty are asynchronous.  It means any I/O calls will
 * return immediately with no guarantee that the requested I/O operation has
 * been completed at the end of the call.  Instead, you will be returned with
 * a {@link ChannelFuture} instance which gives you the information about the
 * result or status of the I/O operation.
 *
 * 所有的IO操作在Netty中都是异步的。那意味着任何的IO调用都不保证马上返回，在最后完成的时刻。
 * 代替的，你可以通过ChannelFuture实例来得到IO操作的结果或状态的相关信息。
 *
 * <p>
 * A {@link ChannelFuture} is either <em>uncompleted</em> or <em>completed</em>.
 * When an I/O operation begins, a new future object is created.  The new future
 * is uncompleted initially - it is neither succeeded, failed, nor cancelled
 * because the I/O operation is not finished yet.  If the I/O operation is
 * finished either successfully, with failure, or by cancellation, the future is
 * marked as completed with more specific information, such as the cause of the
 * failure.  Please note that even failure and cancellation belong to the
 * completed state.
 *
 * ChannelFuture要么是uncompleted，或completed。
 * 当IO操作开始，一个新的future对象被创建。新的future被初始化为uncompleted —— 既不是succeeded、
 * failed、也不是cancelled，因为IO操作还没有完成。若IO操作已完成，要么是成功，要么是失败，要么是
 * 取消，future将被标记为完成，并携带更多的信息。比如失败的原因。
 * 请注意失败和取消，也是成功的一种状态。
 *
 * <pre>
 *                                      +---------------------------+
 *                                      | Completed successfully    |
 *                                      +---------------------------+
 *                                 +---->      isDone() = true      |
 * +--------------------------+    |    |   isSuccess() = true      |
 * |        Uncompleted       |    |    +===========================+
 * +--------------------------+    |    | Completed with failure    |
 * |      isDone() = false    |    |    +---------------------------+
 * |   isSuccess() = false    |----+---->      isDone() = true      |
 * | isCancelled() = false    |    |    |       cause() = non-null  |
 * |       cause() = null     |    |    +===========================+
 * +--------------------------+    |    | Completed by cancellation |
 *                                 |    +---------------------------+
 *                                 +---->      isDone() = true      |
 *                                      | isCancelled() = true      |
 *                                      +---------------------------+
 * </pre>
 *
 * Various methods are provided to let you check if the I/O operation has been
 * completed, wait for the completion, and retrieve the result of the I/O
 * operation. It also allows you to add {@link ChannelFutureListener}s so you
 * can get notified when the I/O operation is completed.
 *
 * 各种方法被提供来检查IO操作是否已完成、等待完成，且找到IO操作的结果。它允许你添加
 * ChannelFutureListeners，用来获得IO操作结束时的通知。
 *
 * <h3>Prefer {@link #addListener(GenericFutureListener)} to {@link #await()}</h3>
 *
 * 更建议使用#addListener(GenericFutureListener)方法，而不是#await()方法。
 *
 * It is recommended to prefer {@link #addListener(GenericFutureListener)} to
 * {@link #await()} wherever possible to get notified when an I/O operation is
 * done and to do any follow-up tasks.
 *
 * 不管在任何情况下，当需要IO操作完成后继续执行后续任务，都推荐使用
 * #addListener(GenericFutureListener)方法，而不是#await()方法。
 *
 * <p>
 * {@link #addListener(GenericFutureListener)} is non-blocking.  It simply adds
 * the specified {@link ChannelFutureListener} to the {@link ChannelFuture}, and
 * I/O thread will notify the listeners when the I/O operation associated with
 * the future is done.  {@link ChannelFutureListener} yields the best
 * performance and resource utilization because it does not block at all, but
 * it could be tricky to implement a sequential logic if you are not used to
 * event-driven programming.
 *
 * #addListener(GenericFutureListener)方法是非阻塞的。它简单地添加指定的ChannelFutureListener
 * 到ChannelFuture，当future关联的IO操作完成时，IO线程将通知所有的listeners。
 * ChannelFutureListener让出虽好的性能和资源利用率，因为它完全不是阻塞的，但是它将会变得非常棘手去
 * 实现一系列逻辑，如果你不用事件驱动编程。
 *
 * <p>
 * By contrast, {@link #await()} is a blocking operation.  Once called, the
 * caller thread blocks until the operation is done.  It is easier to implement
 * a sequential logic with {@link #await()}, but the caller thread blocks
 * unnecessarily until the I/O operation is done and there's relatively
 * expensive cost of inter-thread notification.  Moreover, there's a chance of
 * dead lock in a particular circumstance, which is described below.
 *
 * 相对的，#await()是一个阻塞操作。一旦被调用，调用线程将阻塞知道操作完成。它很容易实现一系列
 * 逻辑，通过使用#await()方法。但是调用线程会进行不必要的阻塞，知道IO操作完成，并且相对昂贵地
 * 使用内部线程通知。而且，还有可能发生死锁，在特定的情况下，以下说明这种情况。
 *
 * <h3>Do not call {@link #await()} inside {@link ChannelHandler}</h3>
 *
 * 不要调用#await()方法，在ChannelHandler内部。
 *
 * <p>
 * The event handler methods in {@link ChannelHandler} are usually called by
 * an I/O thread.  If {@link #await()} is called by an event handler
 * method, which is called by the I/O thread, the I/O operation it is waiting
 * for might never complete because {@link #await()} can block the I/O
 * operation it is waiting for, which is a dead lock.
 *
 * 事件处理方法在ChannelHandler里是经常被IO线程调用的。如果#await()方法被一个事件处理方法
 * 调用，即它将会被IO线程调用。所等待的IO操作将永远不会完成，因为#await()可以阻塞正等待的IO操作，
 * 因此发生了死锁。
 *
 * <pre>
 * // BAD - NEVER DO THIS
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *     future.awaitUninterruptibly();
 *     // Perform post-closure operation
 *     // ...
 * }
 *
 * // GOOD
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *     future.addListener(new {@link ChannelFutureListener}() {
 *         public void operationComplete({@link ChannelFuture} future) {
 *             // Perform post-closure operation
 *             // ...
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * In spite of the disadvantages mentioned above, there are certainly the cases
 * where it is more convenient to call {@link #await()}. In such a case, please
 * make sure you do not call {@link #await()} in an I/O thread.  Otherwise,
 * {@link BlockingOperationException} will be raised to prevent a dead lock.
 *
 * 尽管有以上提到的这些缺点，#await()方法还是有特定场景下的作为更加便利的方法的。在这种情况
 * 下，请确定不要在IO线程中调用#await()方法。否则BlockingOperationException将被抛出来
 * 阻止死锁。
 *
 * <h3>Do not confuse I/O timeout and await timeout</h3>
 *
 * 不要混淆IO超时和等待超时。
 *
 * The timeout value you specify with {@link #await(long)},
 * {@link #await(long, TimeUnit)}, {@link #awaitUninterruptibly(long)}, or
 * {@link #awaitUninterruptibly(long, TimeUnit)} are not related with I/O
 * timeout at all.  If an I/O operation times out, the future will be marked as
 * 'completed with failure,' as depicted in the diagram above.  For example,
 * connect timeout should be configured via a transport-specific option:
 *
 * #await系列的超时是与IO超时完全无关的。如果IO操作超时，future将标记为failure状态的完成，
 * 正如上图所描绘的。举个例子，连接超时将配置为指定的传输选项：
 *
 * <pre>
 * // BAD - NEVER DO THIS
 * {@link Bootstrap} b = ...;
 * {@link ChannelFuture} f = b.connect(...);
 * f.awaitUninterruptibly(10, TimeUnit.SECONDS);
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     // You might get a NullPointerException here because the future
 *     // might not be completed yet.
 *     f.cause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 *
 * // GOOD
 * {@link Bootstrap} b = ...;
 * // Configure the connect timeout option.
 * <b>b.option({@link ChannelOption}.CONNECT_TIMEOUT_MILLIS, 10000);</b>
 * {@link ChannelFuture} f = b.connect(...);
 * f.awaitUninterruptibly();
 *
 * // Now we are sure the future is completed.
 * assert f.isDone();
 *
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     f.cause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 * </pre>
 */
public interface ChannelFuture extends Future<Void> {

    /**
     * Returns a channel where the I/O operation associated with this
     * future takes place.
     *
     * 返回channel，在发生IO操作关联的future中。
     */
    Channel channel();

    @Override
    ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture sync() throws InterruptedException;

    @Override
    ChannelFuture syncUninterruptibly();

    @Override
    ChannelFuture await() throws InterruptedException;

    @Override
    ChannelFuture awaitUninterruptibly();

    /**
     * Returns {@code true} if this {@link ChannelFuture} is a void future and so not allow to call any of the
     * following methods:
     * <ul>
     *     <li>{@link #addListener(GenericFutureListener)}</li>
     *     <li>{@link #addListeners(GenericFutureListener[])}</li>
     *     <li>{@link #await()}</li>
     *     <li>{@link #await(long, TimeUnit)} ()}</li>
     *     <li>{@link #await(long)} ()}</li>
     *     <li>{@link #awaitUninterruptibly()}</li>
     *     <li>{@link #sync()}</li>
     *     <li>{@link #syncUninterruptibly()}</li>
     * </ul>
     */
    boolean isVoid();
}
