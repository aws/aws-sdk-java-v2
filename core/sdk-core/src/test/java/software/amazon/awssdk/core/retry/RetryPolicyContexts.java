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

package software.amazon.awssdk.core.retry;

import software.amazon.awssdk.core.exception.SdkException;

/**
 * Precanned instances of {@link RetryPolicyContext} and factory methods for creating contexts.
 */
public class RetryPolicyContexts {

    /**
     * Empty context object.
     */
    public static final RetryPolicyContext EMPTY = RetryPolicyContext.builder().build();

    public static RetryPolicyContext withException(SdkException e) {
        return RetryPolicyContext.builder()
                                 .exception(e)
                                 .build();
    }

    public static RetryPolicyContext withStatusCode(Integer httpStatusCode) {
        return RetryPolicyContext.builder()
                                 .httpStatusCode(httpStatusCode)
                                 .build();
    }

    public static RetryPolicyContext withRetriesAttempted(int retriesAttempted) {
        return RetryPolicyContext.builder()
                                 .retriesAttempted(retriesAttempted)
                                 .build();
    }
}
