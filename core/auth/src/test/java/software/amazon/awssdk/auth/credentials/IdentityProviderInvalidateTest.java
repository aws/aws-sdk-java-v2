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

package software.amazon.awssdk.auth.credentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Unit tests for identity provider invalidation: chain propagation, LazyAwsCredentialsProvider delegation,
 * and individual provider accessKeyId matching.
 */
@WireMockTest
class IdentityProviderInvalidateTest {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String PROFILE_NAME = "some-profile";
    private static final String TOKEN_STUB = "some-token";
    private static final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    @BeforeEach
    void setup() {
        environmentVariableHelper.reset();
        environmentVariableHelper.set(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT,
                                      "http://localhost:" + wireMockServer.getPort());
    }

    @AfterAll
    static void teardown() {
        environmentVariableHelper.reset();
    }

    // ==================== Chain Propagation Tests ====================

    @Test
    void invalidate_chainPropagatesTo_allChildren() {
        TrackingCredentialsProvider provider1 = new TrackingCredentialsProvider("key1", "secret1");
        TrackingCredentialsProvider provider2 = new TrackingCredentialsProvider("key2", "secret2");
        TrackingCredentialsProvider provider3 = new TrackingCredentialsProvider("key3", "secret3");

        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2, provider3)
                                                                       .build();

        AwsCredentialsIdentity identity = AwsBasicCredentials.create("key1", "secret1");
        chain.invalidate(identity);

        assertThat(provider1.invalidateCallCount).isEqualTo(1);
        assertThat(provider2.invalidateCallCount).isEqualTo(1);
        assertThat(provider3.invalidateCallCount).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void invalidate_chainDoesNotShortCircuit_whenChildThrows() {
        AwsCredentialsProvider mockProvider1 = mock(AwsCredentialsProvider.class);
        AwsCredentialsProvider mockProvider2 = mock(AwsCredentialsProvider.class);
        AwsCredentialsProvider mockProvider3 = mock(AwsCredentialsProvider.class);

        // First provider throws on invalidate
        doThrow(new RuntimeException("Provider 1 failed")).when(mockProvider1).invalidate(any());

        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(mockProvider1, mockProvider2, mockProvider3)
                                                                       .build();

        AwsCredentialsIdentity identity = AwsBasicCredentials.create("key1", "secret1");
        chain.invalidate(identity);

        // All providers should still be called despite the first one throwing
        verify(mockProvider1, times(1)).invalidate(identity);
        verify(mockProvider2, times(1)).invalidate(identity);
        verify(mockProvider3, times(1)).invalidate(identity);
    }

    @Test
    void invalidate_chainWithStaticProvider_isNoOpWithoutError() {
        StaticCredentialsProvider staticProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("staticKey", "staticSecret"));
        TrackingCredentialsProvider trackingProvider = new TrackingCredentialsProvider("key1", "secret1");

        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(staticProvider, trackingProvider)
                                                                       .build();

        AwsCredentialsIdentity identity = AwsBasicCredentials.create("key1", "secret1");

        // Should not throw - StaticCredentialsProvider uses default no-op
        chain.invalidate(identity);

