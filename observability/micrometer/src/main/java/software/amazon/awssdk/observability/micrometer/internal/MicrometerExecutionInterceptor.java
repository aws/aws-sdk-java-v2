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

package software.amazon.awssdk.observability.micrometer.internal;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpResponse;

@SdkInternalApi
public class MicrometerExecutionInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<Observation> OBSERVATION = new ExecutionAttribute<Observation>("Observation");

    ObservationRegistry observationRegistry;

    public MicrometerExecutionInterceptor(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        Observation observation =
            Observation.start("aws.sdk.request", observationRegistry)
                       .lowCardinalityKeyValue("service", executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME))
                       .lowCardinalityKeyValue("operation", executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME))
                       .lowCardinalityKeyValue("region", executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION).id());

        executionAttributes.putAttribute(OBSERVATION, observation);
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        Observation observation = executionAttributes.getAttribute(OBSERVATION);

        if (observation != null) {
            SdkHttpResponse httpResponse = context.httpResponse();

            observation.lowCardinalityKeyValue("http.status_code", Integer.toString(httpResponse.statusCode()));

            observation.stop();
        }
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        // TODO - pass along metric stage values - SdkInternalExecutionAttribute

    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        Observation observation = executionAttributes.getAttribute(OBSERVATION);
        if (observation != null) {
            observation.error(context.exception());
            observation.stop();
        }
    }
}
