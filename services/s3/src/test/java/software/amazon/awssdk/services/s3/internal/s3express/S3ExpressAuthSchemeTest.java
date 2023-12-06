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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;

public class S3ExpressAuthSchemeTest {

    @Test
    void identityProvider_noAwsCredentialProviders_throwsException() {
        DefaultS3ExpressAuthScheme defaultS3ExpressAuthScheme = DefaultS3ExpressAuthScheme.create();

        IdentityProviders eligibleIdentityProviders = IdentityProviders.builder().build();
        assertThatThrownBy(() -> defaultS3ExpressAuthScheme.identityProvider(eligibleIdentityProviders))
            .isInstanceOf(IllegalStateException.class).hasMessage("Could not find a provider for AwsCredentialsIdentity");
    }

    @Test
    void identityProvider_oneEligibleProvider_works() {
        DefaultS3ExpressAuthScheme defaultS3ExpressAuthScheme = DefaultS3ExpressAuthScheme.create();

        IdentityProviders eligibleIdentityProviders = IdentityProviders.builder()
                                                                       .putIdentityProvider(DefaultCredentialsProvider.create())
                                                                       .build();
        IdentityProvider<S3ExpressSessionCredentials> s3ExpressIdentityProvider =
            defaultS3ExpressAuthScheme.identityProvider(eligibleIdentityProviders);
        assertThat(s3ExpressIdentityProvider).isInstanceOf(DefaultS3ExpressIdentityProvider.class);
    }
}
