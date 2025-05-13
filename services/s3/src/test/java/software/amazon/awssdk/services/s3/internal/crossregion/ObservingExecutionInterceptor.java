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

package software.amazon.awssdk.services.s3.internal.crossregion;

import io.micrometer.observation.ObservationRegistry;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;

import java.util.Optional;

public class ObservingExecutionInterceptor implements ExecutionInterceptor {

    // Define execution attributes to store observation-related data
    private static final ExecutionAttribute<Observation> OBSERVATION =
        new ExecutionAttribute<>("aws-sdk-observation");

    private static final ExecutionAttribute<AwsSdkOperationContext> OBSERVATION_CONTEXT =
        new ExecutionAttribute<>("aws-sdk-observation-context");

    private final ObservationRegistry registry;
    private final String observationName;

    public ObservingExecutionInterceptor(ObservationRegistry registry, String observationName) {
        this.registry = registry;
        this.observationName = observationName;
    }

    public ObservingExecutionInterceptor(ObservationRegistry registry) {
        this(registry, "aws.sdk.request");
    }

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        // Create the observation context
        AwsSdkOperationContext observationContext = new AwsSdkOperationContext();

        // Populate context with request details
        populateContext(observationContext, context);

        // Store the context in execution attributes
        executionAttributes.putAttribute(OBSERVATION_CONTEXT, observationContext);

        // Create and start the observation
        Observation observation = Observation.createNotStarted(observationName, () -> observationContext, registry)
                                             .start();

        // Store in execution attributes for later stages
        executionAttributes.putAttribute(OBSERVATION, observation);
    }

    @Override
    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        // Get the observation context
        AwsSdkOperationContext observationContext = executionAttributes.getAttribute(OBSERVATION_CONTEXT);
        if (observationContext != null) {
            // Add HTTP method and path after marshalling
            SdkHttpRequest httpRequest = context.httpRequest();
            if (httpRequest != null) {
                observationContext.setHttpMethod(httpRequest.method().name());
                observationContext.setPath(httpRequest.encodedPath());
            }
        }x
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        // Get the observation context
        AwsSdkOperationContext observationContext = executionAttributes.getAttribute(OBSERVATION_CONTEXT);
        if (observationContext != null) {
            observationContext.setNetworkStartTimeMs(System.currentTimeMillis());
        }
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        // Get the observation and context
        Observation observation = executionAttributes.getAttribute(OBSERVATION);
        AwsSdkOperationContext observationContext = executionAttributes.getAttribute(OBSERVATION_CONTEXT);

        if (observation != null && observationContext != null) {
            // Update with response details
            observationContext.setStatusCode(context.httpResponse().statusCode());

            // Stop the observation
            observation.stop();
        }
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        // Get the observation
        Observation observation = executionAttributes.getAttribute(OBSERVATION);

        if (observation != null) {
            // Record the error
            observation.error(context.exception());

            // Stop the observation
            observation.stop();
        }
    }

    private void populateContext(AwsSdkOperationContext context, Context.BeforeExecution executionContext) {
        // Extract service name from the request class
        String requestClassName = executionContext.request().getClass().getSimpleName();
        String serviceName = requestClassName.replaceAll("Request$", "");

        // Set service and operation names
        context.setServiceName(serviceName);
        context.setOperationName(serviceName);

        // Extract additional details if available
        try {
            // Try to extract bucket name for S3 requests using reflection
            if (requestClassName.contains("S3")) {
                Optional<String> bucketName = executionContext.request().getValueForField("Bucket", String.class);
                bucketName.ifPresent(context::setBucketName);
            }
        } catch (Exception e) {
            // Ignore exceptions from reflection
        }
    }

    // Custom observation context for AWS SDK operations
    static class AwsSdkOperationContext extends Observation.Context {
        private String serviceName;
        private String operationName;
        private String bucketName;
        private String httpMethod;
        private String path;
        private int statusCode;
        private long networkStartTimeMs;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getOperationName() {
            return operationName;
        }

        public void setOperationName(String operationName) {
            this.operationName = operationName;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public long getNetworkStartTimeMs() {
            return networkStartTimeMs;
        }

        public void setNetworkStartTimeMs(long networkStartTimeMs) {
            this.networkStartTimeMs = networkStartTimeMs;
        }
    }

    // Convention for AWS SDK operations
    public static class AwsSdkObservationConvention implements io.micrometer.observation.GlobalObservationConvention<AwsSdkOperationContext> {
        @Override
        public String getName() {
            return "aws.sdk.request";
        }

        @Override
        public String getContextualName(AwsSdkOperationContext context) {
            return context.getServiceName() + " " + context.getOperationName();
        }

        @Override
        public KeyValues getLowCardinalityKeyValues(AwsSdkOperationContext context) {
            KeyValues keyValues = KeyValues.of(
                "aws.service", context.getServiceName(),
                "aws.operation", context.getOperationName()
            );

            // Add HTTP method if available
            if (context.getHttpMethod() != null) {
                keyValues = keyValues.and("http.method", context.getHttpMethod());
            }

            // Add status code if available
            if (context.getStatusCode() > 0) {
                keyValues = keyValues.and("http.status_code", String.valueOf(context.getStatusCode()));
            }

            // Add bucket name if available
            if (context.getBucketName() != null) {
                keyValues = keyValues.and("bucket.name", context.getBucketName());
            }

            return keyValues;
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return context instanceof AwsSdkOperationContext;
        }
    }
}
