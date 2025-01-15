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

package software.amazon.awssdk.auth.credentials;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Custom retry policy for {@link InstanceProfileCredentialsProvider} calls to retrieve credentials
 * from the Amazon EC2 Instance Metadata Service.
 */
@SdkPublicApi
public interface InstanceProfileCredentialsRetryPolicy {

    InstanceProfileCredentialsRetryPolicy NO_RETRY = (retriesAttempted, statusCode, exception) -> false;

    /**
     * Returns whether a failed request should be retried.
     *
     * @param retriesAttempted The number of times the current request has been attempted.
     * @param statusCode The http status code returned by the call to Instance Metadata Service
     * @param exception The error that caused the request to fail.
     *
     * @return True if the failed request should be retried.
     */
    boolean shouldRetry(Integer retriesAttempted, Integer statusCode, Exception exception);
}
