/*
 * Copyright 2013 The Netty Project
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;


/**
 * The result of an asynchronous operation.
 *
 * 异步操作的返回结果。
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.
     *
     * 返回true，当且仅当IO操作已成功完成。
     */
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     *
     * 返回true，当且仅当此操作可以被#cancel(boolean)方法取消。
     */
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has
     * failed.
     *
     * 返回IO操作失败的原因，若IO操作已失败。
     *
     * @return the cause of the failure.
     *         {@code null} if succeeded or this future is not
     *         completed yet.
     *
     *         失败的原因。
     *         返回null，若已执行成功，或此future还没有执行完成。
     */
    Throwable cause();

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listener is notified immediately.
     *
     * 添加指定的listener到当前future。
     * 指定的listener将被通知，当此future已经执行完成，#isDone()返回true。
     * 如果此future已经执行完成，指定的listener将被马上通知到。
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listeners are notified immediately.
     *
     * 批量添加listeners，当future执行完成，马上通知所有指定的listeners。
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     *
     * 从当前future移除第一个指定的listener。指定的listener不再被通知到，当future #isDone()。
     * 如果指定的listener没有关联当前的future，此方法将什么都不做，且静默返回future。
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listeners are not associated with this future, this method
     * does nothing and returns silently.
     *
     * 从当前future批量删除所有指定的第一个listener。指定的listeners不再被通知到，当future执行完成。
     * 若指定的listeners与当前future没有关联，此方法将什么都不做，且静默返回。
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     *
     * 等待当前future直到执行完成。若当前future执行失败，则重新抛出失败的原因。
     */
    Future<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     *
     * 等待当前future执行完成，若执行失败则重新抛出异常原因。
     */
    Future<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     *
     * 等待future执行完成。
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     *
     *         若线程被中断，抛出InterruptedException。
     */
    Future<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without
     * interruption.  This method catches an {@link InterruptedException} and
     * discards it silently.
     *
     * 等待当前future执行完成，不带中断异常。
     * 当前方法会捕捉InterruptedException，且静默忽略。
     */
    Future<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * 在指定的时间内，等待future执行成功。
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     *         返回true，当且仅当future在限定的时间内执行完成。
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     *
     *         若当前线程被中断，抛出InterruptedException。
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * 等待当前future在限定的时间内执行完成。
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     *         返回true，当且仅当future在限定的时间内执行完成。
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     *
     *         若当前线程被中断，抛出InterruptedException。
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     *
     * 非阻塞返回执行结果。若future未执行完成，返回null。
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not rely on the returned {@code null} value.
     *
     * 当返回null，是可以被认为future已经成功执行完成，此时你需要同时通过#isDone()检查future是否返回true。且不依赖于返回
     * 的null值。
     */
    V getNow();

    /**
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with a {@link CancellationException}.
     *
     * 若取消操作成功，它将使future失败，原因为CancellationException。
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
