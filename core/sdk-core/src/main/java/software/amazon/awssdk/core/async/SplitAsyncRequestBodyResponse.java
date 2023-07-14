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

package software.amazon.awssdk.core.async;


import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Containing the result from {@link AsyncRequestBody#split(long, long)}
 */
@SdkPublicApi
public final class SplitAsyncRequestBodyResponse {
    private final SdkPublisher<AsyncRequestBody> asyncRequestBody;
    private final CompletableFuture<Void> future;

    private SplitAsyncRequestBodyResponse(SdkPublisher<AsyncRequestBody> asyncRequestBody, CompletableFuture<Void> future) {
        this.asyncRequestBody = Validate.paramNotNull(asyncRequestBody, "asyncRequestBody");
        this.future = Validate.paramNotNull(future, "future");
    }

    public static SplitAsyncRequestBodyResponse create(SdkPublisher<AsyncRequestBody> asyncRequestBody, CompletableFuture<Void> future) {
        return new SplitAsyncRequestBodyResponse(asyncRequestBody, future);
    }

    /**
     * Returns the converted {@link SdkPublisher} of {@link AsyncRequestBody}s. Each {@link AsyncRequestBody} publishes a specific
     * portion of the original data.
     */
    public SdkPublisher<AsyncRequestBody> asyncRequestBodyPublisher() {
        return asyncRequestBody;
    }

    /**
     * Returns {@link CompletableFuture} that will be notified when all data has been consumed or if an error occurs.
     */
    public CompletableFuture<Void> future() {
        return future;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SplitAsyncRequestBodyResponse that = (SplitAsyncRequestBodyResponse) o;

        if (!asyncRequestBody.equals(that.asyncRequestBody)) {
            return false;
        }
        return future.equals(that.future);
    }

    @Override
    public int hashCode() {
        int result = asyncRequestBody.hashCode();
        result = 31 * result + future.hashCode();
        return result;
    }
}

