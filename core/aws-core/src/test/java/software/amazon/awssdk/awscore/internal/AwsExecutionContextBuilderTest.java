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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.http.NoopTestAwsRequest;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.useragent.AdditionalMetadata;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.profiles.ProfileFile;

@RunWith(MockitoJUnitRunner.class)
public class AwsExecutionContextBuilderTest {

    @Mock
    SdkRequest sdkRequest;

    @Mock
    ExecutionInterceptor interceptor;

    @Mock
    IdentityProvider<AwsCredentialsIdentity> defaultCredentialsProvider;

    @Mock
    Signer defaultSigner;

    @Mock
    Signer clientOverrideSigner;

    @Mock
    Map<String, AuthScheme<?>> defaultAuthSchemes;

    @Before
    public void setUp() throws Exception {
        when(sdkRequest.overrideConfiguration()).thenReturn(Optional.empty());
        when(interceptor.modifyRequest(any(), any())).thenReturn(sdkRequest);
        when(defaultCredentialsProvider.resolveIdentity()).thenAnswer(
            invocationOnMock -> CompletableFuture.completedFuture(AwsCredentialsIdentity.create("ak", "sk")));

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

    // pre SRA, AuthorizationStrategy would setup the signer and resolve identity.
    @Test
    public void preSra_signing_ifNoOverrides_assignDefaultSigner_resolveIdentity() {
        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   preSraClientConfiguration().build());

        assertThat(executionContext.signer()).isEqualTo(defaultSigner);
        verify(defaultCredentialsProvider, times(1)).resolveIdentity();
    }

    // This is post SRA case. This is asserting that AuthorizationStrategy is not used.
    @Test
    public void postSra_ifNoOverrides_doesNotResolveIdentity_doesNotAssignSigner() {
        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   testClientConfiguration().build());

