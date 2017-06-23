/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AwsCredentialsProviderChainTest {

    /**
     * Tests that, by default, the chain remembers which provider was able to
     * provide credentials, and only calls that provider for any additional
     * calls to getCredentials.
     */
    @Test
    public void testReusingLastProvider() throws Exception {
        MockCredentialsProvider provider1 = new MockCredentialsProvider();
        provider1.throwException = true;
        MockCredentialsProvider provider2 = new MockCredentialsProvider();
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2)
                                                                       .build();

        assertEquals(0, provider1.getCredentialsCallCount);
        assertEquals(0, provider2.getCredentialsCallCount);

        chain.getCredentials();
        assertEquals(1, provider1.getCredentialsCallCount);
        assertEquals(1, provider2.getCredentialsCallCount);

        chain.getCredentials();
        assertEquals(1, provider1.getCredentialsCallCount);
        assertEquals(2, provider2.getCredentialsCallCount);

        chain.getCredentials();
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
        MockCredentialsProvider provider1 = new MockCredentialsProvider();
        provider1.throwException = true;
        MockCredentialsProvider provider2 = new MockCredentialsProvider();
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.builder()
                                                                       .credentialsProviders(provider1, provider2)
                                                                       .reuseLastProviderEnabled(false)
                                                                       .build();

        assertEquals(0, provider1.getCredentialsCallCount);
        assertEquals(0, provider2.getCredentialsCallCount);

        chain.getCredentials();
        assertEquals(1, provider1.getCredentialsCallCount);
        assertEquals(1, provider2.getCredentialsCallCount);

        chain.getCredentials();
        assertEquals(2, provider1.getCredentialsCallCount);
        assertEquals(2, provider2.getCredentialsCallCount);
    }


    private static final class MockCredentialsProvider extends StaticCredentialsProvider {
        public int getCredentialsCallCount = 0;
        public boolean throwException = false;

        public MockCredentialsProvider() {
            super(new AwsCredentials("accessKey", "secretKey"));
        }

        @Override
        public AwsCredentials getCredentials() {
            getCredentialsCallCount++;

            if (throwException) {
                throw new RuntimeException("No credentials");
            } else {
                return super.getCredentials();
            }
        }
    }
}
