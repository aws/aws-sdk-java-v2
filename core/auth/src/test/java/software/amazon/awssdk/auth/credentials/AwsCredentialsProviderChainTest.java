/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.StringInputStream;

public class AwsCredentialsProviderChainTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Tests that, by default, the chain remembers which provider was able to
     * provide credentials, and only calls that provider for any additional
     * calls to getCredentials.
     */
    @Test
    public void testReusingLastProvider() throws Exception {
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
    public void testDisableReusingLastProvider() throws Exception {
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
    public void testMissingProfileUsesNextProvider() {
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
     * Tests that getCredentials throws an thrown if all providers in the
     * chain fail to provide credentials.
     */
    @Test
    public void testGetCredentialsException() {
        MockCredentialsProvider provider1 = new MockCredentialsProvider("Failed!");
        MockCredentialsProvider provider2 = new MockCredentialsProvider("Bad!");
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2)
                                                                       .build();

        thrown.expect(SdkClientException.class);
        thrown.expectMessage(provider1.exceptionMessage);
        thrown.expectMessage(provider2.exceptionMessage);

        chain.resolveCredentials();
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
