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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.identity.SdkIdentityProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.CompletableFutureUtils;

class S3ExpressIdentityProviderTest {
    private static final AwsCredentialsIdentity IDENTITY = AwsCredentialsIdentity.create("akid", "skid");

    private S3Client mockS3SyncClient;
    private S3ExpressIdentityCache mockS3ExpressCache;
    private IdentityProvider<AwsCredentialsIdentity> mockProvider;

    @BeforeEach
    public void methodSetup() {
        mockS3SyncClient = Mockito.mock(S3Client.class);
        mockS3ExpressCache = Mockito.mock(S3ExpressIdentityCache.class);
        mockProvider = Mockito.mock(IdentityProvider.class);
    }

    @Test
    void identityprovider_delegatesToCache() {
        when(mockProvider.resolveIdentity(any(ResolveIdentityRequest.class)))
            .thenAnswer(i -> CompletableFuture.completedFuture(IDENTITY));

        IdentityProvider<S3ExpressSessionCredentials> identityProvider =
            new DefaultS3ExpressIdentityProvider(mockS3ExpressCache, mockProvider);

        ResolveIdentityRequest request = ResolveIdentityRequest.builder()
                                                               .putProperty(S3ExpressAuthSchemeProvider.BUCKET, "foo")
                                                               .putProperty(SdkIdentityProperty.SDK_CLIENT, mockS3SyncClient)
                                                               .build();

        identityProvider.resolveIdentity(request).join();

        ArgumentCaptor<S3ExpressIdentityKey> keyCaptor = ArgumentCaptor.forClass(S3ExpressIdentityKey.class);

        Mockito.verify(mockS3ExpressCache).get(keyCaptor.capture());
        S3ExpressIdentityKey key = keyCaptor.getValue();
        assertThat(key.bucket()).isEqualTo("foo");
        assertThat(key.client()).isEqualTo(mockS3SyncClient);
        assertThat(key.identity()).isEqualTo(IDENTITY);
    }

    @Test
    void identityprovider_propagatesIdentityProviderFailures() {
        RuntimeException e = new RuntimeException();
        when(mockProvider.resolveIdentity(any(ResolveIdentityRequest.class))).thenReturn(CompletableFutureUtils.failedFuture(e));

        IdentityProvider<S3ExpressSessionCredentials> identityProvider =
            new DefaultS3ExpressIdentityProvider(mockS3ExpressCache, mockProvider);

        ResolveIdentityRequest request = ResolveIdentityRequest.builder().build();

        assertThatThrownBy(identityProvider.resolveIdentity(request)::join).getCause().isEqualTo(e);
    }
}