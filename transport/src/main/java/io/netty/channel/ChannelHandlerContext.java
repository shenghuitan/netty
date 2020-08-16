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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.concurrent.EventExecutor;

import java.nio.channels.Channels;

/**
 * Enables a {@link ChannelHandler} to interact with its {@link ChannelPipeline}
 * and other handlers. Among other things a handler can notify the next {@link ChannelHandler} in the
 * {@link ChannelPipeline} as well as modify the {@link ChannelPipeline} it belongs to dynamically.
 *
 * 相互作用、除其它事项外
 * 允许ChannelHandler与它的ChannelPipeline里的其它handlers交互。除此之外，一个handler可以通知ChannelPipeline
 * 的下一个ChannelHandler，同时动态修改它所属的ChannelPipeline。
 *
 * <h3>Notify</h3>
 *
 * 通知
 *
 * You can notify the closest handler in the same {@link ChannelPipeline} by calling one of the various methods
 * provided here.
 *
 * 你可以通知最靠近的handler，在相同的ChannelPipeline中，通过调用提供的各种方法中的其中一个。
 *
 * Please refer to {@link ChannelPipeline} to understand how an event flows.
 *
 * 请参考ChannelPipeline，理解一个事件的流转方式。
 *
 * <h3>Modifying a pipeline</h3>
 *
 * 修改Pipeline
 *
 * You can get the {@link ChannelPipeline} your handler belongs to by calling
 * {@link #pipeline()}.  A non-trivial application could insert, remove, or
 * replace handlers in the pipeline dynamically at runtime.
 *
 * 你可以通过调用#pipeline()方法来获得handler所在的ChannelPipeline。一个不一般的应用程序，
 * 可以在运行时在Pipeline中动态插入，删除，或者替换handlers。
 *
 * <h3>Retrieving for later use</h3>
 *
 * 检索以后续使用
 *
 * You can keep the {@link ChannelHandlerContext} for later use, such as
 * triggering an event outside the handler methods, even from a different thread.
 *
 * 你可以保留ChannelHandlerContext，以备后续使用，正如触发一个handler方法外部的事件，甚至一个不同的线程。
 *
 * <pre>
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *
 *     <b>private {@link ChannelHandlerContext} ctx;</b>
 *
 *     public void beforeAdd({@link ChannelHandlerContext} ctx) {
 *         <b>this.ctx = ctx;</b>
 *     }
 *
 *     public void login(String username, password) {
 *         ctx.write(new LoginMessage(username, password));
 *     }
 *     ...
 * }
 * </pre>
 *
 * <h3>Storing stateful information</h3>
 *
 * 存储有状态信息
 *
 * {@link #attr(AttributeKey)} allow you to
 * store and access stateful information that is related with a handler and its
 * context.  Please refer to {@link ChannelHandler} to learn various recommended
 * ways to manage stateful information.
 *
 * AttributeKey允许你存储和访问状态信息，它关联了handler和他的context。请参考
 * ChannelHandler以学习各种推荐的方式去管理状态信息。
 *
 * <h3>A handler can have more than one context</h3>
 *
 * 一个handler可以有多个context
 *
 * Please note that a {@link ChannelHandler} instance can be added to more than
 * one {@link ChannelPipeline}.  It means a single {@link ChannelHandler}
 * instance can have more than one {@link ChannelHandlerContext} and therefore
 * the single instance can be invoked with different
 * {@link ChannelHandlerContext}s if it is added to one or more
 * {@link ChannelPipeline}s more than once.
 *
 * 请注意ChannelHandler实例可以被添加到超过一个ChannelPipeline中。这意味这单个ChannelHandler实例
 * 可以拥有超过一个的ChannelHandlerContext，并且因此一个单一的Handler实例可以被不同的
 * ChannelHandlerContexts调用，如果它添加到了一个或多次添加到多个ChannelPipeline中。
 *
 * <p>
 * For example, the following handler will have as many independent {@link AttributeKey}s
 * as how many times it is added to pipelines, regardless if it is added to the
 * same pipeline multiple times or added to different pipelines multiple times:
 *
 * 例如，以下的handler将有多个独立的AttributeKeys，正如他被多少次添加到pipelines中，不管它被多次
 * 添加到相同的pipeline中，或多次添加到不同的pipelines中。
 *
 * <pre>
 * public class FactorialHandler extends {@link ChannelInboundHandlerAdapter} {
 *
 *   private final {@link AttributeKey}&lt;{@link Integer}&gt; counter = {@link AttributeKey}.valueOf("counter");
 *
 *   // This handler will receive a sequence of increasing integers starting
 *   // from 1.
 *   {@code @Override}
 *   public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     Integer a = ctx.attr(counter).get();
 *
 *     if (a == null) {
 *       a = 1;
 *     }
 *
 *     attr.set(a * (Integer) msg);
 *   }
 * }
 *
 * // Different context objects are given to "f1", "f2", "f3", and "f4" even if
 * // they refer to the same handler instance.  Because the FactorialHandler
 * // stores its state in a context object (using an {@link AttributeKey}), the factorial is
 * // calculated correctly 4 times once the two pipelines (p1 and p2) are active.
 *
 * // 不同的context对象被赋予"f1", "f2", "f3", "f4"，尽管它们是相同的handler实例。因为
 * // FactorialHandler存储它的状态在一个context对象中（通过一个AttributeKey），factorial
 * // 将被正确地计算4次，一旦两个pipelines（p1和p2）是活着的。
 *
 * FactorialHandler fh = new FactorialHandler();
 *
 * {@link ChannelPipeline} p1 = {@link Channels}.pipeline();
 * p1.addLast("f1", fh);
 * p1.addLast("f2", fh);
 *
 * {@link ChannelPipeline} p2 = {@link Channels}.pipeline();
 * p2.addLast("f3", fh);
 * p2.addLast("f4", fh);
 * </pre>
 *
 * <h3>Additional resources worth reading</h3>
 *
 * 值得阅读的其它资源
 *
 * <p>
 * Please refer to the {@link ChannelHandler}, and
 * {@link ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 *
 * 请参考ChannelHandler和ChannelPipeline，查看更多关于inbound和outbound的操作，它们所拥有的
 * 不同的基础，以及它们在pipeline中的流转方式，和在你的应用程序中的操作处理方式。
 *
 */
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    /**
     * Return the {@link Channel} which is bound to the {@link ChannelHandlerContext}.
     *
     * 返回绑定到ChannelHandlerContext的Channel对象。
     */
    Channel channel();

    /**
     * Returns the {@link EventExecutor} which is used to execute an arbitrary task.
     *
     * 返回用于执行任意task的EventExecutor。
     */
    EventExecutor executor();

    /**
     * The unique name of the {@link ChannelHandlerContext}.The name was used when then {@link ChannelHandler}
     * was added to the {@link ChannelPipeline}. This name can also be used to access the registered
     * {@link ChannelHandler} from the {@link ChannelPipeline}.
     *
     * ChannelHandlerContext的唯一名字。此名字用于当ChannelHandler被添加到ChannelPipeline的时候。此名字
     * 同样可以被用于访问注册到ChannelPipeline的ChannelHandler对象。
     */
    String name();

    /**
     * The {@link ChannelHandler} that is bound this {@link ChannelHandlerContext}.
     *
     * 返回绑定到了当前ChannelHandlerContext的ChannelHandler对象。
     */
    ChannelHandler handler();

    /**
     * Return {@code true} if the {@link ChannelHandler} which belongs to this context was removed
     * from the {@link ChannelPipeline}. Note that this method is only meant to be called from with in the
     * {@link EventLoop}.
     *
     * 返回true，若属于当前context的ChannelHandler被从ChannelPipeline中删除了。
     * 注意此方法的原意仅用于EventLoop的调用。
     */
    boolean isRemoved();

    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();

    /**
     * Return the assigned {@link ChannelPipeline}
     *
     * 返回已分配的ChannelPipeline。
     */
    ChannelPipeline pipeline();

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     *
     * 返回已分配的ByteBufAllocator，此将用于分配ByteBuf。
     */
    ByteBufAllocator alloc();

    /**
     * @deprecated Use {@link Channel#attr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * @deprecated Use {@link Channel#hasAttr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);
}