        // The tracking provider should still receive the call
        assertThat(trackingProvider.invalidateCallCount).isEqualTo(1);
    }

    // ==================== LazyAwsCredentialsProvider Delegation Tests ====================

    @Test
    void invalidate_lazyProvider_initialized_delegatesToInner() {
        TrackingCredentialsProvider innerProvider = new TrackingCredentialsProvider("key1", "secret1");
        LazyAwsCredentialsProvider lazyProvider = LazyAwsCredentialsProvider.create(() -> innerProvider);

        // Force initialization by calling resolveCredentials
        lazyProvider.resolveCredentials();

        AwsCredentialsIdentity identity = AwsBasicCredentials.create("key1", "secret1");
        lazyProvider.invalidate(identity);

        assertThat(innerProvider.invalidateCallCount).isEqualTo(1);
    }

    @Test
    void invalidate_lazyProvider_notInitialized_isNoOp() {
        List<Boolean> constructorCalled = new ArrayList<>();
        LazyAwsCredentialsProvider lazyProvider = LazyAwsCredentialsProvider.create(() -> {
            constructorCalled.add(true);
            return new TrackingCredentialsProvider("key1", "secret1");
        });

        // Do NOT call resolveCredentials — provider should not be initialized
        AwsCredentialsIdentity identity = AwsBasicCredentials.create("key1", "secret1");
        lazyProvider.invalidate(identity);

        // The supplier should never have been called - lazy not initialized
        assertThat(constructorCalled).isEmpty();
    }

    // ==================== DefaultCredentialsProvider Delegation Test ====================

    @Test
    void invalidate_defaultCredentialsProvider_delegatesToLazyChain() {
        // DefaultCredentialsProvider delegates to LazyAwsCredentialsProvider which delegates to chain.
        // We verify end-to-end by using profile credentials and checking delegation completes without error.
        DefaultCredentialsProvider provider = DefaultCredentialsProvider.builder()
                                                                       .profileFile(profileWithCredentials("testKey", "testSecret"))
                                                                       .profileName("test")
                                                                       .build();

        // Trigger initialization
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("testKey");

        // Calling invalidate should not throw — it propagates through lazy -> chain -> children
        AwsCredentialsIdentity identity = AwsBasicCredentials.create("testKey", "testSecret");
        provider.invalidate(identity);
    }

    // ==================== Individual Provider AccessKeyId Matching Tests ====================

    @Test
    void invalidate_instanceProfileProvider_matchingAccessKeyId_invalidatesCache() {
        String accessKeyId = "ACCESS_KEY_ID";
        String expirationFarFuture = DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)));
        String credentialsJson = "{\"AccessKeyId\":\"" + accessKeyId + "\","
                                 + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
                                 + "\"Expiration\":\"" + expirationFarFuture + "\"}";

        stubImdsCredentials(credentialsJson);

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        // First call: fetches and caches credentials
        AwsCredentials firstCredentials = provider.resolveCredentials();
        assertThat(firstCredentials.accessKeyId()).isEqualTo(accessKeyId);

        // Set up a different credential response for second fetch
        String newAccessKeyId = "NEW_ACCESS_KEY_ID";
        String newCredentialsJson = "{\"AccessKeyId\":\"" + newAccessKeyId + "\","
                                    + "\"SecretAccessKey\":\"NEW_SECRET_ACCESS_KEY\","
                                    + "\"Expiration\":\"" + expirationFarFuture + "\"}";
        stubImdsCredentials(newCredentialsJson);

        // Invalidate with matching accessKeyId — should mark cache as stale
        AwsCredentialsIdentity identity = AwsBasicCredentials.create(accessKeyId, "SECRET_ACCESS_KEY");
        provider.invalidate(identity);

        // Next resolveCredentials() should attempt a fresh fetch
        AwsCredentials refreshedCredentials = provider.resolveCredentials();
        assertThat(refreshedCredentials.accessKeyId()).isEqualTo(newAccessKeyId);
    }

    @Test
    void invalidate_instanceProfileProvider_nonMatchingAccessKeyId_doesNotInvalidateCache() {
        String accessKeyId = "ACCESS_KEY_ID";
        String expirationFarFuture = DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)));
        String credentialsJson = "{\"AccessKeyId\":\"" + accessKeyId + "\","
                                 + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
                                 + "\"Expiration\":\"" + expirationFarFuture + "\"}";

        stubImdsCredentials(credentialsJson);

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        // First call: fetches and caches credentials
        AwsCredentials firstCredentials = provider.resolveCredentials();
        assertThat(firstCredentials.accessKeyId()).isEqualTo(accessKeyId);

        // Set up different credentials on the server (would be returned if cache is invalidated)
        String newCredentialsJson = "{\"AccessKeyId\":\"NEW_ACCESS_KEY_ID\","
                                    + "\"SecretAccessKey\":\"NEW_SECRET_ACCESS_KEY\","
                                    + "\"Expiration\":\"" + expirationFarFuture + "\"}";
        stubImdsCredentials(newCredentialsJson);

        // Invalidate with a DIFFERENT accessKeyId — should NOT invalidate cache
        AwsCredentialsIdentity differentIdentity = AwsBasicCredentials.create("DIFFERENT_KEY", "SECRET");
        provider.invalidate(differentIdentity);

        // Cache should still return the original cached credential
        AwsCredentials secondCredentials = provider.resolveCredentials();
        assertThat(secondCredentials.accessKeyId()).isEqualTo(accessKeyId);
    }

    // ==================== Helper Methods ====================

    private void stubImdsCredentials(String credentialsJson) {
        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                   .willReturn(aResponse().withBody(TOKEN_STUB)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                   .willReturn(aResponse().withBody(PROFILE_NAME)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME))
                                   .willReturn(aResponse().withBody(credentialsJson)));
    }

    private java.util.function.Supplier<software.amazon.awssdk.profiles.ProfileFile> profileWithCredentials(
        String accessKeyId, String secretAccessKey) {
        String contents = String.format("[test]\naws_access_key_id = %s\naws_secret_access_key = %s\n",
                                        accessKeyId, secretAccessKey);
        return () -> software.amazon.awssdk.profiles.ProfileFile.builder()
                                                                .content(new software.amazon.awssdk.utils.StringInputStream(contents))
                                                                .type(software.amazon.awssdk.profiles.ProfileFile.Type.CREDENTIALS)
                                                                .build();
    }

    /**
     * A simple AwsCredentialsProvider that tracks invalidate() calls.
     */
    private static final class TrackingCredentialsProvider implements AwsCredentialsProvider {
        private final AwsBasicCredentials credentials;
        int invalidateCallCount = 0;

        TrackingCredentialsProvider(String accessKeyId, String secretAccessKey) {
            this.credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return credentials;
        }

        @Override
        public CompletableFuture<Void> invalidate(AwsCredentialsIdentity identity) {
            invalidateCallCount++;
            return CompletableFuture.completedFuture(null);
        }
    }
}
