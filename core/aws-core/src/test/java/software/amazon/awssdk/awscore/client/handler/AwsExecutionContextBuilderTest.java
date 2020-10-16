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

package software.amazon.awssdk.awscore.client.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.AwsExecutionContextBuilder;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;

@RunWith(MockitoJUnitRunner.class)
public class AwsExecutionContextBuilderTest {

    @Mock
    SdkRequest sdkRequest;

    @Mock
    ExecutionInterceptor interceptor;

    @Mock
    Signer defaultSigner, clientOverrideSigner;

    @Before
    public void setUp() throws Exception {
        when(sdkRequest.overrideConfiguration()).thenReturn(Optional.empty());
        when(interceptor.modifyRequest(any(), any())).thenReturn(sdkRequest);
    }

    @Test
    public void verifyInterceptors() {
        AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(), testClientConfiguration());
        verify(interceptor, times(1)).beforeExecution(any(), any());
        verify(interceptor, times(1)).modifyRequest(any(), any());
    }

    @Test
    public void signing_ifNoOverrides_assignDefaultSigner() {
        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(), testClientConfiguration());

        assertThat(executionContext.signer()).isEqualTo(defaultSigner);
    }

    @Test
    public void signing_ifClientOverride_assignClientOverrideSigner() {
        Optional overrideConfiguration = Optional.of(AwsRequestOverrideConfiguration.builder()
                                                                                    .signer(clientOverrideSigner)
                                                                                    .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(overrideConfiguration);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(), testClientConfiguration());

        assertThat(executionContext.signer()).isEqualTo(clientOverrideSigner);
    }
    
    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(sdkRequest)
            .withFullDuplex(false)
            .withOperationName("TestOperation");
    }

    private SdkClientConfiguration testClientConfiguration() {
        List<ExecutionInterceptor> interceptorList = Collections.singletonList(interceptor);
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, new ArrayList<>())
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptorList)
                                     .option(AwsClientOption.CREDENTIALS_PROVIDER, DefaultCredentialsProvider.create())
                                     .option(SdkAdvancedClientOption.SIGNER, this.defaultSigner)
                                     .build();
    }
}
