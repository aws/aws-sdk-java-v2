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

package software.amazon.awssdk.identity.spi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class IdentityProviderConfigurationTest {

    @Test
    public void builder_empty_builds() {
        assertNotNull(IdentityProviderConfiguration.builder().build());
    }

    @Test
    public void identityProvider_withUnknownType_returnsNull() {
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsProvider = new AwsCredentialsProvider();
        IdentityProviderConfiguration identityProviders =
            IdentityProviderConfiguration.builder().putIdentityProvider(awsCredentialsProvider).build();
        assertNull(identityProviders.identityProvider(TokenIdentity.class));
    }

    @Test
    public void identityProvider_canBeRetrieved() {
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsProvider = new AwsCredentialsProvider();
        IdentityProviderConfiguration identityProviders =
            IdentityProviderConfiguration.builder().putIdentityProvider(awsCredentialsProvider).build();
        assertSame(awsCredentialsProvider, identityProviders.identityProvider(AwsCredentialsIdentity.class));
    }

    @Test
    public void putIdentityProvider_ofSameType_isReplaced() {
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsProvider1 = new AwsCredentialsProvider();
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsProvider2 = new AwsCredentialsProvider();
        IdentityProviderConfiguration identityProviders =
            IdentityProviderConfiguration.builder()
                                         .putIdentityProvider(awsCredentialsProvider1)
                                         .putIdentityProvider(awsCredentialsProvider2).build();
        assertSame(awsCredentialsProvider2, identityProviders.identityProvider(AwsCredentialsIdentity.class));
    }

    @Test
    public void identityProvider_withSubType_returnsAppropriateSubType() {
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsProvider = new AwsCredentialsProvider();
        IdentityProvider<AwsSessionCredentialsIdentity> awsSessionCredentialsProvider = new AwsSessionCredentialsProvider();
        IdentityProviderConfiguration identityProviders =
            IdentityProviderConfiguration.builder()
                                         .putIdentityProvider(awsCredentialsProvider)
                                         .putIdentityProvider(awsSessionCredentialsProvider)
                                         .build();

        assertSame(awsCredentialsProvider, identityProviders.identityProvider(AwsCredentialsIdentity.class));
        assertSame(awsSessionCredentialsProvider, identityProviders.identityProvider(AwsSessionCredentialsIdentity.class));
    }

    @Test
    public void identityProvider_withOnlySubType_returnsNullForParentType() {
        IdentityProvider<AwsSessionCredentialsIdentity> awsSessionCredentialsProvider = new AwsSessionCredentialsProvider();
        IdentityProviderConfiguration identityProviders =
            IdentityProviderConfiguration.builder()
                                         .putIdentityProvider(awsSessionCredentialsProvider)
                                         .build();

        assertNull(identityProviders.identityProvider(AwsCredentialsIdentity.class));
    }

    @Test
    public void copyBuilder_addIdentityProvider_works() {
        IdentityProvider<AwsCredentialsIdentity> awsCredentialsProvider = new AwsCredentialsProvider();
        IdentityProviderConfiguration identityProviders =
            IdentityProviderConfiguration.builder()
                                         .putIdentityProvider(awsCredentialsProvider)
                                         .build();

        IdentityProvider<TokenIdentity> tokenProvider = new TokenProvider();
        identityProviders = identityProviders.copy(builder -> builder.putIdentityProvider(tokenProvider));

        assertSame(awsCredentialsProvider, identityProviders.identityProvider(AwsCredentialsIdentity.class));
        assertSame(tokenProvider, identityProviders.identityProvider(TokenIdentity.class));
    }

    private static final class AwsCredentialsProvider implements IdentityProvider<AwsCredentialsIdentity> {

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<? extends AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return null;
        }
    }

    private static final class AwsSessionCredentialsProvider implements IdentityProvider<AwsSessionCredentialsIdentity> {

        @Override
        public Class<AwsSessionCredentialsIdentity> identityType() {
            return AwsSessionCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<? extends AwsSessionCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return null;
        }
    }

    private static final class TokenProvider implements IdentityProvider<TokenIdentity> {

        @Override
        public Class<TokenIdentity> identityType() {
            return TokenIdentity.class;
        }

        @Override
        public CompletableFuture<? extends TokenIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return null;
        }
    }
}
