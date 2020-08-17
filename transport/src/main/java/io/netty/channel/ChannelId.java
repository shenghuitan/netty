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

package io.netty.channel;

import java.io.Serializable;

/**
 * Represents the globally unique identifier of a {@link Channel}.
 *
 * 代表Channel的一个全局唯一标识符。
 *
 * <p>
 * The identifier is generated from various sources listed in the following:
 *
 * 标识符由以下列出的多个来源生成：
 *
 * <ul>
 * <li>MAC address (EUI-48 or EUI-64) or the network adapter, preferably a globally unique one,</li>
 * <li>the current process ID,</li>
 * <li>{@link System#currentTimeMillis()},</li>
 * <li>{@link System#nanoTime()},</li>
 * <li>a random 32-bit integer, and</li>
 * <li>a sequentially incremented 32-bit integer.</li>
 * </ul>
 *
 * MAC地址，或者网络适配器，最好是一个全局唯一的，
 * 当前的进程ID，
 * 当前系统毫秒时间，
 * 当前系统纳秒时间，
 * 一个随机的32位整数，
 * 一个单调递增的32位整数
 *
 * </p>
 * <p>
 * The global uniqueness of the generated identifier mostly depends on the MAC address and the current process ID,
 * which are auto-detected at the class-loading time in best-effort manner.  If all attempts to acquire them fail,
 * a warning message is logged, and random values will be used instead.  Alternatively, you can specify them manually
 * via system properties:
 *
 * 一个全局唯一生成标识符最大程度依赖MAC地址和当前的进程ID，它是自动发现的在class-loading时所在的最大努力的方式。
 * 如果所有的尝试都失败了，将会记录一个警告消息，并且将使用随机值来代替。可选的，你能手动通过系统属性指定它们：
 *
 * <ul>
 * <li>{@code io.netty.machineId} - hexadecimal representation of 48 (or 64) bit integer,
 *     optionally separated by colon or hyphen.</li>
 * <li>{@code io.netty.processId} - an integer between 0 and 65535</li>
 * </ul>
 *
 * {@code io.netty.machineId} - 48或64位整数的十六进制表现，可选地以冒号或连字号隔离。
 * {@code io.netty.processId} - 一个0 - 65535间的整数
 *
 * </p>
 */
public interface ChannelId extends Serializable, Comparable<ChannelId> {
    /**
     * Returns the short but globally non-unique string representation of the {@link ChannelId}.
     *
     * 返回ChannelId的全局不唯一的短字符串标识。
     */
    String asShortText();

    /**
     * Returns the long yet globally unique string representation of the {@link ChannelId}.
     *
     * 返回ChannelId的全局唯一长字符串标识。
     */
    String asLongText();
}
