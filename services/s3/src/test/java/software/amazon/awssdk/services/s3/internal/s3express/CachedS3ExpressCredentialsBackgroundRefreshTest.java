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

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.TooFewActualInvocations;
import org.mockito.verification.VerificationMode;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.SessionCredentials;

class CachedS3ExpressCredentialsBackgroundRefreshTest {
    private Function<S3ExpressIdentityKey, SessionCredentials> mockCredentialsSupplier;
    private S3Client mockS3;
    private AwsCredentialsIdentity mockIdentity;

    private CachedS3ExpressCredentials cachedCredentials;


    @BeforeEach
    public void setup() {
        mockCredentialsSupplier = Mockito.mock(Function.class);
        mockS3 = Mockito.mock(S3Client.class);
        mockIdentity = Mockito.mock(AwsCredentialsIdentity.class);

        cachedCredentials = CachedS3ExpressCredentials.builder(mockCredentialsSupplier)
                                                      .key(S3ExpressIdentityKey.builder()
                                                                               .bucket("bucket")
                                                                               .client(mockS3)
                                                                               .identity(mockIdentity)
                                                                               .build())
                                                      .staleTime(Duration.ZERO)
                                                      .prefetchTime(Duration.ZERO)
                                                      .build();
    }

    @AfterEach
    public void teardown() {
        cachedCredentials.close();
    }

    @Test
    void cachedCredentials_cachesResult() {
        Mockito.when(mockCredentialsSupplier.apply(any())).thenReturn(createSessionCredentials(Instant.now().plus(10, HOURS)));

        cachedCredentials.get();
        cachedCredentials.get();
        cachedCredentials.get();
        Mockito.verify(mockCredentialsSupplier, times(1)).apply(any());
    }

    @Test
    void cachedCredentials_doesAsyncRefresh() throws InterruptedException {
        Mockito.when(mockCredentialsSupplier.apply(any()))
               .thenAnswer(i -> createSessionCredentials(Instant.now().plusSeconds(1).plusMillis(100)));

        cachedCredentials.get();

        waitAndVerifySupplierCalled(atLeast(2));
        assertThat(cachedCredentials.prefetchStrategy().isTaskScheduled()).isTrue();
    }

    @Test
    void cachedCredentials_doesNotAsyncRefreshOnFailure() throws InterruptedException {
        Mockito.when(mockCredentialsSupplier.apply(any()))
               .thenReturn(createSessionCredentials(Instant.now().plusSeconds(1).plusMillis(100)));

        cachedCredentials.get();
        Mockito.when(mockCredentialsSupplier.apply(any())).thenThrow(S3Exception.builder().build());

        waitAndVerifySupplierCalled(times(2));
        assertThat(cachedCredentials.prefetchStrategy().isTaskScheduled()).isFalse();
    }

    private void waitAndVerifySupplierCalled(VerificationMode mode) throws InterruptedException {
        boolean backgroundRefreshComplete = false;
        Instant waitEnd = Instant.now().plus(10, SECONDS);
        while (Instant.now().isBefore(waitEnd)) {
            try {
                Mockito.verify(mockCredentialsSupplier, mode).apply(any());
                backgroundRefreshComplete = true;
                break;
            } catch (TooFewActualInvocations e) {
                Thread.sleep(100);
            }
        }

        assertThat(backgroundRefreshComplete).isTrue();
    }

    private static SessionCredentials createSessionCredentials(Instant credentialExpiration) {
        return SessionCredentials.builder()
                                 .accessKeyId("akid")
                                 .secretAccessKey("skid")
                                 .sessionToken("stok")
                                 .expiration(credentialExpiration)
                                 .build();
    }
}