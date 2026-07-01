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

package software.amazon.awssdk.http.crt.internal.request;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;

/**
 * A {@link HttpRequestBodyStream} adapter whose {@link #sendRequestBody(ByteBuffer)} drains bytes from a
 * {@link BodyChunkPipe} that is fed by the caller thread. The pull callback NEVER blocks: if no data is ready,
 * it returns 0 bytes and CRT reschedules the outgoing-stream task via {@code aws_channel_schedule_task_now},
 * allowing other event-loop tasks (such as a concurrent GET response delivery) to run before the retry.
 */
@SdkInternalApi
final class PipeBackedRequestBodyStream implements HttpRequestBodyStream {

    private final BodyChunkPipe pipe;

    PipeBackedRequestBodyStream(BodyChunkPipe pipe) {
        this.pipe = pipe;
    }

    @Override
    public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
        int drained = pipe.pollDrain(bodyBytesOut);
        return drained < 0;
    }

    @Override
    public boolean resetPosition() {
        // The SDK retry layer (RetryableStage) handles request-level retries by calling prepareRequest() again,
        // CRT does not currently exercise resetPosition for HTTP/1.1, so opting out is safe in practice.
        return false;
    }
}
