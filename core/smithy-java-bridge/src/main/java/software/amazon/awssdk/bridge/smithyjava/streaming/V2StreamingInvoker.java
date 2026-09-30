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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.bridge.smithyjava.client.SmithyBridgeClient;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Runs a streaming operation on the smithy-java pipeline.
 *
 * <p>Generated clients call one of the two {@code invoke} methods rather than inlining this, because the
 * interesting part is not the call but the response body's lifecycle: who closes it, when, and what
 * happens to it when the caller's transformer throws. Getting that wrong leaks a connection per request,
 * which no unit test notices and every load test does.
 */
@SdkProtectedApi
public final class V2StreamingInvoker {

    private V2StreamingInvoker() {
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
     * @return the deserialized output.
     */
    public static <I extends SerializableStruct, O extends SerializableStruct> O invoke(
            SmithyBridgeClient client, I input, ApiOperation<I, O> operation, RequestBody body) {
        return client.invoke(input, operation, V2StreamingBridge.forRequestBody(body));
    }

    /**
     * Invokes an operation whose response is streamed, optionally with a streaming request body too, and
     * hands the body to the caller's {@link ResponseTransformer}.
     *
     * <p>The transformer runs <em>after</em> {@code Client#call} returns, which is the one structural
     * difference from v2 worth knowing about: in v2 the transformer runs inside the retry loop, so a
     * {@link RetryableException} thrown from it retries the whole call. Here it cannot, because retries are
     * already over. {@code compatability_issues.md} §13.4 records that.
     *
     * @param client      the bridge client.
     * @param input       operation input.
     * @param operation   the generated operation.
     * @param body        the caller's request body, or null if the operation does not stream its input.
     * @param transformer the caller's response transformer.
     * @param <I>         input shape.
     * @param <O>         output shape.
     * @param <ReturnT>   whatever the transformer produces.
     * @return the transformer's result.
     */
    public static <I extends SerializableStruct, O extends SerializableStruct, ReturnT> ReturnT invoke(
            SmithyBridgeClient client, I input, ApiOperation<I, O> operation, RequestBody body,
            ResponseTransformer<O, ReturnT> transformer) {

        AtomicReference<DataStream> sink = new AtomicReference<>();
        Consumer<RequestOverrideConfig.Builder> overrides = body == null
                                         ? V2StreamingBridge.forResponseBody(sink)
                                         : V2StreamingBridge.forBoth(body, sink);

        O response = client.invoke(input, operation, overrides);

        // A success with no body at all is legal -- a 204, or a HEAD-shaped response -- and the transformer
        // still expects a stream, so give it an empty one rather than null.
        DataStream responseBody = sink.get();
        AbortableInputStream stream = responseBody == null
                                      ? AbortableInputStream.createEmpty()
                                      : V2DataStreams.toAbortableInputStream(responseBody);

        boolean leaveOpen = transformer.needsConnectionLeftOpen();
        boolean transformed = false;
        try {
            ReturnT result = transform(transformer, response, stream);
            transformed = true;
            return result;
        } finally {
            // Close unless the transformer handed the stream to the caller (toInputStream()), and always
            // close when the transformer threw -- a transformer that failed is not going to close what it
            // never finished reading, no matter what needsConnectionLeftOpen() claims.
            if (!leaveOpen || !transformed) {
                closeQuietly(stream);
            }
        }
    }

    private static <O, ReturnT> ReturnT transform(ResponseTransformer<O, ReturnT> transformer, O response,
                                                  AbortableInputStream stream) {
        // Mirrors BaseSyncClientHandler.transformResponse, including the interrupt checks either side: a
        // transformer that blocks on a slow body is the most likely place for a caller's cancellation to
        // land, and v2 callers are documented to be able to interrupt there.
        try {
            InterruptMonitor.checkInterrupted();
            ReturnT result = transformer.transform(response, stream);
            InterruptMonitor.checkInterrupted();
            return result;
        } catch (RetryableException | AbortedException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AbortedException.create("Thread was interrupted while transforming the response", e);
        } catch (Exception e) {
            // v2 re-checks the interrupt flag here, so that an interrupt which surfaced as some unrelated
            // exception (a closed stream, say) is still reported as an interrupt rather than as a
            // transformer bug.
            if (Thread.currentThread().isInterrupted()) {
                throw AbortedException.create("Thread was interrupted while transforming the response", e);
            }
            throw NonRetryableException.builder().cause(e).build();
        }
    }

    private static void closeQuietly(AbortableInputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            // Closing a response body is cleanup. Letting this escape would replace the caller's result,
            // or their exception, with a failure about the socket teardown.
        }
    }
}
