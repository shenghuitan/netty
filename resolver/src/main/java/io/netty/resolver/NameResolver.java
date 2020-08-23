/*
 * Copyright 2014 The Netty Project
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

package io.netty.resolver;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;
import java.util.List;

/**
 * Resolves an arbitrary string that represents the name of an endpoint into an address.
 *
 * 解析一个任意string，代表终端到一个地址的名字。
 */
public interface NameResolver<T> extends Closeable {

    /**
     * Resolves the specified name into an address.
     *
     * 解析指定的名字到地址。
     *
     * @param inetHost the name to resolve
     *                 解析的名字
     *
     * @return the address as the result of the resolution
     *         地址解析的结果
     */
    Future<T> resolve(String inetHost);

    /**
     * Resolves the specified name into an address.
     *
     * @param inetHost the name to resolve
     * @param promise the {@link Promise} which will be fulfilled when the name resolution is finished
     *
     * @return the address as the result of the resolution
     */
    Future<T> resolve(String inetHost, Promise<T> promise);

    /**
     * Resolves the specified host name and port into a list of address.
     *
     * 解析指定的host名称，装入到一个地址集合中。
     *
     * @param inetHost the name to resolve
     *                 解析的名字
     *
     * @return the list of the address as the result of the resolution
     *         地址集合，解析的结果集。
     */
    Future<List<T>> resolveAll(String inetHost);

    /**
     * Resolves the specified host name and port into a list of address.
     *
     * @param inetHost the name to resolve
     * @param promise the {@link Promise} which will be fulfilled when the name resolution is finished
     *
     * @return the list of the address as the result of the resolution
     */
    Future<List<T>> resolveAll(String inetHost, Promise<List<T>> promise);

    /**
     * Closes all the resources allocated and used by this resolver.
     *
     * 关闭所有资源，分配和使用到的，给这个解析器。
     */
    @Override
    void close();
}
