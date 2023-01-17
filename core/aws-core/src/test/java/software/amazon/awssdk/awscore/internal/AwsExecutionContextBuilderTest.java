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

package software.amazon.awssdk.awscore.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.profiles.ProfileFile;

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
        AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                               testClientConfiguration().build());
        verify(interceptor, times(1)).beforeExecution(any(), any());
        verify(interceptor, times(1)).modifyRequest(any(), any());
    }

    @Test
    public void verifyCoreExecutionAttributesTakePrecedence() {
        ExecutionAttributes requestOverrides = ExecutionAttributes.builder()
                                                                  .put(SdkExecutionAttribute.SERVICE_NAME, "RequestOverrideServiceName")
                                                                  .build();
        Optional requestOverrideConfiguration = Optional.of(AwsRequestOverrideConfiguration.builder()
                                                                                           .executionAttributes(requestOverrides)
                                                                                           .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(requestOverrideConfiguration);

        ExecutionAttributes clientConfigOverrides = ExecutionAttributes.builder()
                                                                       .put(SdkExecutionAttribute.SERVICE_NAME, "ClientConfigServiceName")
                                                                       .build();
        SdkClientConfiguration testClientConfiguration = testClientConfiguration()
            .option(SdkClientOption.SERVICE_NAME, "DoNotOverrideService")
            .option(SdkClientOption.EXECUTION_ATTRIBUTES, clientConfigOverrides)
            .build();

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(), testClientConfiguration);

        assertThat(executionContext.executionAttributes().getAttribute(SdkExecutionAttribute.SERVICE_NAME)).isEqualTo("DoNotOverrideService");
    }

    @Test
    public void signing_ifNoOverrides_assignDefaultSigner() {
        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   testClientConfiguration().build());

        assertThat(executionContext.signer()).isEqualTo(defaultSigner);
    }

    @Test
    public void signing_ifClientOverride_assignClientOverrideSigner() {
        Optional overrideConfiguration = Optional.of(AwsRequestOverrideConfiguration.builder()
                                                                                    .signer(clientOverrideSigner)
                                                                                    .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(overrideConfiguration);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   testClientConfiguration().build());

        assertThat(executionContext.signer()).isEqualTo(clientOverrideSigner);
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_noHttpChecksumTrait_resolvesChecksumSpecs() {
        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   testClientConfiguration().build());

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();
        Optional<ChecksumSpecs> checksumSpecs1 = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes);
        Optional<ChecksumSpecs> checksumSpecs2 = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes);

        assertThat(checksumSpecs1).isNotPresent();
        assertThat(checksumSpecs2).isNotPresent();
        assertThat(checksumSpecs1).isSameAs(checksumSpecs2);
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_singleExecutionContext_resolvesChecksumSpecsOnce() {
        HttpChecksum httpCrc32Checksum =
            HttpChecksum.builder().requestAlgorithm("crc32").isRequestStreaming(true).build();
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams()
            .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM, httpCrc32Checksum);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   testClientConfiguration().build());

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();
        ChecksumSpecs checksumSpecs1 = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).get();
        ChecksumSpecs checksumSpecs2 = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).get();

        assertThat(checksumSpecs1).isSameAs(checksumSpecs2);
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_multipleExecutionContexts_resolvesChecksumSpecsOncePerContext() {
        HttpChecksum httpCrc32Checksum = HttpChecksum.builder().requestAlgorithm("crc32").isRequestStreaming(true).build();
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams()
            .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM, httpCrc32Checksum);
        SdkClientConfiguration clientConfig = testClientConfiguration().build();

        ExecutionContext executionContext1 =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   clientConfig);
        ExecutionAttributes executionAttributes1 = executionContext1.executionAttributes();
        ChecksumSpecs checksumSpecs1 = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes1).get();

        ExecutionContext executionContext2 =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   clientConfig);
        ExecutionAttributes executionAttributes2 = executionContext2.executionAttributes();
        ChecksumSpecs checksumSpecs2 = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes2).get();
        ChecksumSpecs checksumSpecs3 = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes2).get();

        assertThat(checksumSpecs1).isNotSameAs(checksumSpecs2);
        assertThat(checksumSpecs2).isSameAs(checksumSpecs3);
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_profileFileSupplier_storesValueInExecutionAttributes() {
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams();
        Supplier<ProfileFile> profileFileSupplier = () -> null;
        SdkClientConfiguration clientConfig = testClientConfiguration()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, profileFileSupplier)
            .build();

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams, clientConfig);

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();

        assertThat(profileFileSupplier).isSameAs(executionAttributes.getAttribute(SdkExecutionAttribute.PROFILE_FILE_SUPPLIER));
    }
    
    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(sdkRequest)
            .withFullDuplex(false)
            .withOperationName("TestOperation");
    }

    private SdkClientConfiguration.Builder testClientConfiguration() {
        List<ExecutionInterceptor> interceptorList = Collections.singletonList(interceptor);
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, new ArrayList<>())
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptorList)
                                     .option(AwsClientOption.CREDENTIALS_PROVIDER, DefaultCredentialsProvider.create())
                                     .option(SdkAdvancedClientOption.SIGNER, this.defaultSigner);
    }
}
