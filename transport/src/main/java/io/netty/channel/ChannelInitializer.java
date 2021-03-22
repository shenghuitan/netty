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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A special {@link ChannelInboundHandler} which offers an easy way to initialize a {@link Channel} once it was
 * registered to its {@link EventLoop}.
 *
 * 一个特殊的ChannelInboundHandler，提供了简单的方式去初始化Channel，当它被注册到EventLoop。
 *
 * Implementations are most often used in the context of {@link Bootstrap#handler(ChannelHandler)} ,
 * {@link ServerBootstrap#handler(ChannelHandler)} and {@link ServerBootstrap#childHandler(ChannelHandler)} to
 * setup the {@link ChannelPipeline} of a {@link Channel}.
 *
 * 此实现大多数用于Bootstrap#handler(ChannelHandler)、ServerBootstrap#handler(ChannelHandler)、
 * ServerBootstrap#childHandler(ChannelHandler)的context被Channel的ChannelPipeline建立的时候。
 *
 * <pre>
 *
 * public class MyChannelInitializer extends {@link ChannelInitializer} {
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("myHandler", new MyHandler());
 *     }
 * }
 *
 * {@link ServerBootstrap} bootstrap = ...;
 * ...
 * bootstrap.childHandler(new MyChannelInitializer());
 * ...
 * </pre>
 * Be aware that this class is marked as {@link Sharable} and so the implementation must be safe to be re-used.
 *
 * 注意这个方法是标记为Sharable的，所以它的实现可以安全地被重用。
 *
 * ChannelOutboundHandler更多是与写操作关联，服务的初始化是自身的信息的建立，用ChannelInboundHandler会更合适。
 *
 * 注解：
 * 1、ChannelInitializer是一个ChannelHandler，在使用上与其它的ChannelHandler没有区别。
 * 2、提供ChannelInitializer，是为了判断，当进程逻辑相对复杂时，哪些Handler应该添加到Pipeline，哪些不需要，封装了判断逻辑。
 * 3、ChannelInitializer是一个临时的逻辑，一旦初始化完成，即废弃。
 *
 * @param <C>   A sub-type of {@link Channel}
 */
@Sharable
public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelInitializer.class);

    // We use a Set as a ChannelInitializer is usually shared between all Channels in a Bootstrap /
    // ServerBootstrap. This way we can reduce the memory usage compared to use Attributes.
    // 我们使用一个Set作为一个ChannelInitializer，这通常在Bootstrap/ServerBootstrap里的所有Channel中共享。
    // 这种方式相比使用Attributes，我们可以减少内存的使用。
    private final Set<ChannelHandlerContext> initMap = Collections.newSetFromMap(
            new ConcurrentHashMap<ChannelHandlerContext, Boolean>());

    /**
     * This method will be called once the {@link Channel} was registered. After the method returns this instance
     * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
     *
     * 此方法将被调用，一旦Channel被注册。方法返回当前实例后，将被删除，从Channel的ChannelPipeline中。
     *
     * @param ch            the {@link Channel} which was registered.
     *                      被注册的Channel
     *
     * @throws Exception    is thrown if an error occurs. In that case it will be handled by
     *                      {@link #exceptionCaught(ChannelHandlerContext, Throwable)} which will by default close
     *                      the {@link Channel}.
     *                      如果任何错误发生，抛出Exception。
     *                      在这种情况下，它将通过#exceptionCaught(ChannelHandlerContext, Throwable)被处理，默认情况下
     *                      将关闭Channel。
     */
    protected abstract void initChannel(C ch) throws Exception;

    /**
     * 这里将调用initChannel(C ch)，并在完成后将其从Pipeline删除。
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Normally this method will never be called as handlerAdded(...) should call initChannel(...) and remove
        // the handler.
        if (initChannel(ctx)) {
            // we called initChannel(...) so we need to call now pipeline.fireChannelRegistered() to ensure we not
            // miss an event.
            ctx.pipeline().fireChannelRegistered();

            // We are done with init the Channel, removing all the state for the Channel now.
            removeState(ctx);
        } else {
            // Called initChannel(...) before which is the expected behavior, so just forward the event.
            ctx.fireChannelRegistered();
        }
    }

    /**
     * Handle the {@link Throwable} by logging and closing the {@link Channel}. Sub-classes may override this.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (logger.isWarnEnabled()) {
            logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), cause);
        }
        ctx.close();
    }

    /**
     * {@inheritDoc} If override this method ensure you call super!
     *
     * NOTE 在channel注册到EventLoop之后，初始化所有定义的ChannelHandler。
     *
     * initializer在初始化完成后，会被删除。
     * 不会跟普通的ChannelHandler一样被保存下来，对正常的handlers流程有任何的影响。
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isRegistered()) {
            // This should always be true with our current DefaultChannelPipeline implementation.
            // The good thing about calling initChannel(...) in handlerAdded(...) is that there will be no ordering
            // surprises if a ChannelInitializer will add another ChannelInitializer. This is as all handlers
            // will be added in the expected order.
            /*
            这应该永远是true的，与我们当前DefaultChannelPipeline的实现。
            好处是调用initChannel(...) in handlerAdded(...) 将不会有无序的奇怪结果，如果ChannelInitializer将添加另一个
            ChannelInitializer。
            这是为所有handlers都被添加到期望的顺序。
             */

            // 依次把handlers绑定到ChannelHandlerContext
            if (initChannel(ctx)) {

                // We are done with init the Channel, removing the initializer now.
                // 我们完成初始化Channel，现在删除initializer。
                removeState(ctx);
            }
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        initMap.remove(ctx);
    }

    @SuppressWarnings("unchecked")
    private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
        if (initMap.add(ctx)) { // Guard against re-entrance.   // 防止重入，仅处理一次
            try {
                initChannel((C) ctx.channel());
            } catch (Throwable cause) {
                // Explicitly call exceptionCaught(...) as we removed the handler before calling initChannel(...).
                // We do so to prevent multiple calls to initChannel(...).
                exceptionCaught(ctx, cause);
            } finally {
                // 从pipeline中移除当前ChannelInitializer
                ChannelPipeline pipeline = ctx.pipeline();
                if (pipeline.context(this) != null) {
                    pipeline.remove(this);
                }
            }
            return true;
        }
        return false;
    }

    private void removeState(final ChannelHandlerContext ctx) {
        // The removal may happen in an async fashion if the EventExecutor we use does something funky.
        if (ctx.isRemoved()) {
            initMap.remove(ctx);
        } else {
            // The context is not removed yet which is most likely the case because a custom EventExecutor is used.
            // Let's schedule it on the EventExecutor to give it some more time to be completed in case it is offloaded.
            ctx.executor().execute(new Runnable() {
                @Override
                public void run() {
                    initMap.remove(ctx);
                }
            });
        }
    }
}
