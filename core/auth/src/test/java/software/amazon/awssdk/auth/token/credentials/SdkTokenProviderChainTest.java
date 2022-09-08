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

package software.amazon.awssdk.auth.token.credentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.TestBearerToken;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProviderChain;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.core.exception.SdkClientException;

public class SdkTokenProviderChainTest {

    public static final Instant SAMPLE_EXPIRATION_TIME = Instant.ofEpochMilli(1642108606L);
    public static final String SAMPLE_TOKEN_STRING = "mJ_9.B5f-4.1Jmv";

    /**
     * Tests that, by default, the chain remembers which provider was able to provide token, and only calls that provider
     * for any additional calls to getToken.
     */
    @Test
    public void testReusingLastProvider() {
        MockTokenProvider provider1 = new MockTokenProvider("Failed!");
        MockTokenProvider provider2 = new MockTokenProvider();
        SdkTokenProviderChain chain = SdkTokenProviderChain.builder()
                                                           .tokenProviders(provider1, provider2)
                                                           .build();

        assertEquals(0, provider1.getTokenCallCount);
        assertEquals(0, provider2.getTokenCallCount);

        chain.resolveToken();
        assertEquals(1, provider1.getTokenCallCount);
        assertEquals(1, provider2.getTokenCallCount);

        chain.resolveToken();
        assertEquals(1, provider1.getTokenCallCount);
        assertEquals(2, provider2.getTokenCallCount);

        chain.resolveToken();
        assertEquals(1, provider1.getTokenCallCount);
        assertEquals(3, provider2.getTokenCallCount);
    }

    /**
     * Tests that, when provider caching is disabled, the chain will always try all providers in the chain, starting with the
     * first, until it finds a provider that can return token.
     */
    @Test
    public void testDisableReusingLastProvider() {
        MockTokenProvider provider1 = new MockTokenProvider("Failed!");
        MockTokenProvider provider2 = new MockTokenProvider();
        SdkTokenProviderChain chain = SdkTokenProviderChain.builder()
                                                           .tokenProviders(provider1, provider2)
                                                           .reuseLastProviderEnabled(false)
                                                           .build();

        assertEquals(0, provider1.getTokenCallCount);
        assertEquals(0, provider2.getTokenCallCount);

        chain.resolveToken();
        assertEquals(1, provider1.getTokenCallCount);
        assertEquals(1, provider2.getTokenCallCount);

        chain.resolveToken();
        assertEquals(2, provider1.getTokenCallCount);
        assertEquals(2, provider2.getTokenCallCount);
    }


    /**
     * Tests that getToken throws an Exception if all providers in the chain fail to provide token.
     */
    @Test
    public void testGetTokenException() {
        MockTokenProvider provider1 = new MockTokenProvider("Failed!");
        MockTokenProvider provider2 = new MockTokenProvider("Bad!");
        SdkTokenProviderChain chain = SdkTokenProviderChain.builder()
                                                           .tokenProviders(provider1, provider2)
                                                           .build();

        assertThrows(SdkClientException.class, () -> chain.resolveToken());
    }


    private static final class MockTokenProvider implements SdkTokenProvider {
        private final SdkTokenProvider sdkTokenProvider;
        private final String exceptionMessage;
        int getTokenCallCount = 0;

        private MockTokenProvider() {
            this(null);
        }

        private MockTokenProvider(String exceptionMessage) {
            sdkTokenProvider = StaticTokenProvider.create(TestBearerToken.create(SAMPLE_TOKEN_STRING, SAMPLE_EXPIRATION_TIME));
            this.exceptionMessage = exceptionMessage;
        }

        @Override
        public SdkToken resolveToken() {
            getTokenCallCount++;

            if (exceptionMessage != null) {
                throw new RuntimeException(exceptionMessage);
            } else {
                return sdkTokenProvider.resolveToken();
            }
        }
    }
}
