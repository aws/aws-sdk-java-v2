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

package software.amazon.awssdk.services.s3.internal.crt;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.crt.s3.S3MetaRequest;

/**
 * A wrapper class that manages the lifecycle of the underlying {@link S3MetaRequest}. This class is needed to ensure we don't
 * invoke methods on {@link S3MetaRequest} after it's closed, otherwise CRT will crash.
 */
@SdkInternalApi
public class S3MetaRequestWrapper {
    private final S3MetaRequest delegate;
    private volatile boolean isClosed;
    private final Object lock = new Object();

    public S3MetaRequestWrapper(S3MetaRequest delegate) {
        this.delegate = delegate;
    }

    public void close() {
        synchronized (lock) {
            if (!isClosed) {
                isClosed = true;
                delegate.close();
            }
        }
    }

    public void incrementReadWindow(long windowSize) {
        synchronized (lock) {
            if (!isClosed) {
                delegate.incrementReadWindow(windowSize);
            }
        }
    }

    public ResumeToken pause() {
        synchronized (lock) {
            if (!isClosed) {
                return delegate.pause();
            }
        }
        return null;
    }

    /**
     * Asynchronously pauses the meta request. Unlike {@link #pause()}, this is supported for GetObject as well as PutObject, and
     * the returned future only completes once all in-flight work has settled, which for a download to a file means the file has
     * been written and closed.
     *
     * @return a future completed with the resume token, or completed with {@code null} if the meta request has already been
     *         closed or CRT captured no resumable state.
     */
    public CompletableFuture<ResumeToken> pauseAsync() {
        synchronized (lock) {
            if (!isClosed) {
                return delegate.pauseAsync();
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public void cancel() {
        synchronized (lock) {
            if (!isClosed) {
                delegate.cancel();
            }
        }
    }
}
