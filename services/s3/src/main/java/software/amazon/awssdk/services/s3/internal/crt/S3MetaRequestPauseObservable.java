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
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.s3.ResumeToken;

/**
 * An observable that notifies the observer {@link S3CrtAsyncHttpClient} to pause the request.
 */
@SdkInternalApi
public class S3MetaRequestPauseObservable {

    private final Function<S3MetaRequestWrapper, ResumeToken> pause;
    private volatile S3MetaRequestWrapper request;

    public S3MetaRequestPauseObservable() {
        this.pause = S3MetaRequestWrapper::pause;
    }

    /**
     * Subscribe {@link S3MetaRequestWrapper} to be potentially paused later.
     */
    public void subscribe(S3MetaRequestWrapper request) {
        this.request = request;
    }

    /**
     * Pause the request
     */
    public ResumeToken pause() {
        if (this.request != null) {
            return pause.apply(this.request);
        }
        return null;
    }

    /**
     * Asynchronously pause the request. Unlike {@link #pause()}, this is supported for GetObject as well as PutObject, and the
     * returned future only completes once CRT has finished any in-flight work.
     *
     * @return a future completed with the resume token, or completed with {@code null} if the request has not been subscribed
     *         yet or CRT captured no resumable state.
     */
    public CompletableFuture<ResumeToken> pauseAsync() {
        S3MetaRequestWrapper metaRequest = this.request;
        if (metaRequest != null) {
            return metaRequest.pauseAsync();
        }
        return CompletableFuture.completedFuture(null);
    }
}

