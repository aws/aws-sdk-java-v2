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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.awscore.util.TestWrapperSchedulerService;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

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
        AwsToken firstRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(firstRefreshToken).isEqualTo(token1);
        AwsToken secondRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
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

        AwsToken firstRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
        assertThat(firstRefreshToken).isEqualTo(token1);
        AwsToken secondRefreshToken = tokenRefresher.refreshIfStaleAndFetch();
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
                                                                  .enableAutoFetch(true)
                                                                  .build();

        Thread.sleep(400);
        verify(testTokenSupplier, atMost(3)).get();
        tokenRefresher.close();
    }


    @Test
    public void autoRefreshWithDelayValues_when_tokenIsOfDifferentExpiryRange() throws InterruptedException {
        ScheduledExecutorService scheduler = spy(new TestWrapperSchedulerService(
            Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().threadNamePrefix("test-Async-refresher").build())));
        TokenManager<TestToken> tokenManager = mock(TokenManager.class);
        Instant fixedInstant = Instant.parse("2021-12-25T13:30:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        Supplier<TestToken> mockTokenManager = () -> tokenManager.loadToken().get();
        Supplier<TestToken> mockCache = mock(Supplier.class);
        TestToken expiredToken =
            TestToken.builder().token("token1").expirationDate(Instant.parse("2021-12-25T13:30:00Z")).build();
        Function<Long, Instant> offSetFunction = offset -> Instant.ofEpochMilli(fixedInstant.toEpochMilli() + offset);
        TestToken tokenJustAboutToExpire =
            TestToken.builder().token("tokenJustAboutToExpire").expirationDate(offSetFunction.apply(200L)).build();
        TestToken tokenFarAwayFromExpiry =
            TestToken.builder().token("tokenFarAwayFromExpiry").expirationDate(offSetFunction.apply(500L)).build();
        TestToken tokenVeryFarAwayFromExpiry =
            TestToken.builder().token("tokenVeryFarAwayFromExpiry").expirationDate(offSetFunction.apply(Duration.ofDays(1).toMillis())).build();
        when(mockCache.get()).thenReturn(expiredToken)
                             .thenReturn(tokenJustAboutToExpire)
                             .thenReturn(tokenFarAwayFromExpiry)
                             .thenReturn(tokenVeryFarAwayFromExpiry);
        CachedTokenRefresher tokenRefresher = new CachedTokenRefresher(
            mockTokenManager, Duration.ofMillis(200), null, scheduler, mockCache,
            fixedClock, true);

        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        Thread.sleep(2000);
        tokenRefresher.close();

        verify(scheduler, atMost(5)).schedule(any(Runnable.class), longCaptor.capture(), any(TimeUnit.class));
        List<Long> delayedStartTime = longCaptor.getAllValues();

        assertThat(delayedStartTime.get(0)).isEqualTo(200);
        // First Token expired so instead of immediately refreshing the token it waits for the stale time
        assertThat(delayedStartTime.get(1)).isEqualTo(200);
        // Second Token expiry time is equal to stale time, so it delays for stale time.
        assertThat(delayedStartTime.get(2)).isEqualTo(200);
        // Third Token is 500ms more to expiry while we have stale time of 200ms this makes it look ahead and post pone the
        // call by 300Ms
        assertThat(delayedStartTime.get(3)).isEqualTo(300);
        // Fourth token has 1 day expiration time thus the auto refresh will be scheduled after 1 day minus stale time (200Ms)
        assertThat(delayedStartTime.get(4)).isEqualTo(86399800);
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
