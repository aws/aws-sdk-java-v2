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

package software.amazon.awssdk.core.internal.sync;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.sync.ResponseTransformer;

/**
 * A {@link ResponseTransformer} that can reject a call before the SDK dispatches the request. This is the synchronous
 * counterpart to {@link software.amazon.awssdk.core.async.AsyncResponseTransformer#prepare()}, kept internal so no
 * public API is added.
 *
 * @param <ResponseT> Type of unmarshalled response POJO.
 */
@SdkInternalApi
public interface ValidatingResponseTransformer<ResponseT> extends ResponseTransformer<ResponseT, ResponseT> {

    /**
     * Called by the SDK immediately before the request is dispatched. Throwing fails the call without sending a
     * request.
     */
    void validate();
}
