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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.TIME_OFFSET;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;
import static software.amazon.awssdk.core.metrics.CoreMetric.SIGNING_DURATION;

import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.metrics.MetricCollector;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class SigningStageTest {

    private static final SignerProperty<String> SIGNER_PROPERTY = SignerProperty.create(String.class, "key");

    @Mock
    private Identity identity;

    @Mock
    private HttpSigner<Identity> signer;

    @Mock
    private Signer oldSigner;

    @Mock
    MetricCollector metricCollector;

    @Captor
    private ArgumentCaptor<SyncSignRequest<? extends Identity>> signRequestCaptor;

    private HttpClientDependencies httpClientDependencies;
    private SigningStage stage;

    @Before
    public void setup() {
        httpClientDependencies = HttpClientDependencies.builder()
                                                       .clientConfiguration(SdkClientConfiguration.builder().build())
                                                       .build();
        // when tests update TimeOffset to non-zero value, it also sets SdkGlobalTime.setGlobalTimeOffset,
        // so explicitly setting this to default value before each test.
        httpClientDependencies.updateTimeOffset(0);
        stage = new SigningStage(httpClientDependencies);
    }

    @Test
    public void execute_sraSelectsAuthScheme_signs() throws Exception {
        // Set up a scheme with a signer property
        SelectedAuthScheme<Identity> selectedAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(identity),
            signer,
            AuthSchemeOption.builder()
                            .schemeId("my.auth#myAuth")
                            .putSignerProperty(SIGNER_PROPERTY, "value")
                            .build());
        RequestExecutionContext context = createContext(selectedAuthScheme);

        SdkHttpRequest signedRequest = ValidSdkObjects.sdkHttpFullRequest().build();
        when(signer.sign(Mockito.<SyncSignRequest<? extends Identity>>any()))
            .thenReturn(SyncSignedRequest.builder()
                                         .request(signedRequest)
                                         .build());

        SdkHttpFullRequest request = ValidSdkObjects.sdkHttpFullRequest().build();
        SdkHttpFullRequest result = stage.execute(request, context);

        assertThat(result).isSameAs(signedRequest);
        // assert that interceptor context is updated with result
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(result);

        // assert that the input to the signer is as expected, including that signer properties are set
        verify(signer).sign(signRequestCaptor.capture());
        SyncSignRequest<? extends Identity> signRequest = signRequestCaptor.getValue();
        assertThat(signRequest.identity()).isSameAs(identity);
        assertThat(signRequest.request()).isSameAs(request);
        assertThat(signRequest.property(SIGNER_PROPERTY)).isEqualTo("value");

        // assert that metrics are collected
        verify(metricCollector).reportMetric(eq(SIGNING_DURATION), any());

        verifyNoInteractions(oldSigner);
    }

    @Test
    public void execute_sraSelectsNoAuthAuthScheme_skipsSigning() throws Exception {
        // Set up a scheme with smithy.api#noAuth
        SelectedAuthScheme<Identity> selectedAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(identity),
            signer,
            AuthSchemeOption.builder()
                            .schemeId("smithy.api#noAuth")
                            .build());
        RequestExecutionContext context = createContext(selectedAuthScheme);

        SdkHttpFullRequest request = ValidSdkObjects.sdkHttpFullRequest().build();
        SdkHttpFullRequest result = stage.execute(request, context);

        assertThat(result).isSameAs(request);
        // assert that interceptor context is updated with result, which is same as request.
        // To ensure this asserts the logic in the SigningStage to update the InterceptorContext before the signing logic,
        // the request is not set in the InterceptorContext in createContext()
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(request);

        verifyNoInteractions(signer);
        verifyNoInteractions(metricCollector);
    }

    @Test
    public void execute_preSra_signer_signs() throws Exception {
        RequestExecutionContext context = createContext(null, oldSigner);

        SdkHttpFullRequest request = ValidSdkObjects.sdkHttpFullRequest().build();

        SdkHttpFullRequest signedRequest = ValidSdkObjects.sdkHttpFullRequest().build();
        // Creating a copy because original executionAttributes may be directly mutated by SigningStage, e.g., timeOffset
        when(oldSigner.sign(request, context.executionAttributes().copy().putAttribute(TIME_OFFSET, 0))).thenReturn(signedRequest);

        SdkHttpFullRequest result = stage.execute(request, context);

        assertThat(result).isSameAs(signedRequest);
        // assert that interceptor context is updated with result
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(result);

        // assert that metrics are collected
        verify(metricCollector).reportMetric(eq(SIGNING_DURATION), any());

        verifyNoInteractions(signer);
    }

    @Test
    public void execute_preSra_nullSigner_skipsSigning() throws Exception {
        Signer oldSigner = null;
        RequestExecutionContext context = createContext(null, oldSigner);

        SdkHttpFullRequest request = ValidSdkObjects.sdkHttpFullRequest().build();
        SdkHttpFullRequest result = stage.execute(request, context);

        assertThat(result).isSameAs(request);
        // assert that interceptor context is updated with result, which is same as request.
        // To ensure this asserts the logic in the SigningStage to update the InterceptorContext before the signing logic,
        // the request is not set in the InterceptorContext in createContext()
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(request);

        verifyNoInteractions(metricCollector);

        verifyNoInteractions(signer);
    }

    @Test
    public void execute_preSra_signerWithTimeOffset_usesOffset() throws Exception {
        httpClientDependencies.updateTimeOffset(100);

        RequestExecutionContext context = createContext(null, oldSigner);

        SdkHttpFullRequest request = ValidSdkObjects.sdkHttpFullRequest().build();

        SdkHttpFullRequest signedRequest = ValidSdkObjects.sdkHttpFullRequest().build();
        // Creating a copy because original executionAttributes may be directly mutated by SigningStage, e.g., timeOffset
        when(oldSigner.sign(request, context.executionAttributes().copy().putAttribute(TIME_OFFSET, 100))).thenReturn(signedRequest);

        SdkHttpFullRequest result = stage.execute(request, context);

        assertThat(result).isSameAs(signedRequest);
        // assert that interceptor context is updated with result
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(result);

        // assert that metrics are collected
        verify(metricCollector).reportMetric(eq(SIGNING_DURATION), any());

        verifyNoInteractions(signer);
    }


    private RequestExecutionContext createContext(SelectedAuthScheme<Identity> selectedAuthScheme) {
        return createContext(selectedAuthScheme, oldSigner);
    }

    private RequestExecutionContext createContext(SelectedAuthScheme<Identity> selectedAuthScheme, Signer oldSigner) {
        SdkRequest sdkRequest = ValidSdkObjects.sdkRequest();
        InterceptorContext interceptorContext =
            InterceptorContext.builder()
                              .request(sdkRequest)
                              // Normally, this would be set, but there is logic to update the InterceptorContext before and
                              // after signing, so keeping it not set here, so that logic can be asserted in tests.
                              // .httpRequest(request)
                              .build();

        ExecutionAttributes executionAttributes = ExecutionAttributes.builder()
                                                                     .put(SELECTED_AUTH_SCHEME, selectedAuthScheme)
                                                                     .build();

        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(executionAttributes)
                                                            .interceptorContext(interceptorContext)
                                                            .signer(oldSigner)
                                                            .build();

        RequestExecutionContext context = RequestExecutionContext.builder()
                                                                 .executionContext(executionContext)
                                                                 .originalRequest(sdkRequest)
                                                                 .build();
        context.attemptMetricCollector(metricCollector);
        return context;
    }
}
