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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Base class for any {@link TableSchema} implementation that wraps and acts as a different {@link TableSchema}
 * implementation.
 * @param <T> The parameterized type of the {@link TableSchema} being proxied.
 * @param <R> The actual type of the {@link TableSchema} being proxied.
 */
@SdkPublicApi
public abstract class WrappedTableSchema<T, R extends TableSchema<T>> implements TableSchema<T> {
    private final R delegateTableSchema;

    /**
     * Standard constructor.
     * @param delegateTableSchema An instance of {@link TableSchema} to be wrapped and proxied by this class.
     */
    protected WrappedTableSchema(R delegateTableSchema) {
        this.delegateTableSchema = delegateTableSchema;
    }

    /**
     * The delegate table schema that is wrapped and proxied by this class.
     */
    protected R delegateTableSchema() {
        return this.delegateTableSchema;
    }

    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        return this.delegateTableSchema.mapToItem(attributeMap);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
        return this.delegateTableSchema.itemToMap(item, ignoreNulls);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes) {
        return this.delegateTableSchema.itemToMap(item, attributes);
    }

    @Override
    public AttributeValue attributeValue(T item, String attributeName) {
        return this.delegateTableSchema.attributeValue(item, attributeName);
    }

    @Override
    public TableMetadata tableMetadata() {
        return this.delegateTableSchema.tableMetadata();
    }

    @Override
    public EnhancedType<T> itemType() {
        return this.delegateTableSchema.itemType();
    }

    @Override
    public List<String> attributeNames() {
        return this.delegateTableSchema.attributeNames();
    }

    @Override
    public boolean isAbstract() {
        return this.delegateTableSchema.isAbstract();
    }
}
