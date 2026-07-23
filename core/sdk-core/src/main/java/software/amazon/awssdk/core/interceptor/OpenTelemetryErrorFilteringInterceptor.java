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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.utils.Logger;

/**
 * An {@link ExecutionInterceptor} that filters expected exceptions from being
 * recorded as errors in OpenTelemetry spans.
 * <p>
 * This interceptor uses reflection to detect if OpenTelemetry is available on the classpath.
 * If present, it intercepts exceptions and sets the current OpenTelemetry span's status to
 * {@code StatusCode.OK} if the exception is registered to be ignored. This prevents OpenTelemetry's
 * default exception handling from flagging these expected exceptions (e.g. DynamoDB Conditional
 * Check Failures) as span errors.
 */
@SdkPublicApi
public final class OpenTelemetryErrorFilteringInterceptor implements ExecutionInterceptor {
    private static final Logger log = Logger.loggerFor(OpenTelemetryErrorFilteringInterceptor.class);

    private static final Object STATUS_CODE_OK;
    private static final Method SET_STATUS_METHOD;
    private static final Method CURRENT_SPAN_METHOD;
    private static final Set<Class<? extends Throwable>> IGNORED_EXCEPTIONS = ConcurrentHashMap.newKeySet();

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

        initializeFromSystemSettings();
    }

    /**
     * Registers exception classes to be ignored.
     * If any exception in the cause chain matches or inherits from these classes, the OpenTelemetry span status
     * will be set to OK to prevent it from being recorded as an error.
     */
    @SafeVarargs
    public static void addIgnoredExceptions(Class<? extends Throwable>... exceptions) {
        if (exceptions != null) {
            Collections.addAll(IGNORED_EXCEPTIONS, exceptions);
        }
    }

    /**
     * Registers exception classes to be ignored.
     * If any exception in the cause chain matches or inherits from these classes, the OpenTelemetry span status
     * will be set to OK to prevent it from being recorded as an error.
     */
    public static void addIgnoredExceptions(Collection<Class<? extends Throwable>> exceptions) {
        if (exceptions != null) {
            IGNORED_EXCEPTIONS.addAll(exceptions);
        }
    }

    /**
     * Clears all registered ignored exceptions. Intended for testing.
     */
    static void clearIgnoredExceptions() {
        IGNORED_EXCEPTIONS.clear();
    }

    static void initializeFromSystemSettings() {
        try {
            SdkSystemSetting.AWS_OTEL_IGNORED_EXCEPTIONS.getStringValue().ifPresent(setting -> {
                for (String entry : setting.split(",")) {
                    String className = entry.trim();
                    if (!className.isEmpty()) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (Throwable.class.isAssignableFrom(clazz)) {
                                @SuppressWarnings("unchecked")
                                Class<? extends Throwable> throwableClazz = (Class<? extends Throwable>) clazz;
                                IGNORED_EXCEPTIONS.add(throwableClazz);
                            } else {
                                log.warn(() -> "Configured class " + className + " is not a subclass of Throwable.");
                            }
                        } catch (ClassNotFoundException e) {
                            log.warn(() -> "Configured exception class not found: " + className, e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.warn(() -> "Failed to initialize ignored exceptions from system settings.", e);
        }
    }

    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        if (STATUS_CODE_OK == null || SET_STATUS_METHOD == null || CURRENT_SPAN_METHOD == null || IGNORED_EXCEPTIONS.isEmpty()) {
            return context.exception();
        }

        try {
            Throwable exception = context.exception();
            if (shouldIgnoreException(exception)) {
                Object currentSpan = CURRENT_SPAN_METHOD.invoke(null);
                if (currentSpan != null) {
                    SET_STATUS_METHOD.invoke(currentSpan, STATUS_CODE_OK);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.debug(() -> "Failed to set OpenTelemetry span status to OK.", e);
        }

        return context.exception();
    }

    private boolean shouldIgnoreException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            for (Class<? extends Throwable> ignoredClass : IGNORED_EXCEPTIONS) {
                if (ignoredClass.isInstance(current)) {
                    return true;
                }
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return false;
    }
}
