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

package software.amazon.awssdk.services.accountidendpointrouting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;

class AccountIdParameterTests {

    private RestJsonEndpointProvidersEndpointProvider mockEndpointProvider;
    private SdkHttpClient mockHttpClient;

    @BeforeEach
    void setup() {
        mockEndpointProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
        mockHttpClient = mock(SdkHttpClient.class);
        when(mockHttpClient.clientName()).thenReturn("MockHttpClient");
        when(mockHttpClient.prepareRequest(any())).thenThrow(new RuntimeException("boom"));
        when(mockEndpointProvider.resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder()
                                                                  .url(URI.create("https://my-service.com"))
                                                                  .build()));
    }

    @Test
    void accountId_isResolvedToValue_whenEndpointResolverIsCalled_NOTWORKING() {
        RestJsonEndpointProvidersClient client =
            RestJsonEndpointProvidersClient.builder()
                                           .endpointProvider(mockEndpointProvider)
                                           .httpClient(mockHttpClient)
                                           .region(Region.US_WEST_2)
                                           .credentialsProvider(credentialsWithAccountId())
                                           .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {})).hasMessageContaining("boom");

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);
        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams resolvedEndpointParams = paramsCaptor.getValue();
        assertThat(resolvedEndpointParams.accountId()).isNotNull().isEqualTo("accountId");
        assertThat(resolvedEndpointParams.accountEndpointMode()).isNotNull().isEqualTo(AccountIdEndpointMode.PREFERRED.name().toLowerCase());
    }

    @ParameterizedTest
    @EnumSource(AccountIdEndpointMode.class)
    void accountId_isResolvedToValue_whenEndpointResolverIsCalled(AccountIdEndpointMode accountIdEndpointMode) {
        RestJsonEndpointProvidersClient client =
            RestJsonEndpointProvidersClient.builder()
                                           .endpointProvider(mockEndpointProvider)
                                           .httpClient(mockHttpClient)
                                           .region(Region.US_WEST_2)
                                           .credentialsProvider(credentialsWithAccountId())
                                           .accountIdEndpointMode(accountIdEndpointMode)
                                           .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {})).hasMessageContaining("boom");

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);
        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams resolvedEndpointParams = paramsCaptor.getValue();
        assertThat(resolvedEndpointParams.accountId()).isNotNull().isEqualTo("accountId");
        assertThat(resolvedEndpointParams.accountEndpointMode()).isNotNull().isEqualTo(accountIdEndpointMode.name().toLowerCase());
    }

    private static AwsCredentialsProvider credentialsWithAccountId() {
        return () -> AwsSessionCredentials.builder()
                                          .accessKeyId("akid")
                                          .secretAccessKey("skid")
                                          .sessionToken("token")
                                          .accountId("accountId")
                                          .build();
    }
}
