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

package software.amazon.awssdk.core.interceptor;

import java.lang.reflect.Method;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Logger;

/**
 * An {@link ExecutionInterceptor} that filters expected 4xx client-side exceptions from being
 * recorded as errors in OpenTelemetry spans.
 * <p>
 * This interceptor uses reflection to detect if OpenTelemetry is available on the classpath.
 * If present, it intercepts 4xx HTTP responses and sets the current OpenTelemetry span's status to
 * {@code StatusCode.OK}. This prevents OpenTelemetry's default exception handling from flagging
 * these expected responses (e.g. DynamoDB Conditional Check Failures) as span errors.
 */
@SdkPublicApi
public final class OpenTelemetryErrorFilteringInterceptor implements ExecutionInterceptor {
    private static final Logger log = Logger.loggerFor(OpenTelemetryErrorFilteringInterceptor.class);

    private static final Object STATUS_CODE_OK;
    private static final Method SET_STATUS_METHOD;
    private static final Method CURRENT_SPAN_METHOD;

    static {
        Object statusCodeOk = null;
        Method setStatusMethod = null;
        Method currentSpanMethod = null;

        try {
            Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
            Class<?> statusCodeClass = Class.forName("io.opentelemetry.api.trace.StatusCode");

            // Look up StatusCode.OK
            for (Object constant : statusCodeClass.getEnumConstants()) {
                if ("OK".equals(((Enum<?>) constant).name())) {
                    statusCodeOk = constant;
                    break;
                }
            }

            // Look up Span.current()
            currentSpanMethod = spanClass.getMethod("current");

            // Look up Span.setStatus(StatusCode)
            setStatusMethod = spanClass.getMethod("setStatus", statusCodeClass);
        } catch (ClassNotFoundException e) {
            log.debug(() -> "OpenTelemetry classes were not found on the classpath. "
                            + "OpenTelemetryErrorFilteringInterceptor will be a no-op.");
        } catch (Exception e) {
            log.warn(() -> "Failed to initialize OpenTelemetry reflection. Interceptor will be a no-op.", e);
        }

        STATUS_CODE_OK = statusCodeOk;
        SET_STATUS_METHOD = setStatusMethod;
        CURRENT_SPAN_METHOD = currentSpanMethod;
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        if (STATUS_CODE_OK == null || SET_STATUS_METHOD == null || CURRENT_SPAN_METHOD == null) {
            return;
        }

        try {
            int statusCode = context.httpResponse().statusCode();
            if (statusCode >= 400 && statusCode < 500) {
                Object currentSpan = CURRENT_SPAN_METHOD.invoke(null);
                if (currentSpan != null) {
                    SET_STATUS_METHOD.invoke(currentSpan, STATUS_CODE_OK);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.debug(() -> "Failed to set OpenTelemetry span status to OK.", e);
        }
    }
}
