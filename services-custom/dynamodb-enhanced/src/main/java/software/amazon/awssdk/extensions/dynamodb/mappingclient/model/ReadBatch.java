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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

@SdkPublicApi
public final class ReadBatch {
    private final String tableName;
    private final KeysAndAttributes keysAndAttributes;

    private ReadBatch(BuilderImpl<?> builder) {
        this.tableName = builder.mappedTableResource.tableName();
        this.keysAndAttributes = generateKeysAndAttributes(builder.requests, builder.mappedTableResource.tableSchema());
    }

    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new BuilderImpl<>();
    }

    public String tableName() {
        return tableName;
    }

    public KeysAndAttributes keysAndAttributes() {
        return keysAndAttributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadBatch readBatch = (ReadBatch) o;

        if (tableName != null ? !tableName.equals(readBatch.tableName) :
            readBatch.tableName != null) {

            return false;
        }
        return keysAndAttributes != null ?
               keysAndAttributes.equals(readBatch.keysAndAttributes) :
               readBatch.keysAndAttributes == null;
    }

    @Override
    public int hashCode() {
        int result = tableName != null ? tableName.hashCode() : 0;
        result = 31 * result + (keysAndAttributes != null ? keysAndAttributes.hashCode() : 0);
        return result;
    }

    public interface Builder<T> {
        Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource);

        Builder<T> addGetItem(GetItemEnhancedRequest request);

        ReadBatch build();
    }

    private static <T> KeysAndAttributes generateKeysAndAttributes(List<GetItemEnhancedRequest> readRequests,
                                                                   TableSchema<T> tableSchema) {


        Boolean firstRecordConsistentRead = validateAndGetConsistentRead(readRequests);

        List<Map<String, AttributeValue>> keys =
            readRequests.stream()
                        .map(GetItemEnhancedRequest::key)
                        .map(key -> key.keyMap(tableSchema, TableMetadata.primaryIndexName()))
                        .collect(Collectors.toList());

        return KeysAndAttributes.builder()
                                .keys(keys)
                                .consistentRead(firstRecordConsistentRead)
                                .build();

    }

    private static Boolean validateAndGetConsistentRead(List<GetItemEnhancedRequest> readRequests) {
        Boolean firstRecordConsistentRead = null;
        boolean isFirstRecord = true;

        for (GetItemEnhancedRequest request : readRequests) {
            if (isFirstRecord) {
                isFirstRecord = false;
                firstRecordConsistentRead = request.consistentRead();
            } else {
                if (!compareNullableBooleans(firstRecordConsistentRead, request.consistentRead())) {
                    throw new IllegalArgumentException("All batchable read requests for the same "
                                                       + "table must have the same 'consistentRead' "
                                                       + "setting.");
                }
            }
        }
        return firstRecordConsistentRead;
    }

    private static boolean compareNullableBooleans(Boolean one, Boolean two) {
        if (one == null && two == null) {
            return true;
        }

        if (one != null) {
            return one.equals(two);
        } else {
            return false;
        }
    }

    private static final class BuilderImpl<T> implements Builder<T> {
        private MappedTableResource<T> mappedTableResource;
        private List<GetItemEnhancedRequest> requests = new ArrayList<>();

        private BuilderImpl() {
        }

        public Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource) {
            this.mappedTableResource = mappedTableResource;
            return this;
        }

        public Builder<T> addGetItem(GetItemEnhancedRequest request) {
            requests.add(request);
            return this;
        }

        public ReadBatch build() {
            return new ReadBatch(this);
        }

    }
}
