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

package software.amazon.awssdk.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.ProtocolRestJsonAuthSchemeProvider;

/**
 * Tests verifying auth scheme options cache behavior:
 * - Custom client-level providers bypass the cache (instanceof guard)
 * - Per-request overrides bypass the cache
 * - Default provider with caching doesn't break correctness
 */
class AuthSchemeCacheTest {

    @Mock
    private SdkHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockHttpClient.clientName()).thenReturn("MockHttpClient");
        when(mockHttpClient.prepareRequest(any()))
            .thenThrow(new RuntimeException("stop"));
    }

    @Test
    void customClientProvider_isNotCached() {
        AtomicInteger resolveCount = new AtomicInteger(0);

        ProtocolRestJsonAuthSchemeProvider customProvider = params -> {
            resolveCount.incrementAndGet();
            return Collections.singletonList(
                AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build());
        };

        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .authSchemeProvider(customProvider)
            .build();

        // Custom provider is invoked on every call — instanceof guard prevents caching
        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(resolveCount.get()).isEqualTo(1);

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(resolveCount.get()).isEqualTo(2);

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(resolveCount.get()).isEqualTo(3);

        client.close();
    }

    @Test
    void perRequestOverride_usesOverrideProvider() {
        AtomicInteger requestProviderCount = new AtomicInteger(0);

        ProtocolRestJsonAuthSchemeProvider requestProvider = params -> {
            requestProviderCount.incrementAndGet();
            return Collections.singletonList(
                AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build());
        };

        // Use default provider on the client (cache will be active)
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .build();

        // First call with default provider — populates cache
        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(requestProviderCount.get()).isEqualTo(0);

        // Per-request override — must use the override, not the cache
        assertThatThrownBy(() -> client.allTypes(r -> r.overrideConfiguration(
            c -> c.authSchemeProvider(requestProvider)
        ))).hasMessageContaining("stop");
        assertThat(requestProviderCount.get()).isEqualTo(1);

        // Without override — back to cache (request provider not called again)
        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(requestProviderCount.get()).isEqualTo(1);

        client.close();
    }

    @Test
    void defaultProvider_multipleOperationsSucceed() {
        // Smoke test: default provider with caching active doesn't break across operations
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .build();

        // Multiple calls to the same operation succeed (cache hit path)
        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");

        // Different operation also succeeds (cache miss then hit)
        assertThatThrownBy(() -> client.deleteOperation(r -> {})).hasMessageContaining("stop");
        assertThatThrownBy(() -> client.deleteOperation(r -> {})).hasMessageContaining("stop");

        client.close();
    }
}
