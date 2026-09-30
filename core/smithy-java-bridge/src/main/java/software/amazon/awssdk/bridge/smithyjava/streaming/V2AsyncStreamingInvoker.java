/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.bridge.smithyjava.streaming;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.bridge.smithyjava.client.SmithyBridgeClient;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Runs a streaming operation on the smithy-java pipeline for a generated v2 <em>async</em> client.
 *
 * <p>The async sibling of {@link V2StreamingInvoker}, and simpler than it, because nothing here reads a
 * body. The caller's {@link AsyncRequestBody} is handed to the transport as a publisher, and the response
 * body is handed to the caller's {@link AsyncResponseTransformer} as one; bytes move on the transport's
 * event loops in both directions, and the only thread this class occupies is the call envelope's virtual
 * thread, and only until response headers arrive (see {@link SmithyBridgeClient#runAsync}).
 *
 * <p>That is also the fix for the two façade caveats that were about bodies. A deferred-consumption
 * transformer such as {@code toBlockingInputStream()} no longer holds a thread for as long as the caller
 * takes to read: its future completes on {@code onStream}, the envelope thread exits, and the caller's
 * reads drain the transport directly ({@code compatability_issues.md} 14.4). And a body publisher can no
 * longer deadlock against the pool running its own call, because there is no pool (14.2).
 */
@SdkProtectedApi
public final class V2AsyncStreamingInvoker {

    private V2AsyncStreamingInvoker() {
    }

    /**
     * Invokes an operation with a streaming request body and a non-streaming response.
     *
     * @param client    the bridge client.
     * @param input     operation input.
     * @param operation the generated operation.
     * @param body      the caller's request body.
     * @param <I>       input shape.
     * @param <O>       output shape.
     * @return a future of the deserialized output.
     */
    public static <I extends SerializableStruct, O extends SerializableStruct> CompletableFuture<O> invoke(
            SmithyBridgeClient client, I input, ApiOperation<I, O> operation, AsyncRequestBody body) {
        return client.invokeAsync(input, operation, V2StreamingBridge.forRequestBody(V2DataStreams.toDataStream(body)));
    }

    /**
     * Invokes an operation whose response is streamed, optionally with a streaming request body too, and
     * feeds the body to the caller's {@link AsyncResponseTransformer}.
     *
     * <p>The transformer's lifecycle follows v2's order — {@code prepare}, then {@code onResponse}, then
     * {@code onStream}, or {@code exceptionOccurred} on failure — with one difference: v2 calls
     * {@code prepare} once per <em>attempt</em>, inside its retry loop, and this calls it once per
     * <em>call</em>, because smithy-java's retry loop is not reachable from here. The same structural
     * difference as the sync invoker's, recorded as {@code compatability_issues.md} 13.4.
     *
     * @param client      the bridge client.
     * @param input       operation input.
     * @param operation   the generated operation.
     * @param body        the caller's request body, or null if the operation does not stream its input.
     * @param transformer the caller's response transformer.
     * @param <I>         input shape.
     * @param <O>         output shape.
     * @param <ReturnT>   whatever the transformer produces.
     * @return a future of the transformer's result.
     */
    public static <I extends SerializableStruct, O extends SerializableStruct, ReturnT> CompletableFuture<ReturnT> invoke(
            SmithyBridgeClient client, I input, ApiOperation<I, O> operation, AsyncRequestBody body,
            AsyncResponseTransformer<O, ReturnT> transformer) {

        return client.runAsync(() -> {
            AtomicReference<DataStream> sink = new AtomicReference<>();
            Consumer<RequestOverrideConfig.Builder> overrides = body == null
                                             ? V2StreamingBridge.forResponseBody(sink)
                                             : V2StreamingBridge.forBoth(V2DataStreams.toDataStream(body), sink);

            // Before the request, as v2 does: some transformers create their destination here (a file,
            // a buffer sized from nothing), and the future returned now is the one the caller will get.
            CompletableFuture<ReturnT> transformed = transformer.prepare();

            O response;
            try {
                response = client.invoke(input, operation, overrides);
            } catch (RuntimeException e) {
                // invoke has already translated this to the v2 exception. The transformer is told
                // first, as v2 tells it, so a toFile() transformer can delete its partial file.
                transformer.exceptionOccurred(e);
                throw e;
            }

            // A success with no body at all is legal -- a 204, or a HEAD-shaped response -- and the
            // transformer still expects a stream, so give it an empty one rather than none.
            DataStream responseBody = sink.get();
            transformer.onResponse(response);
            transformer.onStream(V2DataStreams.toSdkPublisher(responseBody == null ? DataStream.ofEmpty() : responseBody));
            return transformed;
        });
    }
}
