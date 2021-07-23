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

package software.amazon.awssdk.core.internal.batchutilities;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class BatchContext<RequestT, ResponseT> {

    private RequestT request;
    private final CompletableFuture<ResponseT> response;

    public BatchContext(RequestT request, CompletableFuture<ResponseT> response) {
        this.request = request;
        this.response = response;
    }

    public RequestT request() {
        return request;
    }

    public CompletableFuture<ResponseT> response() {
        return response;
    }

    public RequestT removeRequest() {
        RequestT ret = request;
        request = null;
        return ret;
    }
}
