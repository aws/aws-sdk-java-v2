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

package software.amazon.awssdk.awscore.internal.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;

public class CachedTokenRefresherTest {

    @Test
    public void doNotRefresh_when_valueIsNotStale() {

        Supplier<TestToken> supplier = mock(Supplier.class);
        TestToken token1 =
            TestToken.builder().token("token1").expirationDate(Instant.now().plus(Duration.ofMillis(10000))).build();
        TestToken token2 =
            TestToken.builder().token("token2").expirationDate(Instant.now().plus(Duration.ofMillis(900))).build();
        when(supplier.get()).thenReturn(token1)
                            .thenReturn(token2);

        CachedTokenRefresher tokenRefresher = tokenRefresherBuilder()
            .staleDuration(Duration.ofMillis(99))
            .tokenRetriever(supplier)
            .build();
        SdkToken firstRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(firstRefreshToken).isEqualTo(token1);
        SdkToken secondRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(secondRefreshToken).isEqualTo(token1);
    }

    @Test
    public void refresh_when_valueIsStale() {

        Supplier<TestToken> supplier = mock(Supplier.class);

        TestToken token1 =
            TestToken.builder().token("token1").expirationDate(Instant.now().minus(Duration.ofMillis(1))).build();
        TestToken token2 =
            TestToken.builder().token("token2").expirationDate(Instant.now().plus(Duration.ofMillis(900))).build();
        when(supplier.get()).thenReturn(token1)
                            .thenReturn(token2);

        CachedTokenRefresher tokenRefresher = tokenRefresherBuilder()
            .staleDuration(Duration.ofMillis(99))
            .tokenRetriever(supplier)
            .build();

        SdkToken firstRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(firstRefreshToken).isEqualTo(token1);
        SdkToken secondRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(secondRefreshToken).isEqualTo(token2);
    }

    @Test
    public void refreshTokenFails_when_exceptionHandlerThrowsBackException() {

        Function<RuntimeException, TestToken> fun = e -> {
            throw e;
        };
        CachedTokenRefresher tokenRefresher = tokenRefresherBuilder()
            .staleDuration(Duration.ofMillis(101))
            .exceptionHandler(fun)
            .tokenRetriever(() -> {
                throw SdkException.create("Auth Failure", SdkClientException.create("Error"));
            })
            .build();

        assertThatExceptionOfType(SdkException.class)
            .isThrownBy(() -> tokenRefresher.refreshIfStaleAndFetch()).withMessage("Auth Failure");
    }

    @Test
    public void refreshTokenPasses_when_exceptionHandlerSkipsException() throws ExecutionException, InterruptedException {
        TestToken initialToken = getTestTokenBuilder().token("OldToken").expirationDate(Instant.now()).build();
        CachedTokenRefresher<TestToken> tokenRefresher = tokenRefresherBuilder()
            .staleDuration(Duration.ofMillis(101))
            .exceptionHandler(e -> initialToken)
            .tokenRetriever(() -> {
                throw SdkException.create("Auth Failure", SdkClientException.create("Error"));
            })
            .build();

        TestToken testToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(testToken).isEqualTo(initialToken);
        assertThat(testToken.token()).isEqualTo("OldToken");
        assertThat(initialToken).isEqualTo(testToken);
    }

