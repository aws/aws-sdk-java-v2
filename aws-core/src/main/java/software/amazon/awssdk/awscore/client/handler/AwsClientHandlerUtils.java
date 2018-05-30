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

package software.amazon.awssdk.awscore.client.handler;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.AwsExecutionAttributes;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.config.AwsClientConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
final class AwsClientHandlerUtils {

    private AwsClientHandlerUtils() {

    }

    static ExecutionContext createExecutionContext(SdkRequest originalRequest,
                                                   AwsClientConfiguration clientConfiguration,
                                                   ServiceConfiguration serviceConfiguration) {

        AwsCredentialsProvider credentialsProvider = originalRequest.overrideConfiguration()
                                                                    .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                                                                    .map(c -> (AwsRequestOverrideConfiguration) c)
                                                                    .flatMap(AwsRequestOverrideConfiguration::credentialsProvider)
                                                                    .orElse(clientConfiguration.credentialsProvider());

        ClientOverrideConfiguration overrideConfiguration = clientConfiguration.overrideConfiguration();

        AwsCredentials credentials = credentialsProvider.getCredentials();

        Validate.validState(credentials != null, "Credential providers must never return null.");

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(AwsExecutionAttributes.SERVICE_CONFIG, serviceConfiguration)
            .putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentials)
            .putAttribute(AwsExecutionAttributes.REQUEST_CONFIG, originalRequest.overrideConfiguration()
                                                                                .map(c -> (RequestOverrideConfiguration) c)
                                                                                .orElse(AwsRequestOverrideConfiguration.builder()
                                                                                                                       .build()))
            .putAttribute(AwsExecutionAttributes.SERVICE_SIGNING_NAME,
                          overrideConfiguration.advancedOption(AwsAdvancedClientOption.SERVICE_SIGNING_NAME))
            .putAttribute(AwsExecutionAttributes.AWS_REGION,
                          overrideConfiguration.advancedOption(AwsAdvancedClientOption.AWS_REGION))
            .putAttribute(AwsExecutionAttributes.SIGNING_REGION,
                          overrideConfiguration.advancedOption(AwsAdvancedClientOption.SIGNING_REGION));

        return ExecutionContext.builder()
                               .interceptorChain(new ExecutionInterceptorChain(overrideConfiguration.executionInterceptors()))
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(originalRequest)
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .signer(overrideConfiguration.advancedOption(AwsAdvancedClientOption.SIGNER))
                               .build();
    }
}
