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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static software.amazon.awssdk.enhanced.dynamodb.extensions.TimeToLiveExtension.CUSTOM_METADATA_KEY;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateTimeToLiveEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveResponse;

@SdkInternalApi
public class UpdateTimeToLiveOperation<T> implements TableOperation<T, UpdateTimeToLiveRequest, UpdateTimeToLiveResponse,
    UpdateTimeToLiveEnhancedResponse> {

    private final boolean enabled;

    public UpdateTimeToLiveOperation(boolean enabled) {
        this.enabled = enabled;
    }

    public static <T> UpdateTimeToLiveOperation<T> create(boolean enabled) {
        return new UpdateTimeToLiveOperation<>(enabled);
    }

    @Override
    public OperationName operationName() {
        return OperationName.UPDATE_TIME_TO_LIVE;
    }

    @Override
    public UpdateTimeToLiveRequest generateRequest(TableSchema<T> tableSchema,
                                                   OperationContext operationContext,
                                                   DynamoDbEnhancedClientExtension extension) {
        Map<String, ?> customTTLMetadata = tableSchema.tableMetadata()
                                                      .customMetadataObject(CUSTOM_METADATA_KEY, Map.class).orElse(null);
        if (customTTLMetadata == null) {
            throw new IllegalArgumentException("Custom TTL metadata object is null");
        }
        String ttlAttributeName = (String) customTTLMetadata.get("attributeName");

        return UpdateTimeToLiveRequest.builder()
                                      .tableName(operationContext.tableName())
                                      .timeToLiveSpecification(TimeToLiveSpecification.builder()
                                                                                      .attributeName(ttlAttributeName)
                                                                                      .enabled(enabled).build())
                                      .build();
    }

    @Override
    public Function<UpdateTimeToLiveRequest, UpdateTimeToLiveResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::updateTimeToLive;
    }

    @Override
    public Function<UpdateTimeToLiveRequest, CompletableFuture<UpdateTimeToLiveResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {
        return dynamoDbAsyncClient::updateTimeToLive;
    }

    @Override
    public UpdateTimeToLiveEnhancedResponse transformResponse(UpdateTimeToLiveResponse response,
                                                              TableSchema<T> tableSchema,
                                                              OperationContext operationContext,
                                                              DynamoDbEnhancedClientExtension extension) {
        return UpdateTimeToLiveEnhancedResponse.builder()
                                               .response(response)
                                               .build();
    }
}