    @Test
    public void refreshTokenFails_when_baseTokenIsNotInTokenManagerWhileTokenFailedToObtainFromService() {

        Function<RuntimeException, TestToken> handleException = e -> {
            // handle Exception throws back another exception while handling the exception.
            throw new IllegalStateException("Unable to load token from Disc");
        };

        CachedTokenRefresher tokenRefresher = tokenRefresherBuilder()
            .staleDuration(Duration.ofMillis(101))
            .exceptionHandler(handleException)
            .tokenRetriever(() -> {
                throw SdkException.create("Auth Failure", SdkClientException.create("Error"));
            })
            .build();
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> tokenRefresher.refreshIfStaleAndFetch()).withMessage("Unable to load token from Disc");
    }

    @Test
    public void autoRefresheToken_when_tokensExpire() throws InterruptedException {
        Supplier<TestToken> testTokenSupplier = mock(Supplier.class);
        TestToken token1 =
            TestToken.builder().token("token1").expirationDate(Instant.now().minus(Duration.ofMillis(10000))).build();
        TestToken token3 =
            TestToken.builder().token("token3").expirationDate(Instant.now().plus(Duration.ofDays(1))).build();
        when(testTokenSupplier.get()).thenReturn(token1).thenReturn(token1).thenReturn(token3);
        CachedTokenRefresher tokenRefresher = CachedTokenRefresher.builder()
                                                                  .tokenRetriever(testTokenSupplier).
                                                                  staleDuration(Duration.ofMillis(100))
                                                                  .prefetchTime(Duration.ofMillis(110))
                                                                  .build();
        Thread.sleep(400);
        verify(testTokenSupplier, atMost(3)).get();
    }

    @Test
    public void prefetchToken_whenTokenNotStale_and_withinPrefetchTime() throws InterruptedException {
        Supplier<TestToken> mockCache = mock(Supplier.class);
        Instant startInstance = Instant.now();

        TestToken firstToken =
            TestToken.builder().token("firstToken").expirationDate(startInstance.plusSeconds(1)).build();
        TestToken secondToken =
            TestToken.builder().token("secondTokenWithinStaleTime").expirationDate(startInstance.plusSeconds(1)).build();
        TestToken thirdToken =
            TestToken.builder().token("thirdTokenOutsidePrefetchTime").expirationDate(startInstance.plusMillis(1500)).build();
        TestToken fourthToken =
            TestToken.builder().token("thirdTokenOutsidePrefetchTime").expirationDate(Instant.now().plusSeconds(6000)).build();


        when(mockCache.get()).thenReturn(firstToken)
                             .thenReturn(secondToken)
                             .thenReturn(thirdToken)
                             .thenReturn(fourthToken);

        CachedTokenRefresher cachedTokenRefresher = CachedTokenRefresher.builder()
                                                                        .asyncRefreshEnabled(true)
                                                                        .staleDuration(Duration.ofMillis(1000))
                                                                        .tokenRetriever(mockCache)
                                                                        .prefetchTime(Duration.ofMillis(1500))
                                                                        .build();

        // Sleep is invoked to make sure executor executes refresh in initializeCachedSupplier() in NonBlocking CachedSupplier.PrefetchStrategy
        Thread.sleep(1000);
        verify(mockCache, times(0)).get();
        SdkToken firstRetrieved = cachedTokenRefresher.refreshIfStaleAndFetch();
        assertThat(firstRetrieved).isEqualTo(firstToken);

        Thread.sleep(1000);
        // Sleep to make sure the Async prefetch thread is picked up
        verify(mockCache, times(1)).get();
        SdkToken secondRetrieved = cachedTokenRefresher.refreshIfStaleAndFetch();
        // Note that since the token has already been prefetched mockCache.get() is not called again thus it is secondToken.
        assertThat(secondRetrieved).isEqualTo(secondToken);

        Thread.sleep(1000);
        // Sleep to make sure the Async prefetch thread is picked up
        verify(mockCache, times(2)).get();
        SdkToken thirdRetrievedToken = cachedTokenRefresher.refreshIfStaleAndFetch();
        assertThat(thirdRetrievedToken).isEqualTo(thirdToken);

        // Sleep to make sure the Async prefetch thread is picked up
        Thread.sleep(1000);
        verify(mockCache, times(3)).get();
        SdkToken fourthRetrievedToken = cachedTokenRefresher.refreshIfStaleAndFetch();
        assertThat(fourthRetrievedToken).isEqualTo(fourthToken);

        // Sleep to make sure the Async prefetch thread is picked up
        Thread.sleep(1000);
        verify(mockCache, times(4)).get();
        SdkToken fifthToken = cachedTokenRefresher.refreshIfStaleAndFetch();
        // Note that since Fourth token's expiry date is too high the prefetch is no longer done and the last fetch token is used.
        verify(mockCache, times(4)).get();
        assertThat(fifthToken).isEqualTo(fourthToken);
    }

    @Test
    public void refreshEveryTime_when_ExpirationDateDoesNotExist() throws InterruptedException {

        Supplier<TestToken> supplier = mock(Supplier.class);

        TestToken token1 = TestToken.builder().token("token1").build();
        TestToken token2 = TestToken.builder().token("token2").build();
        when(supplier.get()).thenReturn(token1).thenReturn(token2);


        CachedTokenRefresher tokenRefresher = tokenRefresherBuilder().tokenRetriever(supplier).build();

        SdkToken firstRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(firstRefreshToken).isEqualTo(token1);
        Thread.sleep(1000);
        SdkToken secondRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(secondRefreshToken).isEqualTo(token2);
    }

    private TestAwsResponse.Builder getDefaultTestAwsResponseBuilder() {
        return TestAwsResponse.builder().accessToken("serviceToken")
                              .expiryTime(Instant.ofEpochMilli(1743680000000L)).startUrl("new_start_url");
    }

    private CachedTokenRefresher.Builder tokenRefresherBuilder() {
        return CachedTokenRefresher.builder();
    }

    private TestToken.Builder getTestTokenBuilder() {
        return TestToken.builder().token("sampleToken")
                        .start_url("start_url");
    }
}