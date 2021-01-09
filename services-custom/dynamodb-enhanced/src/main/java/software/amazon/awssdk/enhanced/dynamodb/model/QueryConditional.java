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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.BeginsWithConditional;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.BetweenConditional;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.EqualToConditional;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.SingleKeyItemConditional;

/**
 * An interface for a literal conditional that can be used in an enhanced DynamoDB query. Contains convenient static
 * methods that can be used to construct the most common conditional statements. Query conditionals are not linked to
 * any specific table or schema and can be re-used in different contexts.
 * <p>
 * Example:
 * <pre>
 * {@code
 * QueryConditional sortValueGreaterThanFour = QueryConditional.sortGreaterThan(k -> k.partitionValue(10).sortValue(4));
 * }
 * </pre>
 */
@SdkPublicApi
public interface QueryConditional {
    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is equal to a specific value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional keyEqualTo(Key key) {
        return new EqualToConditional(key);
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is equal to a specific value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional keyEqualTo(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return keyEqualTo(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than a specific value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional sortGreaterThan(Key key) {
        return new SingleKeyItemConditional(key, ">");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than a specific value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional sortGreaterThan(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return sortGreaterThan(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than or equal to a specific
     * value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional sortGreaterThanOrEqualTo(Key key) {
        return new SingleKeyItemConditional(key, ">=");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than or equal to a specific
     * value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional sortGreaterThanOrEqualTo(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return sortGreaterThanOrEqualTo(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than a specific value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional sortLessThan(Key key) {
        return new SingleKeyItemConditional(key, "<");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than a specific value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional sortLessThan(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return sortLessThan(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than or equal to a specific
     * value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional sortLessThanOrEqualTo(Key key) {
        return new SingleKeyItemConditional(key, "<=");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than or equal to a specific
     * value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional sortLessThanOrEqualTo(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return sortLessThanOrEqualTo(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is between two specific values.
     * @param keyFrom the literal key used to compare the start of the range to compare the value of the index against
     * @param keyTo the literal key used to compare the end of the range to compare the value of the index against
     */
    static QueryConditional sortBetween(Key keyFrom, Key keyTo) {
        return new BetweenConditional(keyFrom, keyTo);
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is between two specific values.
     * @param keyFromConsumer 'builder consumer' for the literal key used to compare the start of the range to compare
     *                        the value of the index against
     * @param keyToConsumer 'builder consumer' for the literal key used to compare the end of the range to compare the
     *                      value of the index against
     */
    static QueryConditional sortBetween(Consumer<Key.Builder> keyFromConsumer, Consumer<Key.Builder> keyToConsumer) {
        Key.Builder builderFrom = Key.builder();
        Key.Builder builderTo = Key.builder();
        keyFromConsumer.accept(builderFrom);
        keyToConsumer.accept(builderTo);
        return sortBetween(builderFrom.build(), builderTo.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index begins with a specific value.
     * @param key the literal key used to compare the start of the value of the index against
     */
    static QueryConditional sortBeginsWith(Key key) {
        return new BeginsWithConditional(key);
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index begins with a specific value.
     * @param keyConsumer 'builder consumer'  the literal key used to compare the start of the value of the index
     *                    against
     */
    static QueryConditional sortBeginsWith(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return sortBeginsWith(builder.build());
    }

    /**
     * Generates a conditional {@link Expression} based on specific context that is supplied as arguments.
     * @param tableSchema A {@link TableSchema} that this expression will be used with
     * @param indexName The specific index name of the index this expression will be used with
     * @return A specific {@link Expression} that can be used as part of a query request
     */
    Expression expression(TableSchema<?> tableSchema, String indexName);
}
