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

package software.amazon.awssdk.services.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.restjsonwithwaiters.RestJsonWithWaitersAsyncClient;
import software.amazon.awssdk.services.restjsonwithwaiters.model.AllTypesRequest;
import software.amazon.awssdk.services.restjsonwithwaiters.model.AllTypesResponse;
import software.amazon.awssdk.services.restjsonwithwaiters.model.EmptyModeledException;
import software.amazon.awssdk.services.restjsonwithwaiters.waiters.RestJsonWithWaitersAsyncWaiter;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class WaitersAsyncFunctionalTest {

    public RestJsonWithWaitersAsyncClient asyncClient;
    public RestJsonWithWaitersAsyncWaiter asyncWaiter;

    @Before
    public void setup() {
        asyncClient = mock(RestJsonWithWaitersAsyncClient.class);
        asyncWaiter = RestJsonWithWaitersAsyncWaiter.builder()
                                                    .client(asyncClient)
                                                    .overrideConfiguration(WaiterOverrideConfiguration.builder()
                                                                                                      .maxAttempts(3)
                                                                                                      .backoffStrategy(BackoffStrategy.none())
                                                                                                      .build())
                                                    .build();
    }

    @After
    public void cleanup() {
        asyncClient.close();
        asyncWaiter.close();
    }

    @Test
    public void allTypeOperation_withAsyncWaiter_shouldReturnResponse() throws ExecutionException, InterruptedException {
        AllTypesResponse response = (AllTypesResponse) AllTypesResponse.builder()
                                                                       .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                       .statusCode(200)
                                                                                                       .build())
                                                                       .build();

        CompletableFuture<AllTypesResponse> serviceFuture = new CompletableFuture<>();


        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(serviceFuture);
        CompletableFuture<WaiterResponse<AllTypesResponse>> responseFuture = asyncWaiter.waitUntilAllTypesSuccess(AllTypesRequest.builder()
                                                                                                                                 .integerMember(1)
                                                                                                                                 .build());
        serviceFuture.complete(response);

        assertThat(responseFuture.get().attemptsExecuted()).isEqualTo(1);
        assertThat(responseFuture.get().matched().response()).hasValueSatisfying(r -> assertThat(r).isEqualTo(response));
    }

    @Test
    public void allTypeOperationFailed_withAsyncWaiter_shouldReturnException() throws ExecutionException, InterruptedException {
        CompletableFuture<AllTypesResponse> serviceFuture = new CompletableFuture<>();

        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(serviceFuture);
        CompletableFuture<WaiterResponse<AllTypesResponse>> responseFuture = asyncWaiter.waitUntilAllTypesSuccess(AllTypesRequest.builder().build());

        serviceFuture.completeExceptionally(SdkServiceException.builder().statusCode(200).build());

        assertThat(responseFuture.get().attemptsExecuted()).isEqualTo(1);
        assertThat(responseFuture.get().matched().exception()).hasValueSatisfying(r -> assertThat(r).isInstanceOf(SdkServiceException.class));
    }

    @Test
    public void allTypeOperationRetry_withAsyncWaiter_shouldReturnResponseAfterException() throws ExecutionException, InterruptedException {
        AllTypesResponse response2 = (AllTypesResponse) AllTypesResponse.builder()
                                                                        .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                        .statusCode(200)
                                                                                                        .build())
                                                                        .build();

        CompletableFuture<AllTypesResponse> serviceFuture1 =
            CompletableFutureUtils.failedFuture(SdkServiceException.builder().statusCode(404).build());
        CompletableFuture<AllTypesResponse> serviceFuture2 = new CompletableFuture<>();

        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(serviceFuture1, serviceFuture2);

        CompletableFuture<WaiterResponse<AllTypesResponse>> responseFuture = asyncWaiter.waitUntilAllTypesSuccess(AllTypesRequest.builder().build());

        serviceFuture2.complete(response2);

        assertThat(responseFuture.get().attemptsExecuted()).isEqualTo(2);
        assertThat(responseFuture.get().matched().response()).hasValueSatisfying(r -> assertThat(r).isEqualTo(response2));
    }

    @Test
    public void requestOverrideConfig_shouldTakePrecedence() {
        AllTypesResponse response = (AllTypesResponse) AllTypesResponse.builder()
                                                                        .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                        .statusCode(200)
                                                                                                        .build())
                                                                        .build();

        CompletableFuture<AllTypesResponse> serviceFuture1 = CompletableFutureUtils.failedFuture(SdkServiceException.builder().statusCode(404).build());
        CompletableFuture<AllTypesResponse> serviceFuture2 =  CompletableFuture.completedFuture(response);

        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(serviceFuture1, serviceFuture2);

        assertThatThrownBy(() ->
            asyncWaiter.waitUntilAllTypesSuccess(b -> b.build(), o -> o.maxAttempts(1)).join())
            .hasMessageContaining("exceeded the max retry attempts").hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    public void unexpectedException_shouldNotRetry() {
        CompletableFuture<AllTypesResponse> failedFuture = CompletableFutureUtils.failedFuture(new RuntimeException(""));
        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(failedFuture);

        assertThatThrownBy(() -> asyncWaiter.waitUntilAllTypesSuccess(b -> b.build()).join())
            .hasMessageContaining("An exception was thrown and did not match any waiter acceptors")
            .hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    public void unexpectedResponse_shouldRetry() {
        AllTypesResponse response = (AllTypesResponse) AllTypesResponse.builder()
                                                                    .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                    .statusCode(200)
                                                                                                    .build()).build();
        CompletableFuture<AllTypesResponse> future1 =
            CompletableFuture.completedFuture((AllTypesResponse) AllTypesResponse.builder()
                                                                                 .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                                 .statusCode(202)
                                                                                                                 .build()).build());

        CompletableFuture<AllTypesResponse> future2 =
            CompletableFuture.completedFuture(response);

        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(future1, future2);

        WaiterResponse<AllTypesResponse> waiterResponse = asyncWaiter.waitUntilAllTypesSuccess(b -> b.build()).join();
        assertThat(waiterResponse.attemptsExecuted()).isEqualTo(2);
        assertThat(waiterResponse.matched().response()).hasValueSatisfying(r -> assertThat(r).isEqualTo(response));
    }

    @Test
    public void failureResponse_shouldThrowException() {
        CompletableFuture<AllTypesResponse> future =
            CompletableFuture.completedFuture((AllTypesResponse) AllTypesResponse.builder()
                                                                                 .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                                 .statusCode(500)
                                                                                                                 .build())
                                                                                 .build());
        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(future);
        assertThatThrownBy(() -> asyncWaiter.waitUntilAllTypesSuccess(SdkBuilder::build).join())
            .hasMessageContaining("transitioned the waiter to failure state")
            .hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    public void failureException_shouldThrowException() {
        when(asyncClient.allTypes(any(AllTypesRequest.class))).thenReturn(CompletableFutureUtils.failedFuture(EmptyModeledException.builder()
                                                                                                                                   .awsErrorDetails(AwsErrorDetails.builder()
                                                                                                                                                                   .errorCode("EmptyModeledException")
                                                                                                                                                                   .build())
                                                                                                                                   .build()));
        assertThatThrownBy(() -> asyncWaiter.waitUntilAllTypesSuccess(SdkBuilder::build).join())
            .hasMessageContaining("transitioned the waiter to failure state")
            .hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    public void closeWaiterCreatedWithClient_clientDoesNotClose() {
        asyncWaiter.close();
        verify(asyncClient, never()).close();
    }

    @Test
    public void closeWaiterCreatedWithExecutorService_executorServiceDoesNotClose() {
        ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
        RestJsonWithWaitersAsyncWaiter newWaiter = RestJsonWithWaitersAsyncWaiter.builder()
                                                                                 .scheduledExecutorService(executorService)
                                                                                 .overrideConfiguration(WaiterOverrideConfiguration.builder()
                                                                                                                 .maxAttempts(3)
                                                                                                                 .backoffStrategy(BackoffStrategy.none())
                                                                                                                 .build())
                                                                                 .build();

        newWaiter.close();
        verify(executorService, never()).shutdown();
    }
}
