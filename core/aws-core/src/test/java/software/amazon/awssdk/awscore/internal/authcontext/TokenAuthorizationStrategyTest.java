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
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.signer.SdkTokenExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.internal.token.TestToken;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;

@RunWith(MockitoJUnitRunner.class)
public class TokenAuthorizationStrategyTest {

    private static final String TOKEN_VALUE = "token_value";
    private SdkToken token;

    @Mock SdkRequest sdkRequest;
    @Mock Signer defaultSigner;
    @Mock Signer requestOverrideSigner;
    @Mock SdkTokenProvider tokenProvider;
    @Mock MetricCollector metricCollector;

    @Before
    public void setUp() throws Exception {
        token = TestToken.builder().token(TOKEN_VALUE).build();
        when(sdkRequest.overrideConfiguration()).thenReturn(Optional.empty());
        when(tokenProvider.resolveToken()).thenReturn(token);
    }

    @Test
    public void noOverrideSigner_returnsDefaultSigner() {
        TokenAuthorizationStrategy authorizationContext = TokenAuthorizationStrategy.builder()
                                                                                    .request(sdkRequest)
                                                                                    .defaultSigner(defaultSigner)
                                                                                    .defaultTokenProvider(tokenProvider)
                                                                                    .metricCollector(metricCollector)
                                                                                    .build();
        Signer signer = authorizationContext.resolveSigner();
        assertThat(signer).isEqualTo(defaultSigner);
    }

    @Test
    public void overrideSigner_returnsOverrideSigner() {
        Optional cfg = Optional.of(requestOverrideConfiguration());
        when(sdkRequest.overrideConfiguration()).thenReturn(cfg);
        TokenAuthorizationStrategy authorizationContext = TokenAuthorizationStrategy.builder()
                                                                                    .request(sdkRequest)
                                                                                    .defaultSigner(defaultSigner)
                                                                                    .defaultTokenProvider(tokenProvider)
                                                                                    .metricCollector(metricCollector)
                                                                                    .build();
        Signer signer = authorizationContext.resolveSigner();
        assertThat(signer).isEqualTo(requestOverrideSigner);
    }

    @Test
    public void noDefaultSignerNoOverride_returnsNull() {
        TokenAuthorizationStrategy authorizationContext = TokenAuthorizationStrategy.builder()
                                                                                    .request(sdkRequest)
                                                                                    .defaultSigner(null)
                                                                                    .defaultTokenProvider(tokenProvider)
                                                                                    .metricCollector(metricCollector)
                                                                                    .build();
        Signer signer = authorizationContext.resolveSigner();
        assertThat(signer).isNull();
    }

    @Test
    public void providerExists_credentialsAddedToExecutionAttributes() {
        TokenAuthorizationStrategy authorizationContext = TokenAuthorizationStrategy.builder()
                                                                                    .request(sdkRequest)
                                                                                    .defaultSigner(defaultSigner)
                                                                                    .defaultTokenProvider(tokenProvider)
                                                                                    .metricCollector(metricCollector)
                                                                                    .build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        authorizationContext.addCredentialsToExecutionAttributes(executionAttributes);
        assertThat(executionAttributes.getAttribute(SdkTokenExecutionAttribute.SDK_TOKEN)).isEqualTo(token);
    }

    @Test
    public void noProvider_throwsError() {
        TokenAuthorizationStrategy authorizationContext = TokenAuthorizationStrategy.builder()
                                                                                    .request(sdkRequest)
                                                                                    .defaultSigner(defaultSigner)
                                                                                    .defaultTokenProvider(null)
                                                                                    .metricCollector(metricCollector)
                                                                                    .build();

        assertThatThrownBy(() -> authorizationContext.addCredentialsToExecutionAttributes(new ExecutionAttributes()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("No token provider exists to resolve a token from.");
    }

    private AwsRequestOverrideConfiguration requestOverrideConfiguration() {
        return AwsRequestOverrideConfiguration.builder()
                                              .signer(requestOverrideSigner)
                                              .build();
    }
}
