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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class BatchingExecutionContext<RequestT, ResponseT> {

    private final RequestT request;
    private final CompletableFuture<ResponseT> response;

    private final Optional<Integer> responsePayloadByteSize;

    public BatchingExecutionContext(RequestT request, CompletableFuture<ResponseT> response) {
        this.request = request;
        this.response = response;
        responsePayloadByteSize = RequestPayloadCalculator.calculateMessageSize(request);
    }

    public RequestT request() {
        return request;
    }

    public CompletableFuture<ResponseT> response() {
        return response;
    }

    /**
     * Optional because responsePayloadByteSize is required only for SendMessageRequests and not for other requests.
     */
    public Optional<Integer> responsePayloadByteSize() {
        return responsePayloadByteSize;
    }
}
