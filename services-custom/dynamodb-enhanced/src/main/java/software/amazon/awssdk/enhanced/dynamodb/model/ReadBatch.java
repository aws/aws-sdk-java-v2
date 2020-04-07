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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/**
 * Defines a collection of primary keys for items in a table, stored as {@link KeysAndAttributes}, and
 * used for the batchGetItem() operation (such as
 * {@link DynamoDbEnhancedClient#batchGetItem(BatchGetItemEnhancedRequest)}) as part of a
 * {@link BatchGetItemEnhancedRequest}.
 * <p>
 * A valid request object should contain one or more primary keys.
 */
@SdkPublicApi
public final class ReadBatch {
    private final String tableName;
    private final KeysAndAttributes keysAndAttributes;

    private ReadBatch(BuilderImpl<?> builder) {
        this.tableName = builder.mappedTableResource != null ? builder.mappedTableResource.tableName() : null;
        this.keysAndAttributes = generateKeysAndAttributes(builder.requests, builder.mappedTableResource);
    }

    /**
     * Creates a newly initialized builder for a read batch.
     *
     * @param itemClass the class that items in this table map to
     * @param <T> The type of the modelled object, corresponding to itemClass
     * @return a ReadBatch builder
     */
    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new BuilderImpl<>();
    }

    /**
     * Returns the table name associated with this batch.
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the collection of keys and attributes, see {@link KeysAndAttributes}, in this read batch.
     */
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

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * A valid builder must define a {@link MappedTableResource} and add at least one
     * {@link GetItemEnhancedRequest}.
     *
     * @param <T> the type that items in this table map to
     */
    public interface Builder<T> {

        /**
         * Sets the mapped table resource (table) that the items in this read batch should come from.
         *
         * @param mappedTableResource the table reference
         * @return a builder of this type
         */
        Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource);

        /**
         * Adds a {@link GetItemEnhancedRequest} with a primary {@link Key} to the builder.
         *
         * @param request A {@link GetItemEnhancedRequest}
         * @return a builder of this type
         */
        Builder<T> addGetItem(GetItemEnhancedRequest request);

        /**
         * Adds a {@link GetItemEnhancedRequest} with a primary {@link Key} to the builder by accepting a consumer of
         * {@link GetItemEnhancedRequest.Builder}.
         *
         * @param requestConsumer a {@link Consumer} of {@link GetItemEnhancedRequest}
         * @return a builder of this type
         */
        Builder<T> addGetItem(Consumer<GetItemEnhancedRequest.Builder> requestConsumer);

        /**
         * Adds a GetItem request with a primary {@link Key} to the builder.
         *
         * @param key A {@link Key} to match the record retrieved from the database.
         * @return a builder of this type
         */
        Builder<T> addGetItem(Key key);

        /**
         * Adds a GetItem request to the builder.
         *
         * @param keyItem an item that will have its key fields used to match a record to retrieve from the database.
         * @return a builder of this type
         */
        Builder<T> addGetItem(T keyItem);

        ReadBatch build();
    }

    private static <T> KeysAndAttributes generateKeysAndAttributes(List<GetItemEnhancedRequest> readRequests,
                                                                   MappedTableResource<T> mappedTableResource) {
        if (readRequests == null || readRequests.isEmpty()) {
            return null;
        }

        Boolean firstRecordConsistentRead = validateAndGetConsistentRead(readRequests);

        List<Map<String, AttributeValue>> keys =
            readRequests.stream()
                        .map(GetItemEnhancedRequest::key)
                        .map(key -> key.keyMap(mappedTableResource.tableSchema(), TableMetadata.primaryIndexName()))
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

        @Override
        public Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource) {
            this.mappedTableResource = mappedTableResource;
            return this;
        }

        @Override
        public Builder<T> addGetItem(GetItemEnhancedRequest request) {
            requests.add(request);
            return this;
        }

        @Override
        public Builder<T> addGetItem(Consumer<GetItemEnhancedRequest.Builder> requestConsumer) {
            GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
            requestConsumer.accept(builder);
            return addGetItem(builder.build());
        }

        @Override
        public Builder<T> addGetItem(Key key) {
            return addGetItem(r -> r.key(key));
        }

        @Override
        public Builder<T> addGetItem(T keyItem) {
            return addGetItem(this.mappedTableResource.keyFrom(keyItem));
        }

        @Override
        public ReadBatch build() {
            return new ReadBatch(this);
        }

    }
}
