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
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;


/**
 * A list of {@link ChannelHandler}s which handles or intercepts inbound events and outbound operations of a
 * {@link Channel}.  {@link ChannelPipeline} implements an advanced form of the
 * <a href="http://www.oracle.com/technetwork/java/interceptingfilter-142169.html">Intercepting Filter</a> pattern
 * to give a user full control over how an event is handled and how the {@link ChannelHandler}s in a pipeline
 * interact with each other.
 *
 * ChannelPipeline是ChannelHandler的集合，用来处理或拦截Channel的输入事件和输出操作。ChannelPipeline实现了拦截过滤器模式
 * 的一种高级形式，将给用户全控制一个事件是怎样被处理的，以及ChannelHandler在pipeline里是如何相互交互的。
 *
 * <h3>Creation of a pipeline</h3>
 *
 * 创建pipeline
 *
 * Each channel has its own pipeline and it is created automatically when a new channel is created.
 *
 * 每一个channel都有它自己的pipeline，且它是自动被创建的，在channel被创建的时候。
 *
 * <h3>How an event flows in a pipeline</h3>
 *
 * 一个事件是怎样在pipeline里流转的
 *
 * The following diagram describes how I/O events are processed by {@link ChannelHandler}s in a {@link ChannelPipeline}
 * typically. An I/O event is handled by either a {@link ChannelInboundHandler} or a {@link ChannelOutboundHandler}
 * and be forwarded to its closest handler by calling the event propagation methods defined in
 * {@link ChannelHandlerContext}, such as {@link ChannelHandlerContext#fireChannelRead(Object)} and
 * {@link ChannelHandlerContext#write(Object)}.
 *
 * 下面的图表描述了IO事件在ChannelPipeline的ChannelHandler被处理的典型场景。一个IO事件要么被ChannelInboundHandler处理，
 * 要么被ChannelOutboundHandler处理，然后转发到距离它最近的handler，通过调用事件传播方法，定义在ChannelHandlerContext，比如
 * ChannelHandlerContext#fireChannelRead(Object)，和ChannelHandlerContext#write(Object)。
 *
 * <pre>
 *                                                 I/O Request
 *                                            via {@link Channel} or
 *                                        {@link ChannelHandlerContext}
 *                                                      |
 *  +---------------------------------------------------+---------------+
 *  |                           ChannelPipeline         |               |
 *  |                                                  \|/              |
 *  |    +---------------------+            +-----------+----------+    |
 *  |    | Inbound Handler  N  |            | Outbound Handler  1  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler N-1 |            | Outbound Handler  2  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  .               |
 *  |               .                                   .               |
 *  | ChannelHandlerContext.fireIN_EVT() ChannelHandlerContext.OUT_EVT()|
 *  |        [ method call]                       [method call]         |
 *  |               .                                   .               |
 *  |               .                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  2  |            | Outbound Handler M-1 |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  1  |            | Outbound Handler  M  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  +---------------+-----------------------------------+---------------+
 *                  |                                  \|/
 *  +---------------+-----------------------------------+---------------+
 *  |               |                                   |               |
 *  |       [ Socket.read() ]                    [ Socket.write() ]     |
 *  |                                                                   |
 *  |  Netty Internal I/O Threads (Transport Implementation)            |
 *  +-------------------------------------------------------------------+
 * </pre>
 * An inbound event is handled by the inbound handlers in the bottom-up direction as shown on the left side of the
 * diagram.  An inbound handler usually handles the inbound data generated by the I/O thread on the bottom of the
 * diagram.  The inbound data is often read from a remote peer via the actual input operation such as
 * {@link SocketChannel#read(ByteBuffer)}.  If an inbound event goes beyond the top inbound handler, it is discarded
 * silently, or logged if it needs your attention.
 *
 * 一个inbound事件会被inbound handlers从下往上处理，如图表的左侧。一个inbound handler通常处理inbound数据，通常由IO线程产生，
 * 在图表的底部。inbound数据通常从远端通过实际的输入方法，例如SocketChannel#read(ByteBuffer)来读取。如果一个inbound事件跑到
 * 了超过顶部的inbound handler，它将会波静默忽略，或者日志记录下来，如果你需要关注的话。
 *
 * <p>
 * An outbound event is handled by the outbound handler in the top-down direction as shown on the right side of the
 * diagram.  An outbound handler usually generates or transforms the outbound traffic such as write requests.
 * If an outbound event goes beyond the bottom outbound handler, it is handled by an I/O thread associated with the
 * {@link Channel}. The I/O thread often performs the actual output operation such as
 * {@link SocketChannel#write(ByteBuffer)}.
 *
 * 一个outbound事件会被outbound handler从上往下处理，如上去的右侧展示。一个outbound handler通常会产生会传输outbound流量，比如
 * 写操作。如果一个outbound事件跑超出了outbound handler的底部，它将被IO线程关联的Channel处理。IO线程一般会执行实际的写操作，如
 * SocketChannel#write(ByteBuffer)。
 *
 * <p>
 * For example, let us assume that we created the following pipeline:
 *
 * 例如，让我们假设我们已经创建了以下Pipeline：
 *
 * <pre>
 * {@link ChannelPipeline} p = ...;
 * p.addLast("1", new InboundHandlerA());
 * p.addLast("2", new InboundHandlerB());
 * p.addLast("3", new OutboundHandlerA());
 * p.addLast("4", new OutboundHandlerB());
 * p.addLast("5", new InboundOutboundHandlerX());
 * </pre>
 * In the example above, the class whose name starts with {@code Inbound} means it is an inbound handler.
 * The class whose name starts with {@code Outbound} means it is a outbound handler.
 *
 * 以上例子，类的名字由Inbound开头的代表是一个inbound handler。类名字以Outbound开头的代表着一个outbound handler。
 *
 * <p>
 * In the given example configuration, the handler evaluation order is 1, 2, 3, 4, 5 when an event goes inbound.
 * When an event goes outbound, the order is 5, 4, 3, 2, 1.  On top of this principle, {@link ChannelPipeline} skips
 * the evaluation of certain handlers to shorten the stack depth:
 *
 * NOTE 这里标重点！！！
 *
 * 在给定的配置例子里，当一个事件执行inbound流程，处理器执行的的顺序是1，2，3，4，5。当一个事件执行outbound流程，执行顺序是
 * 5，4，3，2，1。在这些原则之上，ChannelPipeline跳过了某些handlers的执行，以缩短堆栈的深度：
 *
 * <ul>
 * <li>3 and 4 don't implement {@link ChannelInboundHandler}, and therefore the actual evaluation order of an inbound
 *     event will be: 1, 2, and 5.</li>
 *
 *     3和4不会在ChannelInboundHandler中实现，因此一个inbound事件的真实执行顺序将会是：1，2，5。
 *
 * <li>1 and 2 don't implement {@link ChannelOutboundHandler}, and therefore the actual evaluation order of a
 *     outbound event will be: 5, 4, and 3.</li>
 *
 *     1和2不会在ChannelOutboundHandler中实现，因此一个outbound事件的实际执行顺序将会是：5，4，3。
 *
 * <li>If 5 implements both {@link ChannelInboundHandler} and {@link ChannelOutboundHandler}, the evaluation order of
 *     an inbound and a outbound event could be 125 and 543 respectively.</li>
 *
 *     如果5同时实现了ChannelInboundHandler和ChannelOutboundHandler，inbound事件和outbound事件的执行顺序将分别会是
 *     125和543。
 *
 * </ul>
 *
 * <h3>Forwarding an event to the next handler</h3>
 *
 * 转发一个事件到下一个handler
 *
 * As you might noticed in the diagram shows, a handler has to invoke the event propagation methods in
 * {@link ChannelHandlerContext} to forward an event to its next handler.  Those methods include:
 *
 * 正如你注意到的上图展示的，一个handler必须调用事件传播方法，在ChannelHandlerContext中定义，去转发一个事件到
 * 下一个handler。这些方法包括：
 *
 * <ul>
 * <li>Inbound event propagation methods:
 *     <ul>
 *     <li>{@link ChannelHandlerContext#fireChannelRegistered()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelActive()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelRead(Object)}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelReadComplete()}</li>
 *     <li>{@link ChannelHandlerContext#fireExceptionCaught(Throwable)}</li>
 *     <li>{@link ChannelHandlerContext#fireUserEventTriggered(Object)}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelWritabilityChanged()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelInactive()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelUnregistered()}</li>
 *     </ul>
 * </li>
 * <li>Outbound event propagation methods:
 *     <ul>
 *     <li>{@link ChannelHandlerContext#bind(SocketAddress, ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#connect(SocketAddress, SocketAddress, ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#write(Object, ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#flush()}</li>
 *     <li>{@link ChannelHandlerContext#read()}</li>
 *     <li>{@link ChannelHandlerContext#disconnect(ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#close(ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#deregister(ChannelPromise)}</li>
 *     </ul>
 * </li>
 * </ul>
 *
 * and the following example shows how the event propagation is usually done:
 *
 * 以下的例子展示了事件传播是如何完成的：
 *
 * <pre>
 * public class MyInboundHandler extends {@link ChannelInboundHandlerAdapter} {
 *     {@code @Override}
 *     public void channelActive({@link ChannelHandlerContext} ctx) {
 *         System.out.println("Connected!");
 *         ctx.fireChannelActive();
 *     }
 * }
 *
 * public class MyOutboundHandler extends {@link ChannelOutboundHandlerAdapter} {
 *     {@code @Override}
 *     public void close({@link ChannelHandlerContext} ctx, {@link ChannelPromise} promise) {
 *         System.out.println("Closing ..");
 *         ctx.close(promise);
 *     }
 * }
 * </pre>
 *
 * <h3>Building a pipeline</h3>
 *
 * 构建一个pipeline
 *
 * <p>
 * A user is supposed to have one or more {@link ChannelHandler}s in a pipeline to receive I/O events (e.g. read) and
 * to request I/O operations (e.g. write and close).  For example, a typical server will have the following handlers
 * in each channel's pipeline, but your mileage may vary depending on the complexity and characteristics of the
 * protocol and business logic:
 *
 * 用户应该拥有一个或多个ChannelHandlers，在一个pipeline里，以可以接收IO事件，如：read，和请求IO操作，如：write和close。
 * 例如，一个典型的服务将有以下的handlers在每一个channel的pipeline，而你的逻辑可能因协议和业务的复杂度和特点，采用了不同的处理。
 *
 * <ol>
 * <li>Protocol Decoder - translates binary data (e.g. {@link ByteBuf}) into a Java object.</li>
 *
 * 协议解码 - 把二进制数据，如：ByteBuf，解析成Java对象
 *
 * <li>Protocol Encoder - translates a Java object into binary data.</li>
 *
 * 协议编码 - 把Java对象解析成二进制数据。
 *
 * <li>Business Logic Handler - performs the actual business logic (e.g. database access).</li>
 *
 * 业务逻辑处理 - 执行实际的业务逻辑，如数据库访问。
 *
 * </ol>
 *
 * and it could be represented as shown in the following example:
 * 而且它将被以下展示的例子代表：
 *
 * <pre>
 * static final {@link EventExecutorGroup} group = new {@link DefaultEventExecutorGroup}(16);
 * ...
 *
 * {@link ChannelPipeline} pipeline = ch.pipeline();
 *
 * pipeline.addLast("decoder", new MyProtocolDecoder());
 * pipeline.addLast("encoder", new MyProtocolEncoder());
 *
 * // Tell the pipeline to run MyBusinessLogicHandler's event handler methods
 * // in a different thread than an I/O thread so that the I/O thread is not blocked by
 * // a time-consuming task.
 * // If your business logic is fully asynchronous or finished very quickly, you don't
 * // need to specify a group.
 * pipeline.addLast(group, "handler", new MyBusinessLogicHandler());
 * </pre>
 *
 * <h3>Thread safety</h3>
 *
 * 线程安全
 *
 * <p>
 * A {@link ChannelHandler} can be added or removed at any time because a {@link ChannelPipeline} is thread safe.
 * For example, you can insert an encryption handler when sensitive information is about to be exchanged, and remove it
 * after the exchange.
 *
 * 一个ChannelHandler可以被添加或删除，在任何时候，因为ChannelPipeline是线程安全的。
 * 例如，你可以插入一个加密handler，当敏感信息需要被交换的时候，可以删除它在交换完成之后。
 */
public interface ChannelPipeline
        extends ChannelInboundInvoker, ChannelOutboundInvoker, Iterable<Entry<String, ChannelHandler>> {

    /**
     * Inserts a {@link ChannelHandler} at the first position of this pipeline.
     *
     * 插入一个ChannelHandler到当前pipeline的首位。
     *
     * @param name     the name of the handler to insert first
     *                 handler的名字
     *
     * @param handler  the handler to insert first
     *                 插入的handler对象
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     *         如果已经有一个名字相同的handler在pipeline中定义了，抛出IllegalArgumentException。
     *
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     *         如果指定的handler为null，抛出NullPointerException。
     */
    ChannelPipeline addFirst(String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} at the first position of this pipeline.
     *
     * 插入一个ChannelHandler到当前pipeline的首位。
     *
     * @param group    the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     *                 插入的ChannelHandler将使用此EventExecutorGroup来执行
     *
     * @param name     the name of the handler to insert first
     *                 插入的handler的名称
     *
     * @param handler  the handler to insert first
     *                 插入的handler对象
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     *         如果已存在一个相同名字的项目在pipeline中，则抛出IllegalArgumentException。
     *
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     *         如果指定的handler的值为null，抛出NullPointerException。
     */
    ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler);

    /**
     * Appends a {@link ChannelHandler} at the last position of this pipeline.
     *
     * 添加一个ChannelHandler到pipeline的末尾。
     *
     * @param name     the name of the handler to append
     *                 添加的handler的名字
     *
     * @param handler  the handler to append
     *                 添加的handler
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    ChannelPipeline addLast(String name, ChannelHandler handler);

    /**
     * Appends a {@link ChannelHandler} at the last position of this pipeline.
     *
     * @param group    the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} before an existing handler of this
     * pipeline.
     *
     * 插入一个ChannelHandler，在当前的pipeline的一个已存在的handler之前。
     *
     * @param baseName  the name of the existing handler
     *                  当前已存在的handler的名字
     *
     * @param name      the name of the handler to insert before
     *                  插入的handler的名字
     *
     * @param handler   the handler to insert before
     *                  插入的handler对象
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     *         若baseName代表的handler不存在，则抛出NoSuchElementException
     *
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     *         pipeline里的handers名字冲突，抛出IllegalArgumentException
     *
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     *         baseName或handler为null
     */
    ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} before an existing handler of this
     * pipeline.
     *
     * @param group     the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                  methods
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert before
     * @param handler   the handler to insert before
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} after an existing handler of this
     * pipeline.
     *
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert after
     * @param handler   the handler to insert after
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} after an existing handler of this
     * pipeline.
     *
     * @param group     the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                  methods
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert after
     * @param handler   the handler to insert after
     *
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);

    /**
     * Inserts {@link ChannelHandler}s at the first position of this pipeline.
     *
     * @param handlers  the handlers to insert first
     *
     */
    ChannelPipeline addFirst(ChannelHandler... handlers);

    /**
     * Inserts {@link ChannelHandler}s at the first position of this pipeline.
     *
     * @param group     the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}s
     *                  methods.
     * @param handlers  the handlers to insert first
     *
     */
    ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers);

    /**
     * Inserts {@link ChannelHandler}s at the last position of this pipeline.
     *
     * @param handlers  the handlers to insert last
     *
     */
    ChannelPipeline addLast(ChannelHandler... handlers);

    /**
     * Inserts {@link ChannelHandler}s at the last position of this pipeline.
     *
     * @param group     the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}s
     *                  methods.
     * @param handlers  the handlers to insert last
     *
     */
    ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers);

    /**
     * Removes the specified {@link ChannelHandler} from this pipeline.
     *
     * 从pipeline删除ChannelHandler，根据对象的引用删除。
     *
     * @param  handler          the {@link ChannelHandler} to remove
     *
     * @throws NoSuchElementException
     *         if there's no such handler in this pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    ChannelPipeline remove(ChannelHandler handler);

    /**
     * Removes the {@link ChannelHandler} with the specified name from this pipeline.
     *
     * 从pipeline删除ChannelHandler，根据handler的名字删除。
     *
     * @param  name             the name under which the {@link ChannelHandler} was stored.
     *                          handler在pipeline中存储的名字
     *
     * @return the removed handler
     *         返回删除的handler对象引用
     *
     * @throws NoSuchElementException
     *         if there's no such handler with the specified name in this pipeline
     *         当前名字的handler不存在，抛出NoSuchElementException
     *
     * @throws NullPointerException
     *         if the specified name is {@code null}
     *         name为null，则抛出NullPointerException
     */
    ChannelHandler remove(String name);

    /**
     * Removes the {@link ChannelHandler} of the specified type from this pipeline.
     *
     * 从pipeline删除ChannelHandler
     *
     * @param <T>           the type of the handler
     *                      handler的类型
     *
     * @param handlerType   the type of the handler
     *                      handler的类型
     *
     * @return the removed handler
     *         返回已被删除的handler
     *
     * @throws NoSuchElementException
     *         if there's no such handler of the specified type in this pipeline
     *         如果指定类型的handler不存在于pipeline，抛出NoSuchElementException
     *
     * @throws NullPointerException
     *         if the specified handler type is {@code null}
     *         如果handler的类型为null，抛出NullPointerException
     */
    <T extends ChannelHandler> T remove(Class<T> handlerType);

    /**
     * Removes the first {@link ChannelHandler} in this pipeline.
     *
     * 删除pipeline的首个ChannelHandler
     *
     * @return the removed handler
     *         返回已被删除的handler
     *
     * @throws NoSuchElementException
     *         if this pipeline is empty
     *         若pipeline为空，抛出NoSuchElementException
     */
    ChannelHandler removeFirst();

    /**
     * Removes the last {@link ChannelHandler} in this pipeline.
     *
     * 删除pipeline的最后一个ChannelHandler
     *
     * @return the removed handler
     *         返回删除的handler
     *
     * @throws NoSuchElementException
     *         if this pipeline is empty
     *         若当前pipeline为空，抛出NoSuchElementException
     */
    ChannelHandler removeLast();

    /**
     * Replaces the specified {@link ChannelHandler} with a new handler in this pipeline.
     *
     * 在pipeline替换旧的handler为新的handler
     *
     * @param  oldHandler    the {@link ChannelHandler} to be replaced
     *                       需要被替换的ChannelHandler的对象引用
     *
     * @param  newName       the name under which the replacement should be added
     *                       新的handler名字
     *
     * @param  newHandler    the {@link ChannelHandler} which is used as replacement
     *                       新的handler对象
     *
     * @return itself
     *         返回当前pipeline对象

     * @throws NoSuchElementException
     *         if the specified old handler does not exist in this pipeline
     *         旧的handler对象不在pipeline中，抛出NoSuchElementException
     *
     * @throws IllegalArgumentException
     *         if a handler with the specified new name already exists in this
     *         pipeline, except for the handler to be replaced
     *         新的handler的名字已存在pipeline中，抛出IllegalArgumentException
     *
     * @throws NullPointerException
     *         if the specified old handler or new handler is
     *         {@code null}
     *         旧handler或新handler为null，抛出NullPointerException
     */
    ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler);

    /**
     * Replaces the {@link ChannelHandler} of the specified name with a new handler in this pipeline.
     *
     * @param  oldName       the name of the {@link ChannelHandler} to be replaced
     * @param  newName       the name under which the replacement should be added
     * @param  newHandler    the {@link ChannelHandler} which is used as replacement
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if the handler with the specified old name does not exist in this pipeline
     * @throws IllegalArgumentException
     *         if a handler with the specified new name already exists in this
     *         pipeline, except for the handler to be replaced
     * @throws NullPointerException
     *         if the specified old handler or new handler is
     *         {@code null}
     */
    ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler);

    /**
     * Replaces the {@link ChannelHandler} of the specified type with a new handler in this pipeline.
     *
     * @param  oldHandlerType   the type of the handler to be removed
     * @param  newName          the name under which the replacement should be added
     * @param  newHandler       the {@link ChannelHandler} which is used as replacement
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if the handler of the specified old handler type does not exist
     *         in this pipeline
     * @throws IllegalArgumentException
     *         if a handler with the specified new name already exists in this
     *         pipeline, except for the handler to be replaced
     * @throws NullPointerException
     *         if the specified old handler or new handler is
     *         {@code null}
     */
    <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName,
                                         ChannelHandler newHandler);

    /**
     * Returns the first {@link ChannelHandler} in this pipeline.
     *
     * @return the first handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandler first();

    /**
     * Returns the context of the first {@link ChannelHandler} in this pipeline.
     *
     * @return the context of the first handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandlerContext firstContext();

    /**
     * Returns the last {@link ChannelHandler} in this pipeline.
     *
     * @return the last handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandler last();

    /**
     * Returns the context of the last {@link ChannelHandler} in this pipeline.
     *
     * @return the context of the last handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandlerContext lastContext();

    /**
     * Returns the {@link ChannelHandler} with the specified name in this
     * pipeline.
     *
     * @return the handler with the specified name.
     *         {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandler get(String name);

    /**
     * Returns the {@link ChannelHandler} of the specified type in this
     * pipeline.
     *
     * @return the handler of the specified handler type.
     *         {@code null} if there's no such handler in this pipeline.
     */
    <T extends ChannelHandler> T get(Class<T> handlerType);

    /**
     * Returns the context object of the specified {@link ChannelHandler} in
     * this pipeline.
     *
     * @return the context object of the specified handler.
     *         {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandlerContext context(ChannelHandler handler);

    /**
     * Returns the context object of the {@link ChannelHandler} with the
     * specified name in this pipeline.
     *
     * @return the context object of the handler with the specified name.
     *         {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandlerContext context(String name);

    /**
     * Returns the context object of the {@link ChannelHandler} of the
     * specified type in this pipeline.
     *
     * @return the context object of the handler of the specified type.
     *         {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType);

    /**
     * Returns the {@link Channel} that this pipeline is attached to.
     *
     * 返回当前pipeline绑定的Channel
     *
     * @return the channel. {@code null} if this pipeline is not attached yet.
     *         返回绑定的channel。若当前pipeline未绑定，则返回null。
     */
    Channel channel();

    /**
     * Returns the {@link List} of the handler names.
     *
     * 返回所有handlers的名字集合
     */
    List<String> names();

    /**
     * Converts this pipeline into an ordered {@link Map} whose keys are
     * handler names and whose values are handlers.
     *
     * 转换当前pipeline的所有handlers为map。
     * key：handler名字
     * value：handler对象
     */
    Map<String, ChannelHandler> toMap();

    @Override
    ChannelPipeline fireChannelRegistered();

    @Override
    ChannelPipeline fireChannelUnregistered();

    @Override
    ChannelPipeline fireChannelActive();

    @Override
    ChannelPipeline fireChannelInactive();

    @Override
    ChannelPipeline fireExceptionCaught(Throwable cause);

    @Override
    ChannelPipeline fireUserEventTriggered(Object event);

    @Override
    ChannelPipeline fireChannelRead(Object msg);

    @Override
    ChannelPipeline fireChannelReadComplete();

    @Override
    ChannelPipeline fireChannelWritabilityChanged();

    @Override
    ChannelPipeline flush();
}
