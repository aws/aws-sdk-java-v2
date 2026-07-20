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

package software.amazon.awssdk.awscore.interceptor;

import java.net.UnknownHostException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * This interceptor will monitor for {@link UnknownHostException}s and provide the customer with additional information they can
 * use to debug or fix the problem.
 */
@SdkProtectedApi
public final class HelpfulUnknownHostExceptionInterceptor implements ExecutionInterceptor {
    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        if (!hasCause(context.exception(), UnknownHostException.class)) {
            return context.exception();
        }

        StringBuilder error = new StringBuilder();
        error.append("Received an UnknownHostException when attempting to interact with a service. See cause for the "
                     + "exact endpoint that is failing to resolve. ");

        error.append("If this is happening on an endpoint that previously worked, there may be a network connectivity "
                     + "issue or your DNS cache could be storing endpoints for too long.");

        return SdkClientException.builder().message(error.toString()).cause(context.exception()).build();
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