        assertThat(executionContext.signer()).isNull();
        verify(defaultCredentialsProvider, times(0)).resolveIdentity();
    }

    @Test
    public void preSra_signing_ifClientOverride_assignClientOverrideSigner_resolveIdentity() {
        Optional overrideConfiguration = Optional.of(AwsRequestOverrideConfiguration.builder()
                                                                                    .signer(clientOverrideSigner)
                                                                                    .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(overrideConfiguration);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   preSraClientConfiguration().build());

        assertThat(executionContext.signer()).isEqualTo(clientOverrideSigner);
        verify(defaultCredentialsProvider, times(1)).resolveIdentity();
    }

    @Test
    public void postSra_signing_ifClientOverride_assignClientOverrideSigner_resolveIdentity() {
        Optional overrideConfiguration = Optional.of(AwsRequestOverrideConfiguration.builder()
                                                                                    .signer(clientOverrideSigner)
                                                                                    .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(overrideConfiguration);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   testClientConfiguration().build());

        assertThat(executionContext.signer()).isEqualTo(clientOverrideSigner);
        verify(defaultCredentialsProvider, times(1)).resolveIdentity();
    }

    @Test
    public void postSra_oldSignerOverriddenThroughExecutionInterceptor_shouldTakePrecedence() {
        SdkRequest request = NoopTestAwsRequest.builder().build();

        Signer noOpSigner = new NoOpSigner();
        ExecutionInterceptor signerExecutionInterceptor = signerOverrideExecutionInterceptor(noOpSigner);
        SdkClientConfiguration configuration = testClientConfiguration(signerExecutionInterceptor).build();
        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(request),
                                                                                   configuration);

        assertThat(executionContext.signer()).isEqualTo(noOpSigner);
        verify(defaultCredentialsProvider, times(1)).resolveIdentity();
    }

    private ExecutionInterceptor signerOverrideExecutionInterceptor(Signer signer) {
        return new ExecutionInterceptor() {
            @Override
            public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                AwsRequest.Builder builder = (AwsRequest.Builder) context.request().toBuilder();
                builder.overrideConfiguration(c -> c.signer(signer)
                                                    .build());

                return builder.build();
            }
        };
    }

    @Test
    public void preSra_authTypeNone_doesNotAssignSigner_doesNotResolveIdentity() {
        SdkClientConfiguration.Builder clientConfig = preSraClientConfiguration();
        clientConfig.option(SdkClientOption.EXECUTION_ATTRIBUTES)
                    // yes, our code would put false instead of true
                    .putAttribute(SdkInternalExecutionAttribute.IS_NONE_AUTH_TYPE_REQUEST, false);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   clientConfig.build());

        assertThat(executionContext.signer()).isNull();
        verify(defaultCredentialsProvider, times(0)).resolveIdentity();
    }

    @Test
    public void postSra_authTypeNone_doesNotAssignSigner_doesNotResolveIdentity() {
        SdkClientConfiguration.Builder clientConfig = noAuthAuthSchemeClientConfiguration();

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   clientConfig.build());

        assertThat(executionContext.signer()).isNull();
        verify(defaultCredentialsProvider, times(0)).resolveIdentity();
    }

    @Test
    public void preSra_authTypeNone_signerClientOverride_doesNotAssignSigner_doesNotResolveIdentity() {
        SdkClientConfiguration.Builder clientConfig = preSraClientConfiguration();
        clientConfig.option(SdkClientOption.EXECUTION_ATTRIBUTES)
                    // yes, our code would put false instead of true
                    .putAttribute(SdkInternalExecutionAttribute.IS_NONE_AUTH_TYPE_REQUEST, false);
        clientConfig.option(SdkAdvancedClientOption.SIGNER, this.clientOverrideSigner)
                    .option(SdkClientOption.SIGNER_OVERRIDDEN, true);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   clientConfig.build());

        assertThat(executionContext.signer()).isNull();
        verify(defaultCredentialsProvider, times(0)).resolveIdentity();
    }

    @Test
    public void postSra_authTypeNone_signerClientOverride_doesNotAssignSigner_doesNotResolveIdentity() {
        SdkClientConfiguration.Builder clientConfig = noAuthAuthSchemeClientConfiguration();
        clientConfig.option(SdkAdvancedClientOption.SIGNER, this.clientOverrideSigner)
                    .option(SdkClientOption.SIGNER_OVERRIDDEN, true);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   clientConfig.build());

        assertThat(executionContext.signer()).isNull();
        verify(defaultCredentialsProvider, times(0)).resolveIdentity();
    }

    @Test
    public void preSra_authTypeNone_signerRequestOverride_doesNotAssignSigner_doesNotResolveIdentity() {
        SdkClientConfiguration.Builder clientConfig = preSraClientConfiguration();
        clientConfig.option(SdkClientOption.EXECUTION_ATTRIBUTES)
                    // yes, our code would put false instead of true
                    .putAttribute(SdkInternalExecutionAttribute.IS_NONE_AUTH_TYPE_REQUEST, false);

        Optional overrideConfiguration = Optional.of(AwsRequestOverrideConfiguration.builder()
                                                                                    .signer(clientOverrideSigner)
                                                                                    .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(overrideConfiguration);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   clientConfig.build());

        assertThat(executionContext.signer()).isNull();
        verify(defaultCredentialsProvider, times(0)).resolveIdentity();
    }

    @Test
    public void postSra_authTypeNone_signerRequestOverride_doesNotAssignSigner_doesNotResolveIdentity() {
        SdkClientConfiguration.Builder clientConfig = noAuthAuthSchemeClientConfiguration();

        Optional overrideConfiguration = Optional.of(AwsRequestOverrideConfiguration.builder()
                                                                                    .signer(clientOverrideSigner)
                                                                                    .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(overrideConfiguration);

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(clientExecutionParams(),
                                                                                   clientConfig.build());

        assertThat(executionContext.signer()).isNull();
        verify(defaultCredentialsProvider, times(0)).resolveIdentity();
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
    public void invokeInterceptorsAndCreateExecutionContext_singleExecutionContext_resolvesEqualChecksumSpecs() {
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

        assertThat(checksumSpecs1).isEqualTo(checksumSpecs2);
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_multipleExecutionContexts_resolvesEqualChecksumSpecs() {
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

        assertThat(checksumSpecs1).isEqualTo(checksumSpecs2);
        assertThat(checksumSpecs2).isEqualTo(checksumSpecs3);
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

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_withoutIdentityProviders_assignsNull() {
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams();

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   testClientConfiguration().build());

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS)).isNull();
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_requestOverrideForIdentityProvider_updatesIdentityProviders() {
        IdentityProvider<? extends AwsCredentialsIdentity> clientCredentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"));
        IdentityProvider<? extends TokenIdentity> clientTokenProvider = StaticTokenProvider.create(() -> "client-token");
        IdentityProviders identityProviders =
            IdentityProviders.builder()
                             .putIdentityProvider(clientCredentialsProvider)
                             .putIdentityProvider(clientTokenProvider)
                             .build();
        SdkClientConfiguration clientConfig = testClientConfiguration()
            .option(SdkClientOption.IDENTITY_PROVIDERS, identityProviders)
            .build();

        IdentityProvider<? extends AwsCredentialsIdentity> requestCredentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        IdentityProvider<? extends TokenIdentity> requestTokenProvider = StaticTokenProvider.create(() -> "request-token");
        Optional overrideConfiguration =
            Optional.of(AwsRequestOverrideConfiguration.builder()
                                                       .credentialsProvider(requestCredentialsProvider)
                                                       .tokenIdentityProvider(requestTokenProvider)
                                                       .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(overrideConfiguration);

        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams();

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams, clientConfig);

        IdentityProviders actualIdentityProviders =
            executionContext.executionAttributes().getAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);

        IdentityProvider<AwsCredentialsIdentity> actualIdentityProvider =
            actualIdentityProviders.identityProvider(AwsCredentialsIdentity.class);

        assertThat(actualIdentityProvider).isSameAs(requestCredentialsProvider);

        IdentityProvider<TokenIdentity> actualTokenProvider =
            actualIdentityProviders.identityProvider(TokenIdentity.class);

        assertThat(actualTokenProvider).isSameAs(requestTokenProvider);
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_withRequestBody_addsUserAgentMetadata() throws IOException {
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams();
        File testFile = File.createTempFile("testFile", UUID.randomUUID().toString());
        testFile.deleteOnExit();
        executionParams.withRequestBody(RequestBody.fromFile(testFile));

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   testClientConfiguration().build());

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA)).isEqualTo(
            Collections.singletonList(AdditionalMetadata.builder().name("rb").value("f").build())
        );
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_withResponseTransformer_addsUserAgentMetadata() throws IOException {
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams();
        File testFile = File.createTempFile("testFile", UUID.randomUUID().toString());
        testFile.deleteOnExit();
        executionParams.withResponseTransformer(ResponseTransformer.toFile(testFile));

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   testClientConfiguration().build());

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA)).isEqualTo(
            Collections.singletonList(AdditionalMetadata.builder().name("rt").value("f").build())
        );
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_withAsyncRequestBody_addsUserAgentMetadata() throws IOException {
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams();
        File testFile = File.createTempFile("testFile", UUID.randomUUID().toString());
        testFile.deleteOnExit();
        executionParams.withAsyncRequestBody(AsyncRequestBody.fromFile(testFile));

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   testClientConfiguration().build());

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA)).isEqualTo(
            Collections.singletonList(AdditionalMetadata.builder().name("rb").value("f").build())
        );
    }

    @Test
    public void invokeInterceptorsAndCreateExecutionContext_withAsyncResponseTransformer_addsUserAgentMetadata() throws IOException {
        ClientExecutionParams<SdkRequest, SdkResponse> executionParams = clientExecutionParams();
        File testFile = File.createTempFile("testFile", UUID.randomUUID().toString());
        testFile.deleteOnExit();
        executionParams.withAsyncResponseTransformer(AsyncResponseTransformer.toFile(testFile));

        ExecutionContext executionContext =
            AwsExecutionContextBuilder.invokeInterceptorsAndCreateExecutionContext(executionParams,
                                                                                   testClientConfiguration().build());

        ExecutionAttributes executionAttributes = executionContext.executionAttributes();
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA)).isEqualTo(
            Collections.singletonList(AdditionalMetadata.builder().name("rt").value("f").build())
        );
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return clientExecutionParams(sdkRequest);
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams(SdkRequest sdkRequest) {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(sdkRequest)
            .withFullDuplex(false)
            .withOperationName("TestOperation");
    }

    private SdkClientConfiguration.Builder testClientConfiguration() {
        return testClientConfiguration(interceptor);
    }

    private SdkClientConfiguration.Builder testClientConfiguration(ExecutionInterceptor... executionInterceptors) {
        // In real SRA case, SelectedAuthScheme is setup as an executionAttribute by {Service}AuthSchemeInterceptor that is setup
        // in EXECUTION_INTERCEPTORS. But, faking it here for unit test, by already setting SELECTED_AUTH_SCHEME into the
        // executionAttributes.
        SelectedAuthScheme<?> selectedAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(AwsCredentialsIdentity.create("ak", "sk")),
            mock(HttpSigner.class),
            AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build()
        );
        ExecutionAttributes executionAttributes =
            ExecutionAttributes.builder()
                               .put(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme)
                               .build();

        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, Arrays.asList(executionInterceptors))
                                     .option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, defaultCredentialsProvider)
                                     .option(SdkClientOption.AUTH_SCHEMES, defaultAuthSchemes)
                                     .option(SdkClientOption.EXECUTION_ATTRIBUTES, executionAttributes);
    }

    private SdkClientConfiguration.Builder noAuthAuthSchemeClientConfiguration() {
        SdkClientConfiguration.Builder clientConfig = testClientConfiguration();
        SelectedAuthScheme<?> selectedNoAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(AwsCredentialsIdentity.create("ak", "sk")),
            mock(HttpSigner.class),
            AuthSchemeOption.builder().schemeId(NoAuthAuthScheme.SCHEME_ID).build()
        );
        clientConfig.option(SdkClientOption.EXECUTION_ATTRIBUTES)
                    .putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedNoAuthScheme);
        return clientConfig;
    }

    private SdkClientConfiguration.Builder preSraClientConfiguration() {
        SdkClientConfiguration.Builder clientConfiguration = testClientConfiguration();
        clientConfiguration.option(SdkClientOption.EXECUTION_ATTRIBUTES)
                           .putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
        return clientConfiguration.option(SdkClientOption.AUTH_SCHEMES, null)
                                  .option(SdkAdvancedClientOption.SIGNER, this.defaultSigner);
    }
}
