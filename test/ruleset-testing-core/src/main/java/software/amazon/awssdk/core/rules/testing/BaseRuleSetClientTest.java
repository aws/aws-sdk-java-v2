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

package software.amazon.awssdk.core.rules.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public abstract class BaseRuleSetClientTest {
    protected static final AwsCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    protected static final SdkTokenProvider TOKEN_PROVIDER = StaticTokenProvider.create(new TestSdkToken());

    private static SdkHttpClient syncHttpClient;
    private static SdkAsyncHttpClient asyncHttpClient;

    @BeforeAll
    public static void setup() {
        syncHttpClient = mock(SdkHttpClient.class);
        asyncHttpClient = mock(SdkAsyncHttpClient.class);
    }

    @BeforeEach
    public void methodSetup() {
        reset(syncHttpClient, asyncHttpClient);
        when(syncHttpClient.prepareRequest(any())).thenThrow(new RuntimeException("Oops"));

        when(asyncHttpClient.execute(any())).thenAnswer(i -> {
            AsyncExecuteRequest req = i.getArgument(0, AsyncExecuteRequest.class);
            SdkAsyncHttpResponseHandler responseHandler = req.responseHandler();

            responseHandler.onError(new RuntimeException("Oops"));
            return CompletableFutureUtils.failedFuture(new RuntimeException("Something went wrong"));
        });
    }

    protected static void runAndVerify(SyncTestCase testCase) {
        String skipReason = testCase.skipReason();
        Assumptions.assumeTrue(skipReason == null, skipReason);
        Expect expectation = testCase.expectation();
        Runnable r = testCase.operationRunnable();

        if (expectation.error() != null) {
            assertThatThrownBy(r::run).hasMessageContaining(expectation.error());
        } else {
            assertThatThrownBy(r::run).hasMessageContaining("Oops");

            ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
            verify(syncHttpClient).prepareRequest(requestCaptor.capture());

            URI requestUri = requestCaptor.getValue().httpRequest().getUri();
            Endpoint expectedEndpoint = expectation.endpoint();

            assertThat(requestUri.getScheme()).isEqualTo(expectedEndpoint.url().getScheme());
            assertThat(requestUri.getHost()).isEqualTo(expectedEndpoint.url().getHost());
            assertThat(requestUri.getRawPath()).startsWith(expectedEndpoint.url().getRawPath());
        }
    }

    protected static void runAndVerify(AsyncTestCase testCase) {
        String skipReason = testCase.skipReason();
        Assumptions.assumeTrue(skipReason == null, skipReason);
        Expect expectation = testCase.expectation();
        Supplier<CompletableFuture<?>> r = testCase.operationRunnable();

        CompletableFuture<?> executeFuture = r.get();
        if (expectation.error() != null) {
            assertThatThrownBy(executeFuture::get).hasMessageContaining(expectation.error());
        } else {
            assertThatThrownBy(executeFuture::get).hasMessageContaining("Oops");

            ArgumentCaptor<AsyncExecuteRequest> requestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);
            verify(asyncHttpClient).execute(requestCaptor.capture());

            URI requestUri = requestCaptor.getValue().request().getUri();
            Endpoint expectedEndpoint = expectation.endpoint();

            assertThat(requestUri.getScheme()).isEqualTo(expectedEndpoint.url().getScheme());
            assertThat(requestUri.getHost()).isEqualTo(expectedEndpoint.url().getHost());
        }
    }

    protected static SdkHttpClient getSyncHttpClient() {
        return syncHttpClient;
    }

    protected static SdkAsyncHttpClient getAsyncHttpClient() {
        return asyncHttpClient;
    }

    private static class TestSdkToken implements SdkToken {

        @Override
        public String token() {
            return "TOKEN";
        }

        @Override
        public Optional<Instant> expirationTime() {
            return Optional.empty();
        }
    }
}
