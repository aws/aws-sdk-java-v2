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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;

@RunWith(MockitoJUnitRunner.class)
public class AwsCredentialsAuthorizationStrategyTest {

    @Mock SdkRequest sdkRequest;
    @Mock Signer defaultSigner;
    @Mock Signer requestOverrideSigner;
    @Mock AwsCredentialsProvider credentialsProvider;
    @Mock AwsCredentials credentials;
    @Mock MetricCollector metricCollector;

    @Before
    public void setUp() throws Exception {
        when(sdkRequest.overrideConfiguration()).thenReturn(Optional.empty());
        when(credentialsProvider.resolveCredentials()).thenReturn(credentials);
    }

    @Test
    public void noOverrideSigner_returnsDefaultSigner() {
        AwsCredentialsAuthorizationStrategy authorizationContext =
            AwsCredentialsAuthorizationStrategy.builder()
                                               .request(sdkRequest)
                                               .defaultSigner(defaultSigner)
                                               .defaultCredentialsProvider(credentialsProvider)
                                               .metricCollector(metricCollector)
                                               .build();
        Signer signer = authorizationContext.resolveSigner();
        assertThat(signer).isEqualTo(defaultSigner);
    }

    @Test
    public void overrideSigner_returnsOverrideSigner() {
        Optional cfg = Optional.of(requestOverrideConfiguration());
        when(sdkRequest.overrideConfiguration()).thenReturn(cfg);
        AwsCredentialsAuthorizationStrategy authorizationContext =
            AwsCredentialsAuthorizationStrategy.builder()
                                               .request(sdkRequest)
                                               .defaultSigner(defaultSigner)
                                               .defaultCredentialsProvider(credentialsProvider)
                                               .metricCollector(metricCollector)
                                               .build();
        Signer signer = authorizationContext.resolveSigner();
        assertThat(signer).isEqualTo(requestOverrideSigner);
    }

    @Test
    public void noDefaultSignerNoOverride_returnsNull() {
        AwsCredentialsAuthorizationStrategy authorizationContext =
            AwsCredentialsAuthorizationStrategy.builder()
                                               .request(sdkRequest)
                                               .defaultSigner(null)
                                               .defaultCredentialsProvider(credentialsProvider)
                                               .metricCollector(metricCollector)
                                               .build();
        Signer signer = authorizationContext.resolveSigner();
        assertThat(signer).isNull();
    }

    @Test
    public void providerExists_credentialsAddedToExecutionAttributes() {
        AwsCredentialsAuthorizationStrategy authorizationContext =
            AwsCredentialsAuthorizationStrategy.builder()
                                               .request(sdkRequest)
                                               .defaultSigner(defaultSigner)
                                               .defaultCredentialsProvider(credentialsProvider)
                                               .metricCollector(metricCollector)
                                               .build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        authorizationContext.addCredentialsToExecutionAttributes(executionAttributes);
        assertThat(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS)).isEqualTo(credentials);
    }

    @Test
    public void noProvider_throwsError() {
        AwsCredentialsAuthorizationStrategy authorizationContext =
            AwsCredentialsAuthorizationStrategy.builder()
                                               .request(sdkRequest)
                                               .defaultSigner(defaultSigner)
                                               .defaultCredentialsProvider(null)
                                               .metricCollector(metricCollector)
                                               .build();

        assertThatThrownBy(() -> authorizationContext.addCredentialsToExecutionAttributes(new ExecutionAttributes()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("No credentials provider exists to resolve credentials from.");
    }

    private AwsRequestOverrideConfiguration requestOverrideConfiguration() {
        return AwsRequestOverrideConfiguration.builder()
                                              .signer(requestOverrideSigner)
                                              .build();
    }
}
