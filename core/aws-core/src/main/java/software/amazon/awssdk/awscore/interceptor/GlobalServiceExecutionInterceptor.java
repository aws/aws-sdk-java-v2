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

package software.amazon.awssdk.awscore.interceptor;

import java.net.UnknownHostException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * An interceptor that can be used for global services that will tell the customer when they're using a global service that
 * doesn't support non-global regions.
 */
@SdkProtectedApi
public class GlobalServiceExecutionInterceptor implements ExecutionInterceptor {
    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        if (hasCause(context.exception(), UnknownHostException.class) &&
            !executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION).isGlobalRegion()) {
            throw SdkClientException.builder()
                                    .message("This is a global service. Consider setting AWS_GLOBAL or another global " +
                                         "region when creating your client.")
                                    .cause(context.exception())
                                    .build();
        }
    }

    private boolean hasCause(Throwable thrown, Class<? extends Throwable> cause) {
        if (thrown == null) {
            return false;
        }

        if (cause.isAssignableFrom(thrown.getClass())) {
            return true;
        }

        return hasCause(thrown.getCause(), cause);
    }
}
