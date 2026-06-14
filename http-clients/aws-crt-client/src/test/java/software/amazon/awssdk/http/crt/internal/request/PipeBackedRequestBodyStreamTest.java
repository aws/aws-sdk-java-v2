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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class PipeBackedRequestBodyStreamTest {

    @Test
    void sendRequestBody_emptyOpenPipe_returnsFalseAndCopiesNothing() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        PipeBackedRequestBodyStream stream = new PipeBackedRequestBodyStream(pipe);
        ByteBuffer dst = ByteBuffer.allocate(8);

        boolean done = stream.sendRequestBody(dst);

        assertThat(done).isFalse();
        assertThat(dst.position()).isZero();
    }

    @Test
    void sendRequestBody_afterEofAndDrained_returnsTrue() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        ByteBuffer bb = pipe.acquireForFill();
        byte[] payload = {1, 2, 3};
        bb.put(payload);
        bb.flip();
        pipe.publish(bb);
        pipe.signalEof();
        PipeBackedRequestBodyStream stream = new PipeBackedRequestBodyStream(pipe);

        ByteBuffer first = ByteBuffer.allocate(8);
        boolean firstDone = stream.sendRequestBody(first);
        ByteBuffer second = ByteBuffer.allocate(8);
        boolean secondDone = stream.sendRequestBody(second);

        assertThat(firstDone).isFalse();
        assertThat(first.position()).isEqualTo(payload.length);
        assertThat(secondDone).isTrue();
        assertThat(second.position()).isZero();
    }

    @Test
    void sendRequestBody_pipeInError_throwsRuntimeExceptionWithCause() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        IllegalStateException cause = new IllegalStateException("upstream broke");
        pipe.signalError(cause);
        PipeBackedRequestBodyStream stream = new PipeBackedRequestBodyStream(pipe);

        assertThatThrownBy(() -> stream.sendRequestBody(ByteBuffer.allocate(8)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Producer failed")
            .hasRootCauseMessage("upstream broke");
    }

    @Test
    void sendRequestBody_pipeAborted_throwsRuntimeException() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        pipe.abort();
        PipeBackedRequestBodyStream stream = new PipeBackedRequestBodyStream(pipe);

        assertThatThrownBy(() -> stream.sendRequestBody(ByteBuffer.allocate(8)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("aborted");
    }

    @Test
    void resetPosition_returnsFalse() {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 8);
        PipeBackedRequestBodyStream stream = new PipeBackedRequestBodyStream(pipe);

        assertThat(stream.resetPosition()).isFalse();
    }

    /**
     * When CRT's destination buffer is smaller than the chunk size, draining a single chunk
     * requires multiple {@code sendRequestBody} calls. This exercises {@link BodyChunkPipe#pollDrain}'s
     * {@code pendingDrain} state being carried across consumer invocations.
     */
    @Test
    void sendRequestBody_destinationSmallerThanChunk_drainsAcrossMultipleCalls() throws Exception {
        BodyChunkPipe pipe = new BodyChunkPipe(2, 16);
        ByteBuffer bb = pipe.acquireForFill();
        byte[] payload = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        bb.put(payload);
        bb.flip();
        pipe.publish(bb);
        pipe.signalEof();
        PipeBackedRequestBodyStream stream = new PipeBackedRequestBodyStream(pipe);

        ByteBuffer first = ByteBuffer.allocate(3);
        ByteBuffer second = ByteBuffer.allocate(3);
        ByteBuffer third = ByteBuffer.allocate(3);
        ByteBuffer fourth = ByteBuffer.allocate(3);
        ByteBuffer fifth = ByteBuffer.allocate(3);
        boolean firstDone = stream.sendRequestBody(first);
        boolean secondDone = stream.sendRequestBody(second);
        boolean thirdDone = stream.sendRequestBody(third);
        boolean fourthDone = stream.sendRequestBody(fourth);
        boolean fifthDone = stream.sendRequestBody(fifth);

        assertThat(firstDone).isFalse();
        assertThat(secondDone).isFalse();
        assertThat(thirdDone).isFalse();
        assertThat(fourthDone).isFalse();
        assertThat(fifthDone).isTrue();
        assertThat(first.position()).isEqualTo(3);
        assertThat(second.position()).isEqualTo(3);
        assertThat(third.position()).isEqualTo(3);
        assertThat(fourth.position()).isEqualTo(1);
        assertThat(fifth.position()).isZero();

        byte[] reassembled = new byte[payload.length];
        first.flip();
        first.get(reassembled, 0, 3);
        second.flip();
        second.get(reassembled, 3, 3);
        third.flip();
        third.get(reassembled, 6, 3);
        fourth.flip();
        fourth.get(reassembled, 9, 1);
        assertThat(reassembled).containsExactly(payload);
    }
}
