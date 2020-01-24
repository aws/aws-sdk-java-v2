/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.core;

import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.AsyncMappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DatabaseOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@SdkPublicApi
@ThreadSafe
public final class DynamoDbAsyncMappedDatabase implements AsyncMappedDatabase {
    private final DynamoDbAsyncClient dynamoDbClient;
    private final MapperExtension mapperExtension;

    private DynamoDbAsyncMappedDatabase(DynamoDbAsyncClient dynamoDbClient, MapperExtension mapperExtension) {
        this.dynamoDbClient = dynamoDbClient;
        this.mapperExtension = mapperExtension;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> CompletableFuture<T> execute(DatabaseOperation<?, ?, T> operation) {
        return operation.executeAsync(dynamoDbClient, mapperExtension);
    }

    @Override
    public <T> DynamoDbAsyncMappedTable<T> table(String tableName, TableSchema<T> tableSchema) {
        return new DynamoDbAsyncMappedTable<>(dynamoDbClient, mapperExtension, tableSchema, tableName);
    }

    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return dynamoDbClient;
    }

    public MapperExtension mapperExtension() {
        return mapperExtension;
    }

    public Builder toBuilder() {
        return builder().dynamoDbClient(this.dynamoDbClient).extendWith(this.mapperExtension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DynamoDbAsyncMappedDatabase that = (DynamoDbAsyncMappedDatabase) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient)
            : that.dynamoDbClient != null) {

            return false;
        }
        return mapperExtension != null ? mapperExtension.equals(that.mapperExtension) : that.mapperExtension == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (mapperExtension != null ? mapperExtension.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private DynamoDbAsyncClient dynamoDbClient;
        private MapperExtension mapperExtension;

        private Builder() {
        }

        public DynamoDbAsyncMappedDatabase build() {
            if (dynamoDbClient == null) {
                throw new IllegalArgumentException("You must provide a DynamoDbClient to build a "
                                                   + "DynamoDbMappedDatabase.");
            }

            return new DynamoDbAsyncMappedDatabase(dynamoDbClient, mapperExtension);
        }

        public Builder dynamoDbClient(DynamoDbAsyncClient dynamoDbAsyncClient) {
            this.dynamoDbClient = dynamoDbAsyncClient;
            return this;
        }

        public Builder extendWith(MapperExtension mapperExtension) {
            if (mapperExtension != null && this.mapperExtension != null) {
                throw new IllegalArgumentException("You may only extend a DynamoDbMappedDatabase with a single "
                                                   + "extension. To combine multiple extensions, use the "
                                                   + "ChainMapperExtension.");
            }

            this.mapperExtension = mapperExtension;
            return this;
        }
    }
}
