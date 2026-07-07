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

package software.amazon.awssdk.services.s3.internal.crossregion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

public class Expect100ContinueTest {
    private static final AwsCredentialsProvider TEST_CREDS = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("akid", "skid"));
    private SdkHttpClient mockSyncHttp;
    private SdkAsyncHttpClient mockAsyncHttp;

    @BeforeEach
    void setup() {
        mockSyncHttp = mock(SdkHttpClient.class);
        when(mockSyncHttp.prepareRequest(any(HttpExecuteRequest.class))).thenThrow(new RuntimeException("expect 100 continue"));

        mockAsyncHttp = mock(SdkAsyncHttpClient.class);
        CompletableFuture cf = new CompletableFuture();
        cf.completeExceptionally(new RuntimeException("expect 100 continue"));
        when(mockAsyncHttp.execute(any(AsyncExecuteRequest.class))).thenAnswer(i -> {
            AsyncExecuteRequest req = i.getArgument(0);
            SdkAsyncHttpResponseHandler handler = req.responseHandler();
            handler.onError(new RuntimeException("expect 100 continue"));
            return CompletableFuture.completedFuture(null);
        });
    }

    @ParameterizedTest(name = "expect 100-continue enabled = {0}")
    @CsvSource({"true", "false"})
    void sync_alwaysAdds(boolean enabled) {

        try (S3Client s3 = S3Client.builder()
                                   .httpClient(mockSyncHttp)
                                   .region(Region.US_WEST_2)
                                   .credentialsProvider(TEST_CREDS)
                                   .crossRegionAccessEnabled(true)
                                   .serviceConfiguration(o -> o.expectContinueEnabled(enabled)
                                                               .expectContinueThresholdInBytes(1L))
                                   .build()) {
            RequestBody requestBody = RequestBody.fromBytes(new byte[16]);
            assertThatThrownBy(() -> s3.putObject(o -> o.bucket("bucket").key("key"), requestBody))
                .hasMessage("expect 100 continue");

            ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

            verify(mockSyncHttp).prepareRequest(requestCaptor.capture());
            assertHasExpect100Continue(requestCaptor.getValue().httpRequest());
        }
    }

    @ParameterizedTest(name = "expect 100-continue enabled = {0}")
    @CsvSource({"true", "false"})
    void async_alwaysAdds(boolean enabled) {
        try (S3AsyncClient s3 = S3AsyncClient.builder()
                                        .httpClient(mockAsyncHttp)
                                        .region(Region.US_WEST_2)
                                        .credentialsProvider(TEST_CREDS)
                                        .crossRegionAccessEnabled(true)
                                        .serviceConfiguration(o -> o.expectContinueEnabled(enabled)
                                                               .expectContinueThresholdInBytes(1L))
                                        .build()) {
            AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(new byte[16]);
            assertThatThrownBy(s3.putObject(o -> o.bucket("bucket").key("key"), requestBody)::join)
                .hasMessageContaining("expect 100 continue");

            ArgumentCaptor<AsyncExecuteRequest> requestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

            verify(mockAsyncHttp).execute(requestCaptor.capture());
            assertHasExpect100Continue(requestCaptor.getValue().request());
        }
    }

    private static void assertHasExpect100Continue(SdkHttpRequest httpRequest) {
        assertThat(httpRequest.firstMatchingHeader("Expect"))
            .hasValueSatisfying(v -> assertThat(v).isEqualToIgnoringCase("100-continue"));
    }
}
