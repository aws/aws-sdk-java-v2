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

import java.util.Optional;
import org.slf4j.MDC;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.internal.interceptor.TracingSystemSetting;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * The {@code TraceIdExecutionInterceptor} copies the trace details to the {@link #TRACE_ID_HEADER} header, assuming we seem to
 * be running in a lambda environment.
 */
@SdkProtectedApi
public class TraceIdExecutionInterceptor implements ExecutionInterceptor {
    private static final String TRACE_ID_HEADER = "X-Amzn-Trace-Id";
    private static final String LAMBDA_FUNCTION_NAME_ENVIRONMENT_VARIABLE = "AWS_LAMBDA_FUNCTION_NAME";
    private static final String CONCURRENT_TRACE_ID_KEY = "AWS_LAMBDA_X_TraceId";
    private static final ExecutionAttribute<String> CACHED_TRACE_ID = new ExecutionAttribute<>("CachedTraceId");

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        String traceId = MDC.get(CONCURRENT_TRACE_ID_KEY);
        if (traceId != null) {
            executionAttributes.putAttribute(CACHED_TRACE_ID, traceId);
        }
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        Optional<String> traceIdHeader = traceIdHeader(context);
        if (!traceIdHeader.isPresent()) {
            Optional<String> lambdaFunctionName = lambdaFunctionNameEnvironmentVariable();
            Optional<String> traceId = traceId(executionAttributes);

            if (lambdaFunctionName.isPresent() && traceId.isPresent()) {
                return context.httpRequest().copy(r -> r.putHeader(TRACE_ID_HEADER, traceId.get()));
            }
        }
        return context.httpRequest();
    }

    private Optional<String> traceIdHeader(Context.ModifyHttpRequest context) {
        return context.httpRequest().firstMatchingHeader(TRACE_ID_HEADER);
    }

    private Optional<String> traceId(ExecutionAttributes executionAttributes) {
        Optional<String> traceId = Optional.ofNullable(executionAttributes.getAttribute(CACHED_TRACE_ID));
        if (traceId.isPresent()) {
            return traceId;
        }
        return TracingSystemSetting._X_AMZN_TRACE_ID.getStringValue();
    }

    private Optional<String> lambdaFunctionNameEnvironmentVariable() {
        // CHECKSTYLE:OFF - This is not configured by the customer, so it should not be configurable by system property
        return SystemSetting.getStringValueFromEnvironmentVariable(LAMBDA_FUNCTION_NAME_ENVIRONMENT_VARIABLE);
        // CHECKSTYLE:ON
    }
}
