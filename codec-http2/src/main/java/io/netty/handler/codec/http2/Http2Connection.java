/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.UnstableApi;

/**
 * Manager for the state of an HTTP/2 connection with the remote end-point.
 *
 * 管理HTTP/2连接状态，通过远程终端。
 */
@UnstableApi
public interface Http2Connection {
    /**
     * Listener for life-cycle events for streams in this connection.
     *
     * 监听connection的streams的生命周期事件。
     */
    interface Listener {
        /**
         * Notifies the listener that the given stream was added to the connection. This stream may
         * not yet be active (i.e. {@code OPEN} or {@code HALF CLOSED}).
         *
         * 通知linstener，指定的stream已经添加到了connection。stream可能不在active（如：OPEN或HALF CLOSED）。
         *
         * <p>
         * If a {@link RuntimeException} is thrown it will be logged and <strong>not propagated</strong>.
         * Throwing from this method is not supported and is considered a programming error.
         *
         * 如果一个RuntimeException被抛出，它将被日志记录，且不会传播。从这个方法抛出异常不被支持，且被认为是
         * 程序错误。
         */
        void onStreamAdded(Http2Stream stream);

        /**
         * Notifies the listener that the given stream was made active (i.e. {@code OPEN} or {@code HALF CLOSED}).
         *
         * 通知listener，指定的stream已经被active（即：OPEN或HALF CLOSED）。
         *
         * <p>
         * If a {@link RuntimeException} is thrown it will be logged and <strong>not propagated</strong>.
         * Throwing from this method is not supported and is considered a programming error.
         */
        void onStreamActive(Http2Stream stream);

        /**
         * Notifies the listener that the given stream has transitioned from {@code OPEN} to {@code HALF CLOSED}.
         * This method will <strong>not</strong> be called until a state transition occurs from when
         * {@link #onStreamActive(Http2Stream)} was called.
         * The stream can be inspected to determine which side is {@code HALF CLOSED}.
         *
         * 通知listener，指定的stream已经从OPEN过渡到HALF CLOSED状态。此方法不会被调用，直到过渡状态从
         * #onStreamActive(Http2Stream)方法调用中发生。stream可被检查，以决定拿端是HALF CLOSED。
         *
         * <p>
         * If a {@link RuntimeException} is thrown it will be logged and <strong>not propagated</strong>.
         * Throwing from this method is not supported and is considered a programming error.
         */
        void onStreamHalfClosed(Http2Stream stream);

        /**
         * Notifies the listener that the given stream is now {@code CLOSED} in both directions and will no longer
         * be accessible via {@link #forEachActiveStream(Http2StreamVisitor)}.
         *
         * 通知listener，指定的stream现在CLOSED，在双端。且不可再通过#forEachActiveStream(Http2StreamVisitor)
         * 访问到。
         *
         * <p>
         * If a {@link RuntimeException} is thrown it will be logged and <strong>not propagated</strong>.
         * Throwing from this method is not supported and is considered a programming error.
         */
        void onStreamClosed(Http2Stream stream);

        /**
         * Notifies the listener that the given stream has now been removed from the connection and
         * will no longer be returned via {@link Http2Connection#stream(int)}. The connection may
         * maintain inactive streams for some time before removing them.
         *
         * 通知listener，指定的stream已经从connection被删除，且将不可再通过Http2Connection#stream(int)
         * 方法返回。连接不维护inactive streams，因之前已经删除。
         *
         * <p>
         * If a {@link RuntimeException} is thrown it will be logged and <strong>not propagated</strong>.
         * Throwing from this method is not supported and is considered a programming error.
         */
        void onStreamRemoved(Http2Stream stream);

        /**
         * Called when a {@code GOAWAY} frame was sent for the connection.
         *
         * 当连接的一个GOAWAY帧被发送的时候调用。
         *
         * <p>
         * If a {@link RuntimeException} is thrown it will be logged and <strong>not propagated</strong>.
         * Throwing from this method is not supported and is considered a programming error.
         *
         * @param lastStreamId the last known stream of the remote endpoint.
         *                     远程终端的最后一个已知的stream。
         *
         * @param errorCode    the error code, if abnormal closure.
         *                     错误吗，如果不正常关闭。
         *
         * @param debugData    application-defined debug data.
         *                     应用定义的debug数据。
         */
        void onGoAwaySent(int lastStreamId, long errorCode, ByteBuf debugData);

