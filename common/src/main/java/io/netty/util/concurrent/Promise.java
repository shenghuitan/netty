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

/**
 * Special {@link Future} which is writable.
 *
 * 特殊的写操作Future
 */
public interface Promise<V> extends Future<V> {

    /**
     * Marks this future as a success and notifies all
     * listeners.
     *
     * 标记此future已成功，且通知所有的listeners。
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     *
     * 如果结果是已经success的，或已经failed，它将抛出IllegalStateException。
     */
    Promise<V> setSuccess(V result);

    /**
     * Marks this future as a success and notifies all
     * listeners.
     *
     * 标记future success，且通知所有listeners。
     *
     * @return {@code true} if and only if successfully marked this future as
     *         a success. Otherwise {@code false} because this future is
     *         already marked as either a success or a failure.
     *
     *         返回true，当且仅当可成功标记此future为success。否则返回false，因为此future已经被
     *         标记为success或failure了。
     */
    boolean trySuccess(V result);

    /**
     * Marks this future as a failure and notifies all
     * listeners.
     *
     * 标记此future为failure，且通知所有listeners。
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     *
     * 如果future已是success或failed，它将抛出IllegalStateException。
     */
    Promise<V> setFailure(Throwable cause);

    /**
     * Marks this future as a failure and notifies all
     * listeners.
     *
     * 标记当前future为failure，且通知所有的listeners。
     *
     * @return {@code true} if and only if successfully marked this future as
     *         a failure. Otherwise {@code false} because this future is
     *         already marked as either a success or a failure.
     *
     *         返回true，当且仅当成功标记future为failure。否则返回false，因为此future已经
     *         被标记为success或failure。
     */
    boolean tryFailure(Throwable cause);

    /**
     * Make this future impossible to cancel.
     *
     * 标记此future为不可能被cancel。
     *
     * @return {@code true} if and only if successfully marked this future as uncancellable or it is already done
     *         without being cancelled.  {@code false} if this future has been cancelled already.
     *
     *         返回true，当且仅当成功标记future为uncancellable，或它已结束，且没有被取消。返回false，如果future
     *         已经被cancel。
     */
    boolean setUncancellable();

    @Override
    Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> await() throws InterruptedException;

    @Override
    Promise<V> awaitUninterruptibly();

    @Override
    Promise<V> sync() throws InterruptedException;

    @Override
    Promise<V> syncUninterruptibly();
}
