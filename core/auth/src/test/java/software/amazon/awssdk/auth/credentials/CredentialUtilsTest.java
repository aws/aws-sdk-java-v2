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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;

public class CredentialUtilsTest {

    @Test
    public void isAnonymous_AwsCredentials_true() {
        assertThat(CredentialUtils.isAnonymous(AnonymousCredentialsProvider.create().resolveCredentials())).isTrue();
    }

    @Test
    public void isAnonymous_AwsCredentials_false() {
        assertThat(CredentialUtils.isAnonymous(AwsBasicCredentials.create("akid", "skid"))).isFalse();
    }

    @Test
    public void isAnonymous_AwsCredentialsIdentity_true() {
        assertThat(CredentialUtils.isAnonymous((AwsCredentialsIdentity) AnonymousCredentialsProvider.create().resolveCredentials())).isTrue();
    }

    @Test
    public void isAnonymous_AwsCredentialsIdentity_false() {
        assertThat(CredentialUtils.isAnonymous(AwsCredentialsIdentity.create("akid", "skid"))).isFalse();
    }

    @Test
    public void toCredentials_null_returnsNull() {
        assertThat(CredentialUtils.toCredentials(null)).isNull();
    }


    @Test
    public void toCredentials_AwsSessionCredentials_doesNotCreateNewObject() {
        AwsSessionCredentialsIdentity input = AwsSessionCredentials.create("ak", "sk", "session");
        AwsCredentials output = CredentialUtils.toCredentials(input);
        assertThat(output).isSameAs(input);
    }

    @Test
    public void toCredentials_AwsSessionCredentialsIdentity_returnsAwsSessionCredentials() {
        AwsCredentials awsCredentials = CredentialUtils.toCredentials(AwsSessionCredentialsIdentity.create(
            "akid", "skid", "session"));

        assertThat(awsCredentials).isInstanceOf(AwsSessionCredentials.class);
        AwsSessionCredentials awsSessionCredentials = (AwsSessionCredentials) awsCredentials;
        assertThat(awsSessionCredentials.accessKeyId()).isEqualTo("akid");
        assertThat(awsSessionCredentials.secretAccessKey()).isEqualTo("skid");
        assertThat(awsSessionCredentials.sessionToken()).isEqualTo("session");
    }

    @Test
    public void toCredentials_AwsCredentials_returnsAsIs() {
        AwsCredentialsIdentity input = AwsBasicCredentials.create("ak", "sk");
        AwsCredentials output = CredentialUtils.toCredentials(input);
        assertThat(output).isSameAs(input);
    }

    @Test
    public void toCredentials_AwsCredentialsIdentity_returnsAwsCredentials() {
        AwsCredentials awsCredentials = CredentialUtils.toCredentials(AwsCredentialsIdentity.create("akid", "skid"));

        assertThat(awsCredentials.accessKeyId()).isEqualTo("akid");
        assertThat(awsCredentials.secretAccessKey()).isEqualTo("skid");
    }

    @Test
    public void toCredentials_Anonymous_returnsAnonymous() {
        AwsCredentials awsCredentials = CredentialUtils.toCredentials(new AwsCredentialsIdentity() {
            @Override
            public String accessKeyId() {
                return null;
            }

            @Override
            public String secretAccessKey() {
                return null;
            }
        });

        assertThat(awsCredentials.accessKeyId()).isNull();
        assertThat(awsCredentials.secretAccessKey()).isNull();
    }

    @Test
    public void toCredentialsProvider_null_returnsNull() {
        assertThat(CredentialUtils.toCredentialsProvider(null)).isNull();
    }

    @Test
    public void toCredentialsProvider_AwsCredentialsProvider_returnsAsIs() {
        IdentityProvider<AwsCredentialsIdentity> input =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        AwsCredentialsProvider output = CredentialUtils.toCredentialsProvider(input);
        assertThat(output).isSameAs(input);
    }

    @Test
    public void toCredentialsProvider_IdentityProvider_converts() {
        AwsCredentialsProvider credentialsProvider = CredentialUtils.toCredentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")));
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("akid");
        assertThat(credentials.secretAccessKey()).isEqualTo("skid");
    }
}
