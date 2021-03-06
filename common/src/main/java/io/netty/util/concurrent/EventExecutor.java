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
package io.netty.util.concurrent;

/**
 * The {@link EventExecutor} is a special {@link EventExecutorGroup} which comes
 * with some handy methods to see if a {@link Thread} is executed in a event loop.
 * Besides this, it also extends the {@link EventExecutorGroup} to allow for a generic
 * way to access methods.
 *
 * EventExecutor是一个特殊的EventExecutorGroup，附带了一些便利的方法，去观察一个线程Thread是否在一个event loop中执行。
 * 除此之外，它也提供了EventExecutorGroup去允许一个通用的方式去进入方法。
 *
 */
public interface EventExecutor extends EventExecutorGroup {

    /**
     * Returns a reference to itself.
     *
     * 返回自身
     */
    @Override
    EventExecutor next();

    /**
     * Return the {@link EventExecutorGroup} which is the parent of this {@link EventExecutor},
     *
     * 返回EventExecutor的父亲，即管理EventExecutor的EventExecutorGroup。
     */
    EventExecutorGroup parent();

    /**
     * Calls {@link #inEventLoop(Thread)} with {@link Thread#currentThread()} as argument
     *
     * 调用inEventLoop(Thread)方法来查看当前线程是否在此EventExecutor中管理。
     *
     * NOTE 对这个方法一直有疑问！！！
     *
     * 是否在事件Loop中，即是否Worker。
     * 若为Boss，返回false；为Worker，返回true。
     */
    boolean inEventLoop();

    /**
     * Return {@code true} if the given {@link Thread} is executed in the event loop,
     * {@code false} otherwise.
     *
     * 返回true，若是指定的Thread是在此event loop中执行的。否则返回false。
     *
     * 此事件上次执行的线程，与方法参数的线程，是否同一个线程。
     * 若相同则执行，不同则查找上次执行的线程后，再使用上次的线程来执行？
     *
     * 也就是事件和线程强绑定？
     *
     * io.netty.util.concurrent.SingleThreadEventExecutor#inEventLoop(java.lang.Thread)
     * 只有SingleThreadEventExecutor判断了执行绑定线程是否当前线程。
     */
    boolean inEventLoop(Thread thread);

    /**
     * Return a new {@link Promise}.
     */
    <V> Promise<V> newPromise();

    /**
     * Create a new {@link ProgressivePromise}.
     */
    <V> ProgressivePromise<V> newProgressivePromise();

    /**
     * Create a new {@link Future} which is marked as succeeded already. So {@link Future#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    <V> Future<V> newSucceededFuture(V result);

    /**
     * Create a new {@link Future} which is marked as failed already. So {@link Future#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    <V> Future<V> newFailedFuture(Throwable cause);
}
