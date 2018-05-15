/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.regions.util.ResourcesEndpointRetryParameters;
import software.amazon.awssdk.regions.util.ResourcesEndpointRetryPolicy;

@SdkInternalApi
class ContainerCredentialsRetryPolicy implements ResourcesEndpointRetryPolicy {

    /** Max number of times a request is retried before failing. */
    private static final int MAX_RETRIES = 5;

    @Override
    public boolean shouldRetry(int retriesAttempted, ResourcesEndpointRetryParameters retryParams) {
        if (retriesAttempted >= MAX_RETRIES) {
            return false;
        }

        Integer statusCode = retryParams.getStatusCode();
        if (statusCode != null && HttpStatusFamily.of(statusCode) == HttpStatusFamily.SERVER_ERROR) {
            return true;
        }

        if (retryParams.getException() != null && retryParams.getException() instanceof IOException) {
            return true;
        }

        return false;
    }

}
