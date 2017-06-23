/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import java.io.IOException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.retry.internal.CredentialsEndpointRetryParameters;
import software.amazon.awssdk.retry.internal.CredentialsEndpointRetryPolicy;

@SdkInternalApi
class ContainerCredentialsRetryPolicy implements CredentialsEndpointRetryPolicy {

    /** Max number of times a request is retried before failing. */
    private static final int MAX_RETRIES = 5;

    @Override
    public boolean shouldRetry(int retriesAttempted, CredentialsEndpointRetryParameters retryParams) {
        if (retriesAttempted >= MAX_RETRIES) {
            return false;
        }

        Integer statusCode = retryParams.getStatusCode();
        if (statusCode != null && statusCode >= 500 && statusCode < 600) {
            return true;
        }

        if (retryParams.getException() != null && retryParams.getException() instanceof IOException) {
            return true;
        }

        return false;
    }

}
