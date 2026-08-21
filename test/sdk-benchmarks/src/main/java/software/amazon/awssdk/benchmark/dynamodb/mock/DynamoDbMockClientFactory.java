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

package software.amazon.awssdk.benchmark.dynamodb.mock;

import java.util.Map;
import software.amazon.awssdk.benchmark.dynamodb.DynamoDbBenchmarkConstant;
import software.amazon.awssdk.benchmark.utils.MockHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Factory for sync/async DynamoDB clients backed by deterministic mock HTTP responses.
 */
public final class DynamoDbMockClientFactory {

    private DynamoDbMockClientFactory() {
    }

    public static DynamoDbClient syncClient(String successResponseBody) {
        MockHttpClient httpClient = new MockHttpClient(
            successResponseBody,
            DynamoDbBenchmarkConstant.MOCK_ERROR_RESPONSE_BODY);
        return DynamoDbClient.builder()
                             .credentialsProvider(DynamoDbBenchmarkConstant.MOCK_CREDENTIALS_PROVIDER)
                             .region(DynamoDbBenchmarkConstant.REGION)
                             .httpClient(httpClient)
                             .build();
    }

    public static DynamoDbClient syncGetItemClient(Map<String, AttributeValue> item) {
        return syncClient(DynamoDbMockResponseFactory.getItemResponseBody(item));
    }

    public static DynamoDbClient syncPutItemClient() {
        return syncClient(DynamoDbMockResponseFactory.putItemResponseBody());
    }

    public static DynamoDbClient syncQueryClient(Map<String, AttributeValue> item) {
        return syncClient(DynamoDbMockResponseFactory.queryResponseBody(item));
    }

    /**
     * Async GetItem client using the same GetItem response bytes as the sync Get benchmarks.
     * Caller must {@link AsyncMockResources#close()} to shut down the mock executor.
     */
    public static AsyncMockResources asyncGetItemClient(Map<String, AttributeValue> item) {
        byte[] body = DynamoDbMockResponseFactory.getItemResponseBytes(item);
        SdkAsyncHttpClient httpClient = new ReplayingMockAsyncHttpClient(body);
        DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                                                        .credentialsProvider(
                                                            DynamoDbBenchmarkConstant.MOCK_CREDENTIALS_PROVIDER)
                                                        .region(DynamoDbBenchmarkConstant.REGION)
                                                        .httpClient(httpClient)
                                                        .build();
        return new AsyncMockResources(client, httpClient);
    }

    /**
     * Holds an async DynamoDB client and its mock HTTP client so both can be closed.
     * User-supplied {@code httpClient()} instances are not closed by the SDK client.
     */
    public static final class AsyncMockResources implements SdkAutoCloseable {
        private final DynamoDbAsyncClient client;
        private final SdkAsyncHttpClient httpClient;

        AsyncMockResources(DynamoDbAsyncClient client, SdkAsyncHttpClient httpClient) {
            this.client = client;
            this.httpClient = httpClient;
        }

        public DynamoDbAsyncClient client() {
            return client;
        }

        @Override
        public void close() {
            if (client != null) {
                client.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }
}
