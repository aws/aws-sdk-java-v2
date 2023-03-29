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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.metrics.MetricCollector;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationStrategyFactoryTest {

    @Mock SdkRequest sdkRequest;
    @Mock MetricCollector metricCollector;

    @Before
    public void setUp() throws Exception {
        when(sdkRequest.overrideConfiguration()).thenReturn(Optional.empty());
    }

    @Test
    public void credentialTypeBearerToken_returnsTokenStrategy() {
        AuthorizationStrategyFactory factory = new AuthorizationStrategyFactory(sdkRequest, metricCollector,
                                                                                SdkClientConfiguration.builder().build());
        AuthorizationStrategy authorizationStrategy = factory.strategyFor(CredentialType.TOKEN);
        assertThat(authorizationStrategy).isExactlyInstanceOf(TokenAuthorizationStrategy.class);
    }

    @Test
    public void credentialTypeAwsCredentials_returnsCredentialsStrategy() {
        SdkClientConfiguration configuration = SdkClientConfiguration
            .builder()
            .option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, StaticCredentialsProvider.create(AwsBasicCredentials.create(
                "akid", "skid")))
            .build();
        AuthorizationStrategyFactory factory = new AuthorizationStrategyFactory(sdkRequest, metricCollector, configuration);
        AuthorizationStrategy authorizationStrategy = factory.strategyFor(CredentialType.of("AWS"));
        assertThat(authorizationStrategy).isExactlyInstanceOf(AwsCredentialsAuthorizationStrategy.class);
        ExecutionAttributes attributes = new ExecutionAttributes();
        authorizationStrategy.addCredentialsToExecutionAttributes(attributes);
        assertThat(attributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS).accessKeyId()).isEqualTo("akid");
        assertThat(attributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS).secretAccessKey()).isEqualTo("skid");
    }

    @Test
    public void credentialTypeAwsCredentials_withOldClientOption_returnsCredentialsStrategy() {
        SdkClientConfiguration configuration = SdkClientConfiguration
            .builder()
            .option(AwsClientOption.CREDENTIALS_PROVIDER, StaticCredentialsProvider.create(AwsBasicCredentials.create(
                "akid", "skid")))
            .build();
        AuthorizationStrategyFactory factory = new AuthorizationStrategyFactory(sdkRequest, metricCollector, configuration);
        AuthorizationStrategy authorizationStrategy = factory.strategyFor(CredentialType.of("AWS"));
        assertThat(authorizationStrategy).isExactlyInstanceOf(AwsCredentialsAuthorizationStrategy.class);
        ExecutionAttributes attributes = new ExecutionAttributes();
        authorizationStrategy.addCredentialsToExecutionAttributes(attributes);
        assertThat(attributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS).accessKeyId()).isEqualTo("akid");
        assertThat(attributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS).secretAccessKey()).isEqualTo("skid");
    }
}
