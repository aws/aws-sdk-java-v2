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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.internal.interceptor.TracingSystemSetting;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.SystemSetting;
import software.amazon.awssdk.utilslite.SdkInternalThreadLocal;

/**
 * The {@code TraceIdExecutionInterceptor} copies the trace details to the {@link #TRACE_ID_HEADER} header, assuming we seem to
 * be running in a lambda environment.`
 */
@SdkProtectedApi
public class TraceIdExecutionInterceptor implements ExecutionInterceptor {
    private static final String TRACE_ID_HEADER = "X-Amzn-Trace-Id";
    private static final String LAMBDA_FUNCTION_NAME_ENVIRONMENT_VARIABLE = "AWS_LAMBDA_FUNCTION_NAME";
    private static final String CONCURRENT_TRACE_ID_KEY = "AWS_LAMBDA_X_TRACE_ID";
    private static final ExecutionAttribute<String> TRACE_ID = new ExecutionAttribute<>("TraceId");

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        String traceId = SdkInternalThreadLocal.get(CONCURRENT_TRACE_ID_KEY);
        if (traceId != null) {
            executionAttributes.putAttribute(TRACE_ID, traceId);
        }
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        Optional<String> traceIdHeader = traceIdHeader(context);
        if (!traceIdHeader.isPresent()) {
            Optional<String> lambdafunctionName = lambdaFunctionNameEnvironmentVariable();
            Optional<String> traceId = traceId(executionAttributes);

            if (lambdafunctionName.isPresent() && traceId.isPresent()) {
                return context.httpRequest().copy(r -> r.putHeader(TRACE_ID_HEADER, traceId.get()));
            }
        }
        return context.httpRequest();
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        saveTraceId(executionAttributes);
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        saveTraceId(executionAttributes);
    }

    private static void saveTraceId(ExecutionAttributes executionAttributes) {
        String traceId = executionAttributes.getAttribute(TRACE_ID);
        if (traceId != null) {
            SdkInternalThreadLocal.put(CONCURRENT_TRACE_ID_KEY, executionAttributes.getAttribute(TRACE_ID));
        }
    }

    private Optional<String> traceIdHeader(Context.ModifyHttpRequest context) {
        return context.httpRequest().firstMatchingHeader(TRACE_ID_HEADER);
    }

    private Optional<String> traceId(ExecutionAttributes executionAttributes) {
        Optional<String> traceId = Optional.ofNullable(executionAttributes.getAttribute(TRACE_ID));
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