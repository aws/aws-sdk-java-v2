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

package software.amazon.awssdk.retries.api;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.retries.api.internal.RecordSuccessResponseImpl;

/**
 * Response given to the calling code by the {@link RetryStrategy} after calling
 * {@link RetryStrategy#recordSuccess(RecordSuccessRequest)}.
 */
@SdkPublicApi
@ThreadSafe
public interface RecordSuccessResponse {
    /**
     * A {@link RetryToken} acquired a previous {@link RetryStrategy#acquireInitialToken} or
     * {@link RetryStrategy#refreshRetryToken} call.
     */
    RetryToken token();

    /**
     * Creates a new {@link RecordSuccessResponseImpl} with the given token.
     */
    static RecordSuccessResponse create(RetryToken token) {
        return RecordSuccessResponseImpl.create(token);
    }

}
