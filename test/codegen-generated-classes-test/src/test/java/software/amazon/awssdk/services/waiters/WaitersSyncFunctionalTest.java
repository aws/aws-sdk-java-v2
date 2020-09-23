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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.restjsonwithwaiters.RestJsonWithWaitersClient;
import software.amazon.awssdk.services.restjsonwithwaiters.model.AllTypesRequest;
import software.amazon.awssdk.services.restjsonwithwaiters.model.AllTypesResponse;
import software.amazon.awssdk.services.restjsonwithwaiters.waiters.RestJsonWithWaitersWaiter;

public class WaitersSyncFunctionalTest {

    private RestJsonWithWaitersClient client;
    private RestJsonWithWaitersWaiter waiter;

    @Before
    public void setup() {
        client = mock(RestJsonWithWaitersClient.class);
        waiter = RestJsonWithWaitersWaiter.builder()
                                          .client(client)
                                          .overrideConfiguration(WaiterOverrideConfiguration.builder()
                                                                                            .maxAttempts(3)
                                                                                            .backoffStrategy(BackoffStrategy.none())
                                                                                            .build())
                                          .build();
    }

    @After
    public void cleanup() {
        client.close();
        waiter.close();
    }

    @Test
    public void allTypeOperation_withSyncWaiter_shouldReturnResponse() {
        AllTypesResponse response = (AllTypesResponse) AllTypesResponse.builder()
                                                                       .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                       .statusCode(200)
                                                                                                       .build())
                                                                       .build();


        when(client.allTypes(any(AllTypesRequest.class))).thenReturn(response);
        WaiterResponse<AllTypesResponse> waiterResponse = waiter.waitUntilAllTypesSuccess(AllTypesRequest.builder().build());

        assertThat(waiterResponse.attemptsExecuted()).isEqualTo(1);
        assertThat(waiterResponse.matched().response()).hasValueSatisfying(r -> assertThat(r).isEqualTo(response));
    }

    @Test
    public void allTypeOperationFailed_withSyncWaiter_shouldReturnException() {
        when(client.allTypes(any(AllTypesRequest.class))).thenThrow(SdkServiceException.builder().statusCode(200).build());

        WaiterResponse<AllTypesResponse> waiterResponse = waiter.waitUntilAllTypesSuccess(AllTypesRequest.builder().build());

        assertThat(waiterResponse.attemptsExecuted()).isEqualTo(1);
        assertThat(waiterResponse.matched().exception()).hasValueSatisfying(r -> assertThat(r).isInstanceOf(SdkServiceException.class));
    }

    @Test
    public void allTypeOperationRetry_withSyncWaiter_shouldReturnResponseAfterException() {
        AllTypesResponse response1 = (AllTypesResponse) AllTypesResponse.builder()
                                                                        .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                        .statusCode(404)
                                                                                                        .build())
                                                                        .build();
        AllTypesResponse response2 = (AllTypesResponse) AllTypesResponse.builder()
                                                                        .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                        .statusCode(200)
                                                                                                        .build())
                                                                        .build();


        when(client.allTypes(any(AllTypesRequest.class))).thenReturn(response1, response2);

        WaiterResponse<AllTypesResponse> waiterResponse = waiter.waitUntilAllTypesSuccess(AllTypesRequest.builder().build());

        assertThat(waiterResponse.attemptsExecuted()).isEqualTo(2);
        assertThat(waiterResponse.matched().response()).hasValueSatisfying(r -> assertThat(r).isEqualTo(response2));
    }

    @Test
    public void allTypeOperationRetryMoreThanMaxAttempts_withSyncWaiter_shouldThrowException() {
        AllTypesResponse response1 = (AllTypesResponse) AllTypesResponse.builder()
                                                                        .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                        .statusCode(404)
                                                                                                        .build())
                                                                        .build();
        AllTypesResponse response2 = (AllTypesResponse) AllTypesResponse.builder()
                                                                        .sdkHttpResponse(SdkHttpResponse.builder()
                                                                                                        .statusCode(200)
                                                                                                        .build())
                                                                        .build();
        when(client.allTypes(any(AllTypesRequest.class))).thenReturn(response1, response1, response1, response2);
        assertThatThrownBy(() -> waiter.waitUntilAllTypesSuccess(AllTypesRequest.builder().build())).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void closeWaiterCreatedWithClient_clientDoesNotClose() {
        waiter.close();
        verify(client, never()).close();
    }

}