        /**
         * Called when a {@code GOAWAY} was received from the remote endpoint. This event handler duplicates {@link
         * Http2FrameListener#onGoAwayRead(io.netty.channel.ChannelHandlerContext, int, long, io.netty.buffer.ByteBuf)}
         * but is added here in order to simplify application logic for handling {@code GOAWAY} in a uniform way. An
         * application should generally not handle both events, but if it does this method is called second, after
         * notifying the {@link Http2FrameListener}.
         *
         * 当收到远程终端的GOAWAY帧的时候调用。这个时间处理器重复了Http2FrameListener#onGoAwayRead方法，但添加到这里是为了
         * 简化处理GOAWAY的应用逻辑，在一个统一的方式。一个应用通常应该不处理事件，但如果此方法第二次被调用，在通知
         * Http2FrameListener帧之后。
         *
         * <p>
         * If a {@link RuntimeException} is thrown it will be logged and <strong>not propagated</strong>.
         * Throwing from this method is not supported and is considered a programming error.
         * @param lastStreamId the last known stream of the remote endpoint.
         * @param errorCode    the error code, if abnormal closure.
         * @param debugData    application-defined debug data.
         */
        void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData);
    }

    /**
     * A view of the connection from one endpoint (local or remote).
     *
     * 一端的连接视图（本地或远程）
     */
    interface Endpoint<F extends Http2FlowController> {
        /**
         * Increment and get the next generated stream id this endpoint. If negative, the stream IDs are
         * exhausted for this endpoint an no further streams may be created.
         *
         * 增加并且获得下一个生成当前终端的stream id。如果是负数，stream IDs将枯竭，当前终端将不再有streams
         * 可以被创建。
         */
        int incrementAndGetNextStreamId();

        /**
         * Indicates whether the given streamId is from the set of IDs used by this endpoint to
         * create new streams.
         *
         * 表明给定的streamId是否集合的IDs用于当前终端去创建一个新的streams。
         */
        boolean isValidStreamId(int streamId);

        /**
         * Indicates whether or not this endpoint may have created the given stream. This is {@code true} if
         * {@link #isValidStreamId(int)} and {@code streamId} <= {@link #lastStreamCreated()}.
         *
         * 表明当前endpoint是否拥有给定的stream。这将返回true，若#isValidStreamId(int)和
         * streamId <= #lastStreamCreated()。
         */
        boolean mayHaveCreatedStream(int streamId);

        /**
         * Indicates whether or not this endpoint created the given stream.
         *
         * 表明是否当前endpoint创建了给定的stream。
         */
        boolean created(Http2Stream stream);

        /**
         * Indicates whether or a stream created by this endpoint can be opened without violating
         * {@link #maxActiveStreams()}.
         *
         * 表明是否stream通过当前endpoint创建，可以被打开，不违反#maxActiveStreams()。
         */
        boolean canOpenStream();

        /**
         * Creates a stream initiated by this endpoint. This could fail for the following reasons:
         *
         * 创建stream，通过此endpoint初始化。这可能由以下的原因导致失败：
         *
         * <ul>
         * <li>The requested stream ID is not the next sequential ID for this endpoint.</li>
         * 请求的stream ID不是当前endpoint的下一个序列ID。
         *
         * <li>The stream already exists.</li>
         * stream已存在。
         *
         * <li>{@link #canOpenStream()} is {@code false}.</li>
         * #canOpenStream()方法返回false。
         *
         * <li>The connection is marked as going away.</li>
         * 连接已经被标记为：going away。
         *
         * </ul>
         * <p>
         * The initial state of the stream will be immediately set before notifying {@link Listener}s. The state
         * transition is sensitive to {@code halfClosed} and is defined by {@link Http2Stream#open(boolean)}.
         *
         * stream的初始化状态状态将被马上设置，在通知Listeners之前。过渡对于halfClosed敏感，它由Http2Stream#open(boolean)方法定义。
         *
         * @param streamId The ID of the stream
         * @param halfClosed see {@link Http2Stream#open(boolean)}.
         * @see Http2Stream#open(boolean)
         */
        Http2Stream createStream(int streamId, boolean halfClosed) throws Http2Exception;

        /**
         * Creates a push stream in the reserved state for this endpoint and notifies all listeners.
         * This could fail for the following reasons:
         *
         * 创建一个push stream在保留的状态里，为当前endpoint，以及通知所有的listeners。这可能由以下原因引起失败：
         *
         * <ul>
         * <li>Server push is not allowed to the opposite endpoint.</li>
         * Server push不被对面endpoint所允许。
         *
         * <li>The requested stream ID is not the next sequential stream ID for this endpoint.</li>
         * 请求的stream ID不是下一个序列stream ID，对于当前endpoint。
         *
         * <li>The number of concurrent streams is above the allowed threshold for this endpoint.</li>
         * 并发stream数量已经超过了当前endpoint允许的上限值。
         *
         * <li>The connection is marked as going away.</li>
         * 连接已经被标记为going away。
         *
         * <li>The parent stream ID does not exist or is not {@code OPEN} from the side sending the push
         * promise.</li>
         * 父stream ID不存在，或没有OPEN，从push promise的发送端。
         *
         * <li>Could not set a valid priority for the new stream.</li>
         * 无法设置一个有效的优先级给新的stream。
         *
         * </ul>
         *
         * @param streamId the ID of the push stream
         * @param parent the parent stream used to initiate the push stream.
         */
        Http2Stream reservePushStream(int streamId, Http2Stream parent) throws Http2Exception;

        /**
         * Indicates whether or not this endpoint is the server-side of the connection.
         *
         * 表明endpoint是否连接的server端。
         */
        boolean isServer();

        /**
         * This is the <a href="https://tools.ietf.org/html/rfc7540#section-6.5.2">SETTINGS_ENABLE_PUSH</a> value sent
         * from the opposite endpoint. This method should only be called by Netty (not users) as a result of a
         * receiving a {@code SETTINGS} frame.
         *
         * 这是一个SETTINGS_ENABLE_PUSH值，从对面的endpoint发送过来。此方法应仅被Netty调用，作为收到的SETTINGS帧的结果。
         */
        void allowPushTo(boolean allow);

        /**
         * This is the <a href="https://tools.ietf.org/html/rfc7540#section-6.5.2">SETTINGS_ENABLE_PUSH</a> value sent
         * from the opposite endpoint. The initial value must be {@code true} for the client endpoint and always false
         * for a server endpoint.
         *
         * 这是一个SETTINGS_ENABLE_PUSH值，由对端endpoint发送过来。
         * 初始化对client来说，必须是true。对server来说，必须是false。
         */
        boolean allowPushTo();

        /**
         * Gets the number of active streams (i.e. {@code OPEN} or {@code HALF CLOSED}) that were created by this
         * endpoint.
         *
         * 获取由当前endpoint创建的active streams，如：OPEN或HALF CLOSED。
         */
        int numActiveStreams();

        /**
         * Gets the maximum number of streams (created by this endpoint) that are allowed to be active at
         * the same time. This is the
         * <a href="https://tools.ietf.org/html/rfc7540#section-6.5.2">SETTINGS_MAX_CONCURRENT_STREAMS</a>
         * value sent from the opposite endpoint to restrict stream creation by this endpoint.
         * <p>
         * The default value returned by this method must be "unlimited".
         *
         * 获取最大的streams数量（由当前endpoint创建），允许同时active的数量。
         * 这是SETTINGS_MAX_CONCURRENT_STREAMS的值，由对端endpoint发送而来，严格有当前的endpoint创建stream。
         *
         * 此方法的默认值必须是"unlimited"。
         */
        int maxActiveStreams();

        /**
         * Sets the limit for {@code SETTINGS_MAX_CONCURRENT_STREAMS}.
         *
         * 设置SETTINGS_MAX_CONCURRENT_STREAMS的最大值。
         *
         * @param maxActiveStreams The maximum number of streams (created by this endpoint) that are allowed to be
         * active at once. This is the
         * <a href="https://tools.ietf.org/html/rfc7540#section-6.5.2">SETTINGS_MAX_CONCURRENT_STREAMS</a> value sent
         * from the opposite endpoint to restrict stream creation by this endpoint.
         */
        void maxActiveStreams(int maxActiveStreams);

        /**
         * Gets the ID of the stream last successfully created by this endpoint.
         *
         * 获取stream的ID，有endpoint上次成功创建。
         */
        int lastStreamCreated();

        /**
         * If a GOAWAY was received for this endpoint, this will be the last stream ID from the
         * GOAWAY frame. Otherwise, this will be {@code -1}.
         *
         * 如果GOAWAY由当前endpoint被接收，这将是最后一个stream ID，从GOAWAY帧起。否则，值将是-1。
         */
        int lastStreamKnownByPeer();

        /**
         * Gets the flow controller for this endpoint.
         *
         * 获取当前endpoint的流控制器。
         */
        F flowController();

        /**
         * Sets the flow controller for this endpoint.
         *
         * 设置当前endpoint的流控制器。
         */
        void flowController(F flowController);

        /**
         * Gets the {@link Endpoint} opposite this one.
         *
         * 获取对端endpoint。
         */
        Endpoint<? extends Http2FlowController> opposite();
    }

    /**
     * A key to be used for associating application-defined properties with streams within this connection.
     *
     * key，用于关联应用定义的属性，通过streams，在这个connection里。
     */
    interface PropertyKey {
    }

    /**
     * Close this connection. No more new streams can be created after this point and
     * all streams that exists (active or otherwise) will be closed and removed.
     * <p>Note if iterating active streams via {@link #forEachActiveStream(Http2StreamVisitor)} and an exception is
     * thrown it is necessary to call this method again to ensure the close completes.
     *
     * 关闭连接。没有更多的新streams可以被创建，在这个点之后，并且所有streams，存在（avtive或反之）将被关闭和删除。
     * 注意如果迭代active streams，通过#forEachActiveStream(Http2StreamVisitor)方法，一个异常将被抛出，它是
     * 必须再次调用这个方法去保证完全关闭。
     *
     * @param promise Will be completed when all streams have been removed, and listeners have been notified.
     *                将被完成，当所有streams被删除，listeners将被通知。
     *
     * @return A future that will be completed when all streams have been removed, and listeners have been notified.
     *              一个future将被完成，当所有streams被删除，listeners被通知。
     */
    Future<Void> close(Promise<Void> promise);

    /**
     * Creates a new key that is unique within this {@link Http2Connection}.
     *
     * 创建一个新的key，在这个HTTP2Connection里唯一。
     */
    PropertyKey newKey();

    /**
     * Adds a listener of stream life-cycle events.
     *
     * 添加一个listener，监听stream的生命周期事件。
     */
    void addListener(Listener listener);

    /**
     * Removes a listener of stream life-cycle events. If the same listener was added multiple times
     * then only the first occurrence gets removed.
     *
     * 删除一个listener，stream生命周期的事件。如果相同的listener被添加多次，仅第一次出现的被删除。
     */
    void removeListener(Listener listener);

    /**
     * Gets the stream if it exists. If not, returns {@code null}.
     *
     * Get stream，如果stream存在。否则，返回null。
     */
    Http2Stream stream(int streamId);

    /**
     * Indicates whether or not the given stream may have existed within this connection. This is a short form
     * for calling {@link Endpoint#mayHaveCreatedStream(int)} on both endpoints.
     *
     * 表明指定的stream是否存在于这个connection。这是一个简短形式的调用Endpoint#mayHaveCreatedStream(int)，
     * 在双端。
     */
    boolean streamMayHaveExisted(int streamId);

    /**
     * Gets the stream object representing the connection, itself (i.e. stream zero). This object
     * always exists.
     *
     * 获取stream对象，代表这个连接本身。也就是stream 0。这个对象总是存在。
     */
    Http2Stream connectionStream();

    /**
     * Gets the number of streams that are actively in use (i.e. {@code OPEN} or {@code HALF CLOSED}).
     *
     * 获取streams的数量，活着被使用中的，即OPEN或HALF CLOSE。
     */
    int numActiveStreams();

    /**
     * Provide a means of iterating over the collection of active streams.
     *
     * 提供一个方式去迭代所有的活着的streams，在整个collection中。
     *
     * @param visitor The visitor which will visit each active stream.
     *                参观者，遍历每一个active stream。
     *
     * @return The stream before iteration stopped or {@code null} if iteration went past the end.
     *              返回stream，在迭代停止之前，或null，如果迭代过了终点。
     */
    Http2Stream forEachActiveStream(Http2StreamVisitor visitor) throws Http2Exception;

    /**
     * Indicates whether or not the local endpoint for this connection is the server.
     *
     * 表明connection的本地终端是否Server。
     */
    boolean isServer();

    /**
     * Gets a view of this connection from the local {@link Endpoint}.
     *
     * 获取connection的视图，从本地终端。
     */
    Endpoint<Http2LocalFlowController> local();

    /**
     * Gets a view of this connection from the remote {@link Endpoint}.
     *
     * 获取connection的视图，从远程终端。
     */
    Endpoint<Http2RemoteFlowController> remote();

    /**
     * Indicates whether or not a {@code GOAWAY} was received from the remote endpoint.
     *
     * 表明是否从远程终端收到了GOAWAY。
     */
    boolean goAwayReceived();

    /**
     * Indicates that a {@code GOAWAY} was received from the remote endpoint and sets the last known stream.
     *
     * 表明从远程终端收到了一个GOAWAY，设置最后一个知道的stream。
     *
     * @param lastKnownStream The Last-Stream-ID in the
     * <a href="https://tools.ietf.org/html/rfc7540#section-6.8">GOAWAY</a> frame.
     *                        最后的stream id，在GOAWAY帧。
     *
     * @param errorCode the Error Code in the
     * <a href="https://tools.ietf.org/html/rfc7540#section-6.8">GOAWAY</a> frame.
     *                  错误吗，在GOAWAY帧。
     *
     * @param message The Additional Debug Data in the
     * <a href="https://tools.ietf.org/html/rfc7540#section-6.8">GOAWAY</a> frame. Note that reference count ownership
     * belongs to the caller (ownership is not transferred to this method).
     *                附加的Debug数据，在GOAWAY帧。注意引用数量所有权属于调用者（所有权并没有传输到此方法）。
     */
    void goAwayReceived(int lastKnownStream, long errorCode, ByteBuf message) throws Http2Exception;

    /**
     * Indicates whether or not a {@code GOAWAY} was sent to the remote endpoint.
     *
     * 表明GOAWAY是否已被发送到远程终端。
     */
    boolean goAwaySent();

    /**
     * Updates the local state of this {@link Http2Connection} as a result of a {@code GOAWAY} to send to the remote
     * endpoint.
     *
     * 更新Http2Connection的本地状态，作为一个GOAWAY已经发送到远程终端的结果。
     *
     * @param lastKnownStream The Last-Stream-ID in the
     * <a href="https://tools.ietf.org/html/rfc7540#section-6.8">GOAWAY</a> frame.
     *                        最后stream id，在GOAWAY帧。
     *
     * @param errorCode the Error Code in the
     * <a href="https://tools.ietf.org/html/rfc7540#section-6.8">GOAWAY</a> frame.
     * <a href="https://tools.ietf.org/html/rfc7540#section-6.8">GOAWAY</a> frame. Note that reference count ownership
     * belongs to the caller (ownership is not transferred to this method).
     *                  错误吗，在GOAWAY帧。注意引用数量所有权属于调用者（所有权没有传输到此方法）。
     *
     * @return {@code true} if the corresponding {@code GOAWAY} frame should be sent to the remote endpoint.
     *          返回true，如果相应的GOAWAY帧已经发送到了远程终端。
     */
    boolean goAwaySent(int lastKnownStream, long errorCode, ByteBuf message) throws Http2Exception;
}
