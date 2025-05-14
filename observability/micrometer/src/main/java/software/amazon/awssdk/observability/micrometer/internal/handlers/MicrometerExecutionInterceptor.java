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

package software.amazon.awssdk.observability.micrometer.internal.handlers;

import io.micrometer.observation.Observation;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.observability.micrometer.MicrometerObservabilityPlugin;

/**
 * ExecutionInterceptor implementation that integrates with Micrometer's Observation API
 * to provide metrics and tracing for AWS SDK operations.
 */
@SdkInternalApi
public class MicrometerExecutionInterceptor implements ExecutionInterceptor {
    private static final ExecutionAttribute<Observation> OBSERVATION = new ExecutionAttribute<>("Observation");

    private final MicrometerObservabilityPlugin config;

    public MicrometerExecutionInterceptor(MicrometerObservabilityPlugin config) {
        this.config = config;
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        if (!config.isMetricsEnabled() && !config.isTracingEnabled()) {
            return;
        }

        String serviceName = executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME);
        String operationName = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        String observationName = config.metricNamespace() + "." + serviceName + "." + operationName;

        Observation observation = Observation.createNotStarted(observationName, config.observationRegistry())
                                                 .lowCardinalityKeyValue("service", serviceName)
                                                 .lowCardinalityKeyValue("operation", operationName);

        if (executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION) != null) {
            observation.lowCardinalityKeyValue("region",
                                           executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION).id());
        }

        config.customTags().forEach(tag ->
                                        observation.lowCardinalityKeyValue(tag.getKey(), tag.getValue()));

        observation.start();
        executionAttributes.putAttribute(OBSERVATION, observation);
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        Observation observation = executionAttributes.getAttribute(OBSERVATION);
        if (observation != null && context.httpResponse() != null) {
            SdkHttpResponse httpResponse = context.httpResponse();

            observation.lowCardinalityKeyValue("http.status_code",
                                               Integer.toString(httpResponse.statusCode()));
        }
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        Observation observation = executionAttributes.getAttribute(OBSERVATION);
        if (observation != null) {
            if (context.httpResponse() != null) {
                String awsRequestId = getAwsRequestId(context.httpResponse());
                if (awsRequestId != null) {
                    observation.lowCardinalityKeyValue("aws.request_id", awsRequestId);
                }
            }

            observation.stop();
        }
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        Observation observation = executionAttributes.getAttribute(OBSERVATION);
        if (observation != null) {
            Throwable exception = context.exception();
            observation.error(exception);

            observation.lowCardinalityKeyValue("error.type", exception.getClass().getSimpleName());

            observation.stop();
        }
    }

    private String getAwsRequestId(SdkHttpResponse response) {
        return response.firstMatchingHeader("x-amz-request-id").orElse(null);
    }
}

