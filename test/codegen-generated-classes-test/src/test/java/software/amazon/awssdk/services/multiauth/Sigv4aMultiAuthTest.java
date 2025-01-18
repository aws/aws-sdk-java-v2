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

package software.amazon.awssdk.services.multiauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.multiauth.auth.scheme.MultiauthAuthSchemeParams;
import software.amazon.awssdk.services.multiauth.auth.scheme.MultiauthAuthSchemeProvider;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Unit tests for the Sigv4a multi-auth functionality.
 */
class Sigv4aMultiAuthTest {

    private EnvironmentVariableHelper environmentVariableHelper;
    private SdkHttpClient mockHttpClient;
    private MultiauthAuthSchemeProvider multiauthAuthSchemeProvider;

    @BeforeEach
    void setUp() {
        environmentVariableHelper = new EnvironmentVariableHelper();
        multiauthAuthSchemeProvider = mock(MultiauthAuthSchemeProvider.class);

        mockHttpClient = mock(SdkHttpClient.class);
        when(mockHttpClient.clientName()).thenReturn("MockHttpClient");
        when(mockHttpClient.prepareRequest(any())).thenThrow(new RuntimeException("expected exception"));

        List<AuthSchemeOption> authSchemeOptions = Collections.singletonList(
            AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build()
        );
        when(multiauthAuthSchemeProvider.resolveAuthScheme(any(MultiauthAuthSchemeParams.class)))
            .thenReturn(authSchemeOptions);
    }

    @AfterEach
    void tearDown() {
        environmentVariableHelper.reset();
    }

    @Test
    void requestHasRegionSetParamsUpdatedToRegion() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, "us-west-2,us-west-1");

        MultiauthClient multiauthClient = MultiauthClient.builder()
                                                         .httpClient(mockHttpClient)
                                                         .authSchemeProvider(multiauthAuthSchemeProvider)
                                                         .region(Region.US_WEST_2)
                                                         .build();

        assertThatThrownBy(() -> multiauthClient.sigv4aOperation(r -> r.stringMember("")))
            .hasMessageContaining("expected exception");

        ArgumentCaptor<MultiauthAuthSchemeParams> paramsCaptor =
            ArgumentCaptor.forClass(MultiauthAuthSchemeParams.class);
        verify(multiauthAuthSchemeProvider).resolveAuthScheme(paramsCaptor.capture());

        MultiauthAuthSchemeParams resolvedAuthSchemeParams = paramsCaptor.getValue();
        assertThat(resolvedAuthSchemeParams.regionSet())
            .isEqualTo(RegionSet.create(Arrays.asList("us-west-2", "us-west-1")));
        assertThat(resolvedAuthSchemeParams.apiType()).isEqualTo("NoAuthProperties");
    }

    @Test
    void authSchemeParamsPassedAsNullIfClientIsNotConfiguredWithRegionSet() {
        MultiauthClient multiauthClient = MultiauthClient.builder()
                                                         .httpClient(mockHttpClient)
                                                         .authSchemeProvider(multiauthAuthSchemeProvider)
                                                         .region(Region.US_WEST_2)
                                                         .build();

        assertThatThrownBy(() -> multiauthClient.sigv4aOperation(r -> r.stringMember("")))
            .hasMessageContaining("expected exception");

        ArgumentCaptor<MultiauthAuthSchemeParams> paramsCaptor =
            ArgumentCaptor.forClass(MultiauthAuthSchemeParams.class);
        verify(multiauthAuthSchemeProvider).resolveAuthScheme(paramsCaptor.capture());

        MultiauthAuthSchemeParams resolvedAuthSchemeParams = paramsCaptor.getValue();
        assertThat(resolvedAuthSchemeParams.regionSet()).isNull();
        assertThat(resolvedAuthSchemeParams.apiType()).isEqualTo("NoAuthProperties");
    }

    @Test
    void errorWhenSigv4aDoesNotHasFallbackSigv4() {
        MultiauthClient multiauthClient = MultiauthClient.builder()
                                                         .httpClient(mockHttpClient)
                                                         .region(Region.US_WEST_2)
                                                         .build();

        assertThatThrownBy(() -> multiauthClient.sigv4aOperation(r -> r.stringMember("")))
            .hasMessageContaining("You must add a dependency on the 'software.amazon.awssdk:http-auth-aws-crt' "
                                  + "module to enable the CRT-V4a signing feature");

    }

    @Test
    void fallBackToSigv4WhenSigv4aIsNotAvailable() {
        MultiauthClient multiauthClient = MultiauthClient.builder()
                                                         .httpClient(mockHttpClient)
                                                         .region(Region.US_WEST_2)
                                                         .build();

        assertThatThrownBy(() -> multiauthClient.sigv4AndSigv4aOperation(r -> r.stringMember("")))
            .hasMessageContaining("expected exception");

        ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        verify(mockHttpClient).prepareRequest(httpRequestCaptor.capture());
        SdkHttpRequest request = httpRequestCaptor.getAllValues().get(0).httpRequest();
        assertThat(request.firstMatchingHeader("Authorization")).isPresent();
    }


    @Test
    void authSchemesParamsUpdatedWithStaticContextAndDefaultEndpointParams() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, "us-west-2,us-west-1");

        MultiauthClient multiauthClient = MultiauthClient.builder()
                                                         .httpClient(mockHttpClient)
                                                         .authSchemeProvider(multiauthAuthSchemeProvider)
                                                         .region(Region.EU_CENTRAL_1)
                                                         .build();

        assertThatThrownBy(() -> multiauthClient.operationWithOnlyRegionEndpointParams(r -> r.stringMember("")))
            .hasMessageContaining("expected exception");

        ArgumentCaptor<MultiauthAuthSchemeParams> paramsCaptor =
            ArgumentCaptor.forClass(MultiauthAuthSchemeParams.class);
        verify(multiauthAuthSchemeProvider).resolveAuthScheme(paramsCaptor.capture());

        MultiauthAuthSchemeParams resolvedAuthSchemeParams = paramsCaptor.getValue();
        assertThat(resolvedAuthSchemeParams.regionSet())
            .isEqualTo(RegionSet.create(Arrays.asList("us-west-2", "us-west-1")));
        assertThat(resolvedAuthSchemeParams.apiType())
            .isEqualTo("RegionDefinedInRules");
        assertThat(resolvedAuthSchemeParams.operation())
            .isEqualTo("operationWithOnlyRegionEndpointParams");
        assertThat(resolvedAuthSchemeParams.region()).isEqualTo(Region.EU_CENTRAL_1);
        assertThat(resolvedAuthSchemeParams.useFips()).isFalse();
        assertThat(resolvedAuthSchemeParams.useDualStack()).isFalse();
    }
}
