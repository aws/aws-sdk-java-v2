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

package software.amazon.awssdk.services.delegatingclients;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.protocolrestjson.DelegatingProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonRequest;
import software.amazon.awssdk.services.protocolrestjson.paginators.PaginatedOperationWithResultKeyPublisher;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

public class DelegatingAsyncClientTest {

    private static final String INTERCEPTED_HEADER = "intercepted-header";
    private static final String INTERCEPTED_HEADER_VALUE = "intercepted-value";
    private static final String RESPONSE = "response";

    MockAsyncHttpClient mockAsyncHttpClient = new MockAsyncHttpClient();
    ProtocolRestJsonAsyncClient defaultClient = ProtocolRestJsonAsyncClient.builder()
                                                                           .httpClient(mockAsyncHttpClient)
                                                                           .endpointOverride(URI.create("http://localhost"))
                                                                           .build();
    ProtocolRestJsonAsyncClient decoratingClient = new DecoratingClient(defaultClient);

    @BeforeEach
    public void before() {
        mockAsyncHttpClient.stubNextResponse(
            HttpExecuteResponse.builder()
                               .response(SdkHttpResponse.builder().statusCode(200).build())
                               .responseBody(AbortableInputStream.create(new StringInputStream(RESPONSE)))
                               .build());
    }

    @Test
    public void standardOp_Request_standardFutureResponse_delegatingClient_SuccessfullyIntercepts() {
        decoratingClient.allTypes(AllTypesRequest.builder().stringMember("test").build()).join();
        validateIsDecorated();
    }

    @Test
    public void standardOp_ConsumerRequest_standardFutureResponse_delegatingClient_SuccessfullyIntercepts() {
        decoratingClient.allTypes(r -> r.stringMember("test")).join();
        validateIsDecorated();
    }

    @Test
    public void paginatedOp_Request_standardFutureResponse_delegatingClient_SuccessfullyIntercepts() {
        decoratingClient.paginatedOperationWithResultKey(
            PaginatedOperationWithResultKeyRequest.builder().nextToken("token").build())
                        .join();
        validateIsDecorated();
    }

    @Test
    public void paginatedOp_ConsumerRequest_standardFutureResponse_delegatingClient_SuccessfullyIntercepts() {
        decoratingClient.paginatedOperationWithResultKey(r -> r.nextToken("token")).join();
        validateIsDecorated();
    }

    @Test
    public void paginatedOp_Request_publisherResponse_delegatingClient_DoesNotIntercept() throws Exception {
        PaginatedOperationWithResultKeyPublisher publisher =
            decoratingClient.paginatedOperationWithResultKeyPaginator(PaginatedOperationWithResultKeyRequest.builder()
                                                                                                            .nextToken("token")
                                                                                                            .build());
        CompletableFuture<Void> future = publisher.subscribe(PaginatedOperationWithResultKeyResponse::items);
        future.get();
        validateIsDecorated();
    }

    @Test
    public void paginatedOp_ConsumerRequest_publisherResponse_delegatingClient_DoesNotIntercept() throws Exception {
        PaginatedOperationWithResultKeyPublisher publisher =
            decoratingClient.paginatedOperationWithResultKeyPaginator(r -> r.nextToken("token").build());
        CompletableFuture<Void> future = publisher.subscribe(PaginatedOperationWithResultKeyResponse::items);
        future.get();
        validateIsDecorated();
    }

    private void validateIsDecorated() {
        SdkHttpRequest lastRequest = mockAsyncHttpClient.getLastRequest();
        assertThat(lastRequest.headers().get(INTERCEPTED_HEADER)).isNotNull();
        assertThat(lastRequest.headers().get(INTERCEPTED_HEADER).get(0)).isEqualTo(INTERCEPTED_HEADER_VALUE);
    }

    private static final class DecoratingClient extends DelegatingProtocolRestJsonAsyncClient {

        DecoratingClient(ProtocolRestJsonAsyncClient client) {
            super(client);
        }

        @Override
        protected <T extends ProtocolRestJsonRequest, ReturnT> CompletableFuture<ReturnT>
        invokeOperation(T request, Function<T, CompletableFuture<ReturnT>> operation) {
            return operation.apply(decorateRequest(request));
        }

        @SuppressWarnings("unchecked")
        private <T extends ProtocolRestJsonRequest> T decorateRequest(T request) {
            AwsRequestOverrideConfiguration alin = AwsRequestOverrideConfiguration.builder()
                                                                                  .putHeader(INTERCEPTED_HEADER, INTERCEPTED_HEADER_VALUE)
                                                                                  .build();
            return (T) request.toBuilder()
                              .overrideConfiguration(alin)
                              .build();
        }
    }
}
