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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import static java.util.Collections.emptyList;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.readAndTransformSingleItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;

@SdkPublicApi
public final class BatchGetResultPage {
    private final BatchGetItemResponse batchGetItemResponse;
    private final MapperExtension mapperExtension;

    private BatchGetResultPage(Builder builder) {
        this.batchGetItemResponse = builder.batchGetItemResponse;
        this.mapperExtension = builder.mapperExtension;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> List<T> getResultsForTable(MappedTableResource<T> mappedTable) {
        List<Map<String, AttributeValue>> results =
            batchGetItemResponse.responses()
                                .getOrDefault(mappedTable.tableName(), emptyList());

        return results.stream()
                      .map(itemMap -> readAndTransformSingleItem(itemMap,
                                                                 mappedTable.tableSchema(),
                                                                 OperationContext.create(mappedTable.tableName()),
                                                                 mapperExtension))
                      .collect(Collectors.toList());
    }

    public static final class Builder {

        private BatchGetItemResponse batchGetItemResponse;
        private MapperExtension mapperExtension;

        private Builder() {
        }

        public Builder batchGetItemResponse(BatchGetItemResponse batchGetItemResponse) {
            this.batchGetItemResponse = batchGetItemResponse;
            return this;
        }

        public Builder mapperExtension(MapperExtension mapperExtension) {
            this.mapperExtension = mapperExtension;
            return this;
        }

        public BatchGetResultPage build() {
            return new BatchGetResultPage(this);
        }
    }
}
