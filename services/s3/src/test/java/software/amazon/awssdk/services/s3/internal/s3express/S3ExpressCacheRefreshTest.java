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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.s3.model.CreateSessionResponse;
import software.amazon.awssdk.services.s3.model.SessionCredentials;


@ExtendWith(MockitoExtension.class)
class S3ExpressCacheRefreshTest {

    private static final String ACCESS_KEY = "accessKeyId";
    private static final String SECRET_KEY = "secretAccessKey";
    private static final String SESSION_TOKEN = "sessionToken";
    private static final S3ExpressIdentityKey KEY = S3ExpressIdentityKey.builder()
                                                                        .bucket("Bucket-1")
                                                                        .client(mock(SdkClient.class))
                                                                        .identity(mock(AwsCredentialsIdentity.class))
                                                                        .build();

    @Mock
    Function<S3ExpressIdentityKey, SessionCredentials> identitySupplier;

    @BeforeEach
    public void methodSetup() {

    }

    @Test
    void when_supplierIsAccessedMultipleTimesWithinExpirationTime_NoExtraCallsAreMade() {
        when(identitySupplier.apply(any())).thenAnswer(invocation -> {
            CreateSessionResponse sessionResponse = createSessionResponse(1, Instant.now().plus(Duration.ofSeconds(5)));
            return sessionResponse.credentials();
        });

        CachedS3ExpressCredentials cache = CachedS3ExpressCredentials.builder(identitySupplier)
                                                                     .key(KEY)
                                                                     .prefetchTime(Duration.ofSeconds(1))
                                                                     .staleTime(Duration.ofMillis(200))
                                                                     .build();

        SessionCredentials credentials;
        credentials = cache.get();
        credentials = cache.get();

        verify(identitySupplier, times(1)).apply(KEY);
        verifyCredentialsInSequence(credentials, 1);
    }

    @Test
    void when_credentialsReachPrefetchRange_credentialsAreRefreshed() throws InterruptedException {
        when(identitySupplier.apply(KEY)).thenAnswer(new Answer<SessionCredentials>() {
            private int i = 0;
            @Override
            public SessionCredentials answer(InvocationOnMock invocation) {
                i++;
                CreateSessionResponse sessionResponse = createSessionResponse(i, Instant.now().plus(Duration.ofSeconds(10)));
                return sessionResponse.credentials();
            }
        });

        CachedS3ExpressCredentials cache = CachedS3ExpressCredentials.builder(identitySupplier)
                                                                         .key(KEY)
                                                                         .prefetchTime(Duration.ofSeconds(2))
                                                                         .staleTime(Duration.ofMillis(100))
                                                                         .build();


        List<SessionCredentials> sessionCredentials = new ArrayList<>();
        sessionCredentials.add(cache.get());
        Thread.sleep(1 * 1000);
        sessionCredentials.add(cache.get());
        Thread.sleep(10 * 1000);
        sessionCredentials.add(cache.get());
        Thread.sleep(2 * 1000);
        sessionCredentials.add(cache.get());

        verify(identitySupplier, times(2)).apply(KEY);

        String firstCredentialAccessKey = stringWithSequenceNumber(ACCESS_KEY, 1);
        String secondCredentialAccessKey = stringWithSequenceNumber(ACCESS_KEY, 2);
        assertThat(sessionCredentials.get(0).accessKeyId()).isEqualTo(firstCredentialAccessKey);
        assertThat(sessionCredentials.get(1).accessKeyId()).isEqualTo(firstCredentialAccessKey);
        assertThat(sessionCredentials.get(2).accessKeyId()).isEqualTo(secondCredentialAccessKey);
        assertThat(sessionCredentials.get(3).accessKeyId()).isEqualTo(secondCredentialAccessKey);
    }

    @Test
    void credentials_getRefreshedMultipleTimes() throws InterruptedException {
        when(identitySupplier.apply(KEY)).thenAnswer(new Answer<SessionCredentials>() {
            private int sequenceNumber = 0;
            @Override
            public SessionCredentials answer(InvocationOnMock invocation) {
                sequenceNumber++;
                CreateSessionResponse sessionResponse = createSessionResponse(sequenceNumber,
                                                                              Instant.now().plus(Duration.ofSeconds(1)));
                return sessionResponse.credentials();
            }
        });
        CachedS3ExpressCredentials cache = CachedS3ExpressCredentials.builder(identitySupplier)
                                                                     .key(KEY)
                                                                     .prefetchTime(Duration.ofMillis(200))
                                                                     .staleTime(Duration.ofMillis(40))
                                                                     .build();
        SessionCredentials sessionCredentials = null;
        int numGets = 20;
        int minimumRefreshesExpectedWithMargin = 15;;

        for (int i = 0; i < numGets; i++) {
            sessionCredentials = cache.get();
            Thread.sleep(1000);
        }
        assertThat(sessionCredentials).isNotNull();
        int responseSequenceNumber = parseSequenceNumber(sessionCredentials.accessKeyId());
        assertThat(responseSequenceNumber).isGreaterThan(minimumRefreshesExpectedWithMargin);
    }

    @Test
    void when_supplierThrowsException_ExceptionIsPropagated() {
        when(identitySupplier.apply(any())).thenAnswer(invocation -> {
            throw new RuntimeException("Oops");
        });

        CachedS3ExpressCredentials cache = CachedS3ExpressCredentials.builder(identitySupplier).key(KEY).build();

        assertThatThrownBy(() -> cache.get()).hasMessage("Oops");
    }

    private void verifyCredentialsInSequence(SessionCredentials actualCredentials, int sequenceNumber) {
        assertThat(actualCredentials.accessKeyId()).isEqualTo(stringWithSequenceNumber(ACCESS_KEY, sequenceNumber));
        assertThat(actualCredentials.secretAccessKey()).isEqualTo(stringWithSequenceNumber(SECRET_KEY, sequenceNumber));
        assertThat(actualCredentials.sessionToken()).isEqualTo(stringWithSequenceNumber(SESSION_TOKEN, sequenceNumber));
    }

    private static CreateSessionResponse createSessionResponse(int sequenceNumber, Instant expires) {
        return CreateSessionResponse.builder()
                                    .credentials(SessionCredentials.builder()
                                                                  .accessKeyId(stringWithSequenceNumber(ACCESS_KEY, sequenceNumber))
                                                                  .secretAccessKey(stringWithSequenceNumber(SECRET_KEY, sequenceNumber))
                                                                  .sessionToken(stringWithSequenceNumber(SESSION_TOKEN, sequenceNumber))
                                                                  .expiration(expires)
                                                                  .build())
                                    .build();
    }

    private static String stringWithSequenceNumber(String value, int sequenceNumber) {
        return value + "_" + sequenceNumber;
    }

    private Integer parseSequenceNumber(String s) {
        int sequenceNumIndex = s.lastIndexOf('_');
        String substring = s.substring(sequenceNumIndex + 1);
        return Integer.parseInt(substring);
    }
}