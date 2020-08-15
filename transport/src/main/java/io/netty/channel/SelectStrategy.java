/*
 * Copyright 2016 The Netty Project
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

import io.netty.util.IntSupplier;

/**
 * Select strategy interface.
 *
 * 选择策略接口，其子类仅有DefaultSelectStrategy。
 * Netty仅提供了一种实现，暴露了接口，供自定义实现一个新的SelectStrategy。
 *
 * Provides the ability to control the behavior of the select loop. For example a blocking select
 * operation can be delayed or skipped entirely if there are events to process immediately.
 *
 * 提供能力去控制select loop的行为。例如：一个blocking select操作可以是延迟，或者完全跳过，当有事件需要马上
 * 处理的时候。
 *
 */
public interface SelectStrategy {

    /**
     * Indicates a blocking select should follow.
     *
     * 表示应该执行一个阻塞select。
     */
    int SELECT = -1;
    /**
     * Indicates the IO loop should be retried, no blocking select to follow directly.
     *
     * 表示IO循环应被重试，没有阻塞select去马上执行。
     */
    int CONTINUE = -2;
    /**
     * Indicates the IO loop to poll for new events without blocking.
     *
     * 表示IO循环以非阻塞的方式来轮询新事件
     */
    int BUSY_WAIT = -3;

    /**
     * The {@link SelectStrategy} can be used to steer the outcome of a potential select
     * call.
     *
     * 可使用的SelectStrategy，用来转向潜在选择的调用的结果。
     *
     * @param selectSupplier The supplier with the result of a select result.
     *                       选择器的提供商，用于提供select的结果。
     *
     * @param hasTasks true if tasks are waiting to be processed.
     *                 true，代表热舞正在等待被处理。
     *
     * @return {@link #SELECT} if the next step should be blocking select {@link #CONTINUE} if
     *         the next step should be to not select but rather jump back to the IO loop and try
     *         again. Any value >= 0 is treated as an indicator that work needs to be done.
     *
     *         #SELECT 如果选择下一步将被阻塞
     *         #CONTINUE 如果下一步将不被选择，宁可往后跳到IO循环，并且重试。
     *         任何一个 >= 0 的值都被对待为一个指示任务需要被接收。
     */
    int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;
}
