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

import io.netty.util.concurrent.GenericProgressiveFutureListener;

import java.util.EventListener;

/**
 * An {@link EventListener} listener which will be called once the sending task associated with future is
 * being transferred.
 *
 * 一个EventListener监听器，将被调用，一旦发送任务关联的future正在传输。
 */
public interface ChannelProgressiveFutureListener extends GenericProgressiveFutureListener<ChannelProgressiveFuture> {
    // Just a type alias
}
