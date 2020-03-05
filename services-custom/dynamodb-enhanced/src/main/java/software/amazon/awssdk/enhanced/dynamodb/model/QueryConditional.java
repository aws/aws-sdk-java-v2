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
 * {@code
 * QueryConditional partitionValueGreaterThanTen = QueryConditional.greaterThan(k -> k.partitionValue(10));
 * }
 */
@SdkPublicApi
public interface QueryConditional {
    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is equal to a specific value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional equalTo(Key key) {
        return new EqualToConditional(key);
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is equal to a specific value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional equalTo(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return equalTo(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than a specific value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional greaterThan(Key key) {
        return new SingleKeyItemConditional(key, ">");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than a specific value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional greaterThan(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return greaterThan(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than or equal to a specific
     * value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional greaterThanOrEqualTo(Key key) {
        return new SingleKeyItemConditional(key, ">=");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is greater than or equal to a specific
     * value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional greaterThanOrEqualTo(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return greaterThanOrEqualTo(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than a specific value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional lessThan(Key key) {
        return new SingleKeyItemConditional(key, "<");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than a specific value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional lessThan(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return lessThan(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than or equal to a specific
     * value.
     * @param key the literal key used to compare the value of the index against
     */
    static QueryConditional lessThanOrEqualTo(Key key) {
        return new SingleKeyItemConditional(key, "<=");
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is less than or equal to a specific
     * value.
     * @param keyConsumer 'builder consumer' for the literal key used to compare the value of the index against
     */
    static QueryConditional lessThanOrEqualTo(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return lessThanOrEqualTo(builder.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is between two specific values.
     * @param keyFrom the literal key used to compare the start of the range to compare the value of the index against
     * @param keyTo the literal key used to compare the end of the range to compare the value of the index against
     */
    static QueryConditional between(Key keyFrom, Key keyTo) {
        return new BetweenConditional(keyFrom, keyTo);
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index is between two specific values.
     * @param keyFromConsumer 'builder consumer' for the literal key used to compare the start of the range to compare
     *                        the value of the index against
     * @param keyToConsumer 'builder consumer' for the literal key used to compare the end of the range to compare the
     *                      value of the index against
     */
    static QueryConditional between(Consumer<Key.Builder> keyFromConsumer, Consumer<Key.Builder> keyToConsumer) {
        Key.Builder builderFrom = Key.builder();
        Key.Builder builderTo = Key.builder();
        keyFromConsumer.accept(builderFrom);
        keyToConsumer.accept(builderTo);
        return between(builderFrom.build(), builderTo.build());
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index begins with a specific value.
     * @param key the literal key used to compare the start of the value of the index against
     */
    static QueryConditional beginsWith(Key key) {
        return new BeginsWithConditional(key);
    }

    /**
     * Creates a {@link QueryConditional} that matches when the key of an index begins with a specific value.
     * @param keyConsumer 'builder consumer'  the literal key used to compare the start of the value of the index
     *                    against
     */
    static QueryConditional beginsWith(Consumer<Key.Builder> keyConsumer) {
        Key.Builder builder = Key.builder();
        keyConsumer.accept(builder);
        return beginsWith(builder.build());
    }

    /**
     * Generates a conditional {@link Expression} based on specific context that is supplied as arguments.
     * @param tableSchema A {@link TableSchema} that this expression will be used with
     * @param indexName The specific index name of the index this expression will be used with
     * @return A specific {@link Expression} that can be used as part of a query request
     */
    Expression expression(TableSchema<?> tableSchema, String indexName);
}
