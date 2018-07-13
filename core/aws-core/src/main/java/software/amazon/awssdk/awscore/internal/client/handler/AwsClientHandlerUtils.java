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

package software.amazon.awssdk.awscore.internal.client.handler;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.internal.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.internal.interceptor.InterceptorContext;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class AwsClientHandlerUtils {

    private AwsClientHandlerUtils() {

    }

    public static ExecutionContext createExecutionContext(SdkRequest originalRequest,
                                                   SdkClientConfiguration clientConfig) {

        AwsCredentialsProvider clientCredentials = clientConfig.option(AwsClientOption.CREDENTIALS_PROVIDER);
        AwsCredentialsProvider credentialsProvider = originalRequest.overrideConfiguration()
                                                                    .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                                                                    .map(c -> (AwsRequestOverrideConfiguration) c)
                                                                    .flatMap(AwsRequestOverrideConfiguration::credentialsProvider)
                                                                    .orElse(clientCredentials);

        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        Validate.validState(credentials != null, "Credential providers must never return null.");

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG, clientConfig.option(SdkClientOption.SERVICE_CONFIGURATION))
            .putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, credentials)
            .putAttribute(AwsSignerExecutionAttribute.REQUEST_CONFIG,
                          originalRequest.overrideConfiguration()
                                         .map(c -> (RequestOverrideConfiguration) c)
                                         .orElse(AwsRequestOverrideConfiguration.builder().build()))
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME,
                          clientConfig.option(AwsClientOption.SERVICE_SIGNING_NAME))
            .putAttribute(AwsExecutionAttribute.AWS_REGION, clientConfig.option(AwsClientOption.AWS_REGION))
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, clientConfig.option(AwsClientOption.SIGNING_REGION));

        ExecutionInterceptorChain executionInterceptorChain =
                new ExecutionInterceptorChain(clientConfig.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        return ExecutionContext.builder()
                               .interceptorChain(executionInterceptorChain)
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(originalRequest)
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .signer(clientConfig.option(AwsAdvancedClientOption.SIGNER))
                               .build();
    }
}
