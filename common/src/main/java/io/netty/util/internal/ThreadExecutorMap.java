/*
 * Copyright 2019 The Netty Project
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
package io.netty.util.internal;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Allow to retrieve the {@link EventExecutor} for the calling {@link Thread}.
 *
 * 允许找回EventExecutor，给调用的Thread。
 */
public final class ThreadExecutorMap {

    private static final FastThreadLocal<EventExecutor> mappings = new FastThreadLocal<EventExecutor>();

    private ThreadExecutorMap() { }

    /**
     * Returns the current {@link EventExecutor} that uses the {@link Thread}, or {@code null} if none / unknown.
     *
     * 返回当前EventExecutor使用的Thread，或者null，如果不存在，或未知。
     */
    public static EventExecutor currentExecutor() {
        return mappings.get();
    }

    /**
     * Set the current {@link EventExecutor} that is used by the {@link Thread}.
     *
     * 设置当前EventExecutor，被Thread使用。
     */
    private static void setCurrentEventExecutor(EventExecutor executor) {
        mappings.set(executor);
    }

    /**
     * Decorate the given {@link Executor} and ensure {@link #currentExecutor()} will return {@code eventExecutor}
     * when called from within the {@link Runnable} during execution.
     *
     * 装饰给定的Executor，保证#currentExecutor()将返回eventExecutor，在执行期间从Runnable内部被调用。
     */
    public static Executor apply(final Executor executor, final EventExecutor eventExecutor) {
        ObjectUtil.checkNotNull(executor, "executor");
        ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
        // 创建一个新的Executor，使用eventExecutor来执行提交到创建的Executor中执行的command。
        // 这里保证在当前线程获取事件执行器时，必然返回eventExecutor。
        // 反过来判断，若eventExecutor为空，则需要绑定一个。
        return new Executor() {
            @Override
            public void execute(final Runnable command) {
                // command与eventExecutor是绑定的，但是executor与command没有任何关系
                executor.execute(apply(command, eventExecutor));
            }
        };
    }

    /**
     * Decorate the given {@link Runnable} and ensure {@link #currentExecutor()} will return {@code eventExecutor}
     * when called from within the {@link Runnable} during execution.
     *
     * 装饰给定的Runnable，保证#currentExecutor()将返回eventExecutor，当被从在Runnable执行期间中调用。
     *
     * NOTE 这个方法，实际上是为了使用#currentExecutor()来执行command。
     */
    public static Runnable apply(final Runnable command, final EventExecutor eventExecutor) {
        ObjectUtil.checkNotNull(command, "command");
        ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
        return new Runnable() {
            @Override
            public void run() {
                setCurrentEventExecutor(eventExecutor);
                try {
                    // run方法的执行，应该会使用到#currentExecutor()
                    // 此方法实际上是会循环执行，直到线程池shutdown才结束循环，而退出。
                    command.run();
                } finally {
                    // 这里应该理解为，释放eventExecutor
                    setCurrentEventExecutor(null);
                }
            }
        };
    }

    /**
     * Decorate the given {@link ThreadFactory} and ensure {@link #currentExecutor()} will return {@code eventExecutor}
     * when called from within the {@link Runnable} during execution.
     *
     * 装饰给定的ThreadFactory，保证#currentExecutor()将返回eventExecutor，当被从Runnable执行期间调用。
     */
    public static ThreadFactory apply(final ThreadFactory threadFactory, final EventExecutor eventExecutor) {
        ObjectUtil.checkNotNull(threadFactory, "command");
        ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return threadFactory.newThread(apply(r, eventExecutor));
            }
        };
    }
}
