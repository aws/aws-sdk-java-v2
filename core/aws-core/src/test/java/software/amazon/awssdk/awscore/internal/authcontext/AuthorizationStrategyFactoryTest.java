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

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.metrics.MetricCollector;

public class AuthorizationStrategyFactoryTest {

    @Mock SdkRequest sdkRequest;
    @Mock MetricCollector metricCollector;

    @Test
    public void credentialTypeBearerToken_returnsTokenStrategy() {
        AuthorizationStrategyFactory factory = new AuthorizationStrategyFactory(sdkRequest, metricCollector,
                                                                                SdkClientConfiguration.builder().build());
        AuthorizationStrategy authorizationStrategy = factory.strategyFor(CredentialType.TOKEN);
        assertThat(authorizationStrategy).isExactlyInstanceOf(TokenAuthorizationStrategy.class);
    }

    @Test
    public void credentialTypeAwsCredentials_returnsCredentialsStrategy() {
        AuthorizationStrategyFactory factory = new AuthorizationStrategyFactory(sdkRequest, metricCollector,
                                                                                SdkClientConfiguration.builder().build());
        AuthorizationStrategy authorizationStrategy = factory.strategyFor(CredentialType.of("AWS"));
        assertThat(authorizationStrategy).isExactlyInstanceOf(AwsCredentialsAuthorizationStrategy.class);
    }

}
