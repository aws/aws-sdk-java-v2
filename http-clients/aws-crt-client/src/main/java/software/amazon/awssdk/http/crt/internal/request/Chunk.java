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

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A reusable byte buffer carrying request body data through the {@link BodyChunkPipe}.
 *
 * <p>Internal to this package; used only by {@link BodyChunkPipe} and {@link SyncRequestBodyPump}.
 *
 * <p>{@code pos} and {@code len} are intentionally non-volatile: hand-off between the producer and
 * consumer always goes through the {@code ArrayBlockingQueue} (or the {@code freeLock} monitor on
 * recycle), both of which provide release/acquire happens-before for the field writes.
 */
@SdkInternalApi
final class Chunk {
    private final byte[] data;
    private int pos;
    private int len;

    Chunk(int chunkSize) {
        this.data = new byte[chunkSize];
    }

    byte[] data() {
        return data;
    }

    int pos() {
        return pos;
    }

    void pos(int pos) {
        this.pos = pos;
    }

    int len() {
        return len;
    }

    void len(int len) {
        this.len = len;
    }
}
