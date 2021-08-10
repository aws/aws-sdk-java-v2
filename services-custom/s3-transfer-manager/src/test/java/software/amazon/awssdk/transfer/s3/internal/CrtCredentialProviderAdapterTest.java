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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.HttpCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;

public class CrtCredentialProviderAdapterTest {

    @Test
    public void crtCredentials_withSession_shouldConvert() {
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider
            .create(AwsSessionCredentials.create("foo", "bar", "session"));

        CredentialsProvider crtCredentialsProvider = new CrtCredentialsProviderAdapter(awsCredentialsProvider)
            .crtCredentials();

        Credentials credentials = crtCredentialsProvider.getCredentials().join();

        assertThat(credentials.getAccessKeyId()).isEqualTo("foo".getBytes(StandardCharsets.UTF_8));
        assertThat(credentials.getSecretAccessKey()).isEqualTo("bar".getBytes(StandardCharsets.UTF_8));
        assertThat(credentials.getSessionToken()).isEqualTo("session".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void crtCredentials_withoutSession_shouldConvert() {
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider
            .create(AwsBasicCredentials.create("foo", "bar"));

        CredentialsProvider crtCredentialsProvider = new CrtCredentialsProviderAdapter(awsCredentialsProvider)
            .crtCredentials();

        Credentials credentials = crtCredentialsProvider.getCredentials().join();

        assertThat(credentials.getAccessKeyId()).isEqualTo("foo".getBytes(StandardCharsets.UTF_8));
        assertThat(credentials.getSecretAccessKey()).isEqualTo("bar".getBytes(StandardCharsets.UTF_8));
        assertThat(credentials.getSessionToken()).isNull();
    }

    @Test
    public void crtCredentials_provideAwsCredentials_shouldInvokeResolveAndClose() {
        HttpCredentialsProvider awsCredentialsProvider = Mockito.mock(HttpCredentialsProvider.class);
        AwsCredentials credentials = new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return "foo";
            }

            @Override
            public String secretAccessKey() {
                return "bar";
            }
        };
        when(awsCredentialsProvider.resolveCredentials()).thenReturn(credentials);

        CrtCredentialsProviderAdapter adapter = new CrtCredentialsProviderAdapter(awsCredentialsProvider);
        CredentialsProvider crtCredentialsProvider = adapter.crtCredentials();

        Credentials crtCredentials = crtCredentialsProvider.getCredentials().join();
        assertThat(crtCredentials.getAccessKeyId()).isEqualTo("foo".getBytes(StandardCharsets.UTF_8));
        assertThat(crtCredentials.getSecretAccessKey()).isEqualTo("bar".getBytes(StandardCharsets.UTF_8));
        verify(awsCredentialsProvider).resolveCredentials();

        adapter.close();
        verify(awsCredentialsProvider).close();
    }
}
