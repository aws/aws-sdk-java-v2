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

package software.amazon.awssdk.awscore.internal.authcontext;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Will create the correct authorization strategy based on provided credential type.
 */
@SdkInternalApi
public final class AuthorizationStrategyFactory {

    private final SdkRequest request;
    private final MetricCollector metricCollector;
    private final SdkClientConfiguration clientConfiguration;

    public AuthorizationStrategyFactory(SdkRequest request,
                                        MetricCollector metricCollector,
                                        SdkClientConfiguration clientConfiguration) {
        this.request = request;
        this.metricCollector = metricCollector;
        this.clientConfiguration = clientConfiguration;
    }

    public AuthorizationStrategy strategyFor(CredentialType credentialType) {
        if (credentialType == CredentialType.TOKEN) {
            return tokenAuthorizationStrategy();
        }
        return awsCredentialsAuthorizationStrategy();
    }

    private TokenAuthorizationStrategy tokenAuthorizationStrategy() {
        Signer defaultSigner = clientConfiguration.option(SdkAdvancedClientOption.TOKEN_SIGNER);
        SdkTokenProvider defaultTokenProvider = clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER);
        return TokenAuthorizationStrategy.builder()
                                         .request(request)
                                         .defaultSigner(defaultSigner)
                                         .defaultTokenProvider(defaultTokenProvider)
                                         .metricCollector(metricCollector)
                                         .build();
    }

    private AwsCredentialsAuthorizationStrategy awsCredentialsAuthorizationStrategy() {
        Signer defaultSigner = clientConfiguration.option(SdkAdvancedClientOption.SIGNER);
        AwsCredentialsProvider defaultCredentialsProvider = clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER);
        return AwsCredentialsAuthorizationStrategy.builder()
                                                  .request(request)
                                                  .defaultSigner(defaultSigner)
                                                  .defaultCredentialsProvider(defaultCredentialsProvider)
                                                  .metricCollector(metricCollector)
                                                  .build();
    }
}
