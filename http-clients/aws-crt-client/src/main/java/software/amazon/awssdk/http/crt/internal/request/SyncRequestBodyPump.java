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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * Caller-thread producer that reads from the customer's {@link InputStream} and publishes chunks to a
 * {@link BodyChunkPipe}. Runs on the caller (sync) thread between stream activation and
 * {@code responseFuture.join()}, ensuring the blocking {@code read()} happens off the CRT event loop.
 */
@SdkInternalApi
public final class SyncRequestBodyPump {
    private static final Logger LOG = Logger.loggerFor(SyncRequestBodyPump.class);

    private final ContentStreamProvider contentStreamProvider;
    private final BodyChunkPipe pipe;
    private final String tag;

    SyncRequestBodyPump(ContentStreamProvider contentStreamProvider, BodyChunkPipe pipe) {
        this(contentStreamProvider, pipe, "-");
    }

    SyncRequestBodyPump(ContentStreamProvider contentStreamProvider, BodyChunkPipe pipe, String reqId) {
        this.contentStreamProvider = contentStreamProvider;
        this.pipe = pipe;
        this.tag = "[reqId=" + reqId + "] ";
    }

    /**
     * Pump the entire input stream into the pipe. Runs on the caller thread; never invoked on the CRT
     * event-loop thread. On EOF signals the pipe normally; on {@link IOException} signals an error and rethrows.
     */
    public void pump() throws IOException {
        LOG.info(() -> tag + "pump() entered");
        try (InputStream in = contentStreamProvider.newStream()) {
            while (true) {
                ByteBuffer chunk = pipe.acquireForFill();
                if (chunk == null) {
                    LOG.info(() -> tag + "pump() exiting due to abort (acquireForFill returned null)");
                    return;
                }
                int read;
                try {
                    read = in.read(chunk.array(), chunk.arrayOffset() + chunk.position(), chunk.remaining());
                } catch (IOException ioe) {
                    LOG.info(() -> tag + "pump() exiting due to error: " + ioe.getMessage());
                    pipe.signalError(ioe);
                    throw ioe;
                }
                if (read < 0) {
                    LOG.info(() -> tag + "pump() exiting due to eof");
                    pipe.signalEof();
                    return;
                }
                chunk.position(chunk.position() + read);
                chunk.flip();
                pipe.publish(chunk);
            }
        } catch (InterruptedException ie) {
            LOG.info(() -> tag + "pump() exiting due to interrupt");
            pipe.abort();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while writing request body", ie);
        }
    }

    /**
     * Abort the underlying pipe (e.g., when the caller's {@code call()} is cancelled).
     */
    public void abort() {
        LOG.info(() -> tag + "pump.abort() called");
        pipe.abort();
    }
}
