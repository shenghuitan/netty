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
package io.netty.channel;

import java.util.Queue;

/**
 * Factory used to create {@link Queue} instances that will be used to store tasks for an {@link EventLoop}.
 *
 * Generally speaking the returned {@link Queue} MUST be thread-safe and depending on the {@link EventLoop}
 * implementation must be of type {@link java.util.concurrent.BlockingQueue}.
 *
 * 工厂类的应用
 *
 * 用于创建Queue实例的工厂类，将使用于存储任务，提供EventLoop使用。
 *
 * 一般来说，返回的Queue必须是线程安全的，并且根据EventLoop的实现，它必须是{@link java.util.concurrent.BlockingQueue}
 * 的子类。
 */
public interface EventLoopTaskQueueFactory {

    /**
     * Returns a new {@link Queue} to use.
     *
     * 返回一个Queue实例。
     *
     * @param maxCapacity the maximum amount of elements that can be stored in the {@link Queue} at a given point
     *                    in time.
     *                    Queue的可存储的元素的最大个数，在给定的时间点。
     *                    怎样描述这个时间点？
     *
     * @return the new queue.
     *         返回Queue，默认实现是返回LinkedBlockingQueue。
     */
    Queue<Runnable> newTaskQueue(int maxCapacity);
}
