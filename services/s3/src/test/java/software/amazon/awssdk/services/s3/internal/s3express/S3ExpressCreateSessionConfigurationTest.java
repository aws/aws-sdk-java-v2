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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.CreateSessionRequest;
import software.amazon.awssdk.services.s3.model.CreateSessionResponse;
import software.amazon.awssdk.services.s3.model.SessionCredentials;


@ExtendWith(MockitoExtension.class)
class S3ExpressCreateSessionConfigurationTest {

    private static final Duration DEFAULT_API_CALL_TIMEOUT_VALUE = Duration.ofSeconds(10);
    private static final StaticCredentialsProvider CREDENTIALS_PROVIDER = StaticCredentialsProvider.create(mock(AwsCredentials.class));

    private static final CreateSessionResponse EMPTY_RESPONSE = CreateSessionResponse.builder()
                                                                                     .credentials(SessionCredentials.builder().build())
                                                                                     .build();

    @Mock
    S3Client s3Client;
    @Mock
    S3AsyncClient s3AsyncClient;
    @Captor
    ArgumentCaptor<CreateSessionRequest> requestCaptor;

    @Test
    void when_noApiCallTimeoutIsSet_DefaultValueIsUsedByCreateSessionRequest() {
        when(s3Client.createSession((CreateSessionRequest) any())).thenReturn(EMPTY_RESPONSE);
        when(s3Client.serviceClientConfiguration()).thenReturn(serviceClientConfigurationWithApiCallTimeout(null));

        S3ExpressIdentityCache s3ExpressIdentityCache = S3ExpressIdentityCache.create();
        s3ExpressIdentityCache.getCredentials(key(s3Client), CREDENTIALS_PROVIDER);

        verifyCreateSessionApiCallTimeoutOverride(DEFAULT_API_CALL_TIMEOUT_VALUE);
    }

    @Test
    void when_clientApiCallTimeoutIsSet_valueIsUsedByCreateSessionRequest() {
        Duration clientApiCallTimeout = Duration.ofSeconds(3);

        when(s3Client.serviceClientConfiguration()).thenReturn(serviceClientConfigurationWithApiCallTimeout(clientApiCallTimeout));
        when(s3Client.createSession((CreateSessionRequest) any())).thenReturn(EMPTY_RESPONSE);

        S3ExpressIdentityCache s3ExpressIdentityCache = S3ExpressIdentityCache.create();
        s3ExpressIdentityCache.getCredentials(key(s3Client), CREDENTIALS_PROVIDER);

        verifyCreateSessionApiCallTimeoutOverride(clientApiCallTimeout);
    }

    @Test
    void async_when_noApiCallTimeoutIsSet_DefaultValueIsUsedByCreateSessionRequest() {
        when(s3AsyncClient.createSession((CreateSessionRequest) any())).thenReturn(CompletableFuture.completedFuture(EMPTY_RESPONSE));
        when(s3AsyncClient.serviceClientConfiguration()).thenReturn(serviceClientConfigurationWithApiCallTimeout(null));

        S3ExpressIdentityCache s3ExpressIdentityCache = S3ExpressIdentityCache.create();
        s3ExpressIdentityCache.getCredentials(key(s3AsyncClient), CREDENTIALS_PROVIDER);

        asyncVerifyCreateSessionApiCallTimeoutOverride(DEFAULT_API_CALL_TIMEOUT_VALUE);
    }

    @Test
    void async_when_clientpiCallTimeoutIsSet_valueIsUsedByCreateSessionRequest() {
        Duration clientApiCallTimeout = Duration.ofSeconds(3);

        when(s3AsyncClient.serviceClientConfiguration()).thenReturn(serviceClientConfigurationWithApiCallTimeout(clientApiCallTimeout));
        when(s3AsyncClient.createSession((CreateSessionRequest) any())).thenReturn(CompletableFuture.completedFuture(EMPTY_RESPONSE));

        S3ExpressIdentityCache s3ExpressIdentityCache = S3ExpressIdentityCache.create();
        s3ExpressIdentityCache.getCredentials(key(s3AsyncClient), CREDENTIALS_PROVIDER);

        asyncVerifyCreateSessionApiCallTimeoutOverride(clientApiCallTimeout);
    }

    private S3ServiceClientConfiguration serviceClientConfigurationWithApiCallTimeout(Duration apiCallTimeout) {
        return S3ServiceClientConfiguration.builder()
                                           .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                                             .apiCallTimeout(apiCallTimeout)
                                                                                             .build())
                                           .build();
    }

    private S3ExpressIdentityKey key(SdkClient client) {
        return S3ExpressIdentityKey.builder()
                            .bucket("Bucket-1")
                            .client(client)
                            .identity(mock(AwsCredentialsIdentity.class))
                            .build();
    }

    private void asyncVerifyCreateSessionApiCallTimeoutOverride(Duration expectedTimeout) {
        verify(s3AsyncClient, times(1)).createSession(requestCaptor.capture());
        verifyApiCallTimeoutOverride(expectedTimeout);
    }

    private void verifyCreateSessionApiCallTimeoutOverride(Duration expectedTimeout) {
        verify(s3Client, times(1)).createSession(requestCaptor.capture());
        verifyApiCallTimeoutOverride(expectedTimeout);
    }

    private void verifyApiCallTimeoutOverride(Duration expectedTimeout) {
        Optional<AwsRequestOverrideConfiguration> awsRequestOverrideConfiguration = requestCaptor.getValue().overrideConfiguration();
        assertThat(awsRequestOverrideConfiguration).isPresent();
        assertThat(awsRequestOverrideConfiguration.get().apiCallTimeout()).isPresent().hasValue(expectedTimeout);
    }
}