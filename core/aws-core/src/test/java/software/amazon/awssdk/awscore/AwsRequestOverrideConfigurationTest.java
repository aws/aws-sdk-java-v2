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

package software.amazon.awssdk.awscore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;

public class AwsRequestOverrideConfigurationTest {

    @Test
    public void testCredentialsProviderWorksWithBothOldAndNewInterfaceTypes() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("akid","skid"));

        AwsRequestOverrideConfiguration configuration1 = AwsRequestOverrideConfiguration
            .builder().credentialsProvider(credentialsProvider).build();

        AwsRequestOverrideConfiguration configuration2 = AwsRequestOverrideConfiguration
            .builder().credentialsProvider((IdentityProvider<AwsCredentialsIdentity>) credentialsProvider).build();

        assertCredentialsEqual(configuration1.credentialsProvider().get(), configuration1.credentialsIdentityProvider().get());
        assertCredentialsEqual(configuration2.credentialsProvider().get(), configuration2.credentialsIdentityProvider().get());
        assertCredentialsEqual(configuration1.credentialsProvider().get(), configuration2.credentialsIdentityProvider().get());
        assertCredentialsEqual(configuration2.credentialsProvider().get(), configuration1.credentialsIdentityProvider().get());
    }

    @Test
    public void testTokenIdentityProvider() {
        IdentityProvider<TokenIdentity> tokenIdentityProvider = StaticTokenProvider.create(() -> "test-token");

        AwsRequestOverrideConfiguration configuration1 = AwsRequestOverrideConfiguration
            .builder().tokenIdentityProvider(tokenIdentityProvider).build();

        assertThat(configuration1.tokenIdentityProvider().get().resolveIdentity().join().token())
            .isEqualTo(tokenIdentityProvider.resolveIdentity().join().token());
    }

    private void assertCredentialsEqual(AwsCredentialsProvider credentialsProvider,
                                        IdentityProvider<? extends AwsCredentialsIdentity> identityProvider) {
        AwsCredentials creds1 = credentialsProvider.resolveCredentials();
        AwsCredentialsIdentity creds2 = identityProvider.resolveIdentity().join();
        assertThat(creds1.accessKeyId()).isEqualTo(creds2.accessKeyId());
        assertThat(creds1.secretAccessKey()).isEqualTo(creds2.secretAccessKey());
    }
}
