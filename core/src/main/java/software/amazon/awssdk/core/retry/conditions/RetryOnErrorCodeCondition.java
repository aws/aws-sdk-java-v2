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

package software.amazon.awssdk.core.retry.conditions;

import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

@SdkPublicApi
public class RetryOnErrorCodeCondition implements RetryCondition {

    private final Set<String> retryableErrorCodes;

    // TODO: Switch to varargs and Set.of()
    public RetryOnErrorCodeCondition(Set<String> retryableErrorCodes) {
        this.retryableErrorCodes = retryableErrorCodes;
    }

    @Override
    public boolean shouldRetry(RetryPolicyContext context) {

        Exception ex = context.exception();
        if (ex != null && ex instanceof SdkServiceException) {
            SdkServiceException exception = (SdkServiceException) ex;

            if (retryableErrorCodes.contains(exception.errorCode())) {
                return true;
            }
        }
        return false;
    }
}
