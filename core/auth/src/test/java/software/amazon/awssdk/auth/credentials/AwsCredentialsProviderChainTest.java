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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.StringInputStream;

public class AwsCredentialsProviderChainTest {

    /**
     * Tests that, by default, the chain remembers which provider was able to
     * provide credentials, and only calls that provider for any additional
     * calls to getCredentials.
     */
    @Test
    public void resolveCredentials_reuseEnabled_reusesLastProvider() throws Exception {
        MockCredentialsProvider provider1 = new MockCredentialsProvider("Failed!");
        MockCredentialsProvider provider2 = new MockCredentialsProvider();
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2)
                                                                       .build();

        assertEquals(0, provider1.getCredentialsCallCount);
        assertEquals(0, provider2.getCredentialsCallCount);

        chain.resolveCredentials();
        assertEquals(1, provider1.getCredentialsCallCount);
        assertEquals(1, provider2.getCredentialsCallCount);

        chain.resolveCredentials();
        assertEquals(1, provider1.getCredentialsCallCount);
        assertEquals(2, provider2.getCredentialsCallCount);

        chain.resolveCredentials();
        assertEquals(1, provider1.getCredentialsCallCount);
        assertEquals(3, provider2.getCredentialsCallCount);
    }

    /**
     * Tests that, when provider caching is disabled, the chain will always try
     * all providers in the chain, starting with the first, until it finds a
     * provider that can return credentials.
     */
    @Test
    public void resolveCredentials_reuseDisabled_alwaysGoesThroughChain() throws Exception {
        MockCredentialsProvider provider1 = new MockCredentialsProvider("Failed!");
        MockCredentialsProvider provider2 = new MockCredentialsProvider();
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2)
                                                                       .reuseLastProviderEnabled(false)
                                                                       .build();

        assertEquals(0, provider1.getCredentialsCallCount);
        assertEquals(0, provider2.getCredentialsCallCount);

        chain.resolveCredentials();
        assertEquals(1, provider1.getCredentialsCallCount);
        assertEquals(1, provider2.getCredentialsCallCount);

        chain.resolveCredentials();
        assertEquals(2, provider1.getCredentialsCallCount);
        assertEquals(2, provider2.getCredentialsCallCount);
    }

    @Test
    public void resolveCredentials_missingProfile_usesNextProvider() {
        ProfileCredentialsProvider provider =
            new ProfileCredentialsProvider.BuilderImpl()
                .defaultProfileFileLoader(() -> ProfileFile.builder()
                                                           .content(new StringInputStream(""))
                                                           .type(ProfileFile.Type.CONFIGURATION)
                                                           .build())
                .build();

        MockCredentialsProvider provider2 = new MockCredentialsProvider();

        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder().credentialsProviders(provider, provider2).build();

        chain.resolveCredentials();
        assertEquals(1, provider2.getCredentialsCallCount);
    }

    /**
     * Tests that resolveCredentials throws an thrown if all providers in the
     * chain fail to provide credentials.
     */
    @Test
    public void resolveCredentials_allProvidersFail_throwsExceptionWithMessageFromAllProviders() {
        MockCredentialsProvider provider1 = new MockCredentialsProvider("Failed!");
        MockCredentialsProvider provider2 = new MockCredentialsProvider("Bad!");
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2)
                                                                       .build();

        SdkClientException e = assertThrows(SdkClientException.class, () -> chain.resolveCredentials());
        assertThat(e.getMessage()).contains(provider1.exceptionMessage);
        assertThat(e.getMessage()).contains(provider2.exceptionMessage);
    }

    @Test
    public void resolveCredentials_emptyChain_throwsException() {
        assertThrowsIllegalArgument(() -> AwsCredentialsProviderChain.of());

        assertThrowsIllegalArgument(() -> AwsCredentialsProviderChain
            .builder()
            .credentialsProviders()
            .build());

        assertThrowsIllegalArgument(() -> AwsCredentialsProviderChain
            .builder()
            .credentialsProviders(Arrays.asList())
            .build());
    }

    private void assertThrowsIllegalArgument(Executable executable) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertThat(e.getMessage()).contains("No credential providers were specified.");
    }

    /**
     * Tests that the chain is setup correctly with the overloaded methods that accept the AwsCredentialsProvider type.
     */
    @Test
    public void createMethods_withOldCredentialsType_work() {
        AwsCredentialsProvider provider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
            "accessKey", "secretKey"));
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.of(provider));
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.builder().credentialsProviders(provider).build());
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.builder().credentialsProviders(Arrays.asList(provider)).build());
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.builder().addCredentialsProvider(provider).build());
    }

    /**
     * Tests that the chain is setup correctly with the overloaded methods that accept the IdentityProvider type.
     */
    @Test
    public void createMethods_withNewCredentialsType_work() {
        IdentityProvider<AwsCredentialsIdentity> provider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
            "accessKey", "secretKey"));
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.of(provider));
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.builder().credentialsProviders(provider).build());
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.builder().credentialsIdentityProviders(Arrays.asList(provider)).build());
        assertChainResolvesCorrectly(AwsCredentialsProviderChain.builder().addCredentialsProvider(provider).build());
    }

    private static void assertChainResolvesCorrectly(AwsCredentialsProviderChain chain) {
        AwsCredentials credentials = chain.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("accessKey");
        assertThat(credentials.secretAccessKey()).isEqualTo("secretKey");
    }

    @Test
    public void invalidate_propagatesToAllChildren() {
        TrackingCredentialsProvider provider1 = new TrackingCredentialsProvider("key1", "secret1");
        TrackingCredentialsProvider provider2 = new TrackingCredentialsProvider("key2", "secret2");
        TrackingCredentialsProvider provider3 = new TrackingCredentialsProvider("key3", "secret3");

        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2, provider3)
                                                                       .build();

        AwsCredentialsIdentity identity = AwsBasicCredentials.create("key1", "secret1");
        chain.invalidate(identity).join();

        assertThat(provider1.invalidateCallCount).isEqualTo(1);
        assertThat(provider2.invalidateCallCount).isEqualTo(1);
        assertThat(provider3.invalidateCallCount).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void invalidate_doesNotShortCircuit_whenChildThrows() {
        AwsCredentialsProvider mockProvider1 = mock(AwsCredentialsProvider.class);
        AwsCredentialsProvider mockProvider2 = mock(AwsCredentialsProvider.class);
        AwsCredentialsProvider mockProvider3 = mock(AwsCredentialsProvider.class);

        doThrow(new RuntimeException("Provider 1 failed")).when(mockProvider1).invalidate(any());

        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(mockProvider1, mockProvider2, mockProvider3)
                                                                       .build();

        AwsCredentialsIdentity identity = AwsBasicCredentials.create("key1", "secret1");
        chain.invalidate(identity).join();

        verify(mockProvider1, times(1)).invalidate(identity);
        verify(mockProvider2, times(1)).invalidate(identity);
        verify(mockProvider3, times(1)).invalidate(identity);
    }

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

    private static final class MockCredentialsProvider implements AwsCredentialsProvider {
        private final StaticCredentialsProvider staticCredentialsProvider;
        private final String exceptionMessage;
        int getCredentialsCallCount = 0;

        private MockCredentialsProvider() {
            this(null);
        }

        private MockCredentialsProvider(String exceptionMessage) {
            staticCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey"));
            this.exceptionMessage = exceptionMessage;
        }

        @Override
        public AwsCredentials resolveCredentials() {
            getCredentialsCallCount++;

            if (exceptionMessage != null) {
                throw new RuntimeException(exceptionMessage);
            } else {
                return staticCredentialsProvider.resolveCredentials();
            }
        }
    }
}
