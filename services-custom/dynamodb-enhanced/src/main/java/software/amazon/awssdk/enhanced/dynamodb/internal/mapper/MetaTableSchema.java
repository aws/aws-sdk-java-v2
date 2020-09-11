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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An implementation of {@link TableSchema} that can be instantiated as an uninitialized reference and then lazily
 * initialized later with a concrete {@link TableSchema} at which point it will behave as the real object.
 * <p>
 * This allows an immutable {@link TableSchema} to be declared and used in a self-referential recursive way within its
 * builder/definition path. Any attempt to use the {@link MetaTableSchema} as a concrete {@link TableSchema} before
 * calling {@link #initialize(TableSchema)} will cause an exception to be thrown.
 */
@SdkInternalApi
public class MetaTableSchema<T> implements TableSchema<T> {
    private TableSchema<T> concreteTableSchema;

    private MetaTableSchema() {
    }

    public static <T> MetaTableSchema<T> create(Class<T> itemClass) {
        return new MetaTableSchema<>();
    }

    @Override
    public T mapToItem(Map<String, AttributeValue> attributeMap) {
        return concreteTableSchema().mapToItem(attributeMap);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls) {
        return concreteTableSchema().itemToMap(item, ignoreNulls);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes) {
        return concreteTableSchema().itemToMap(item, attributes);
    }

    @Override
    public AttributeValue attributeValue(T item, String attributeName) {
        return concreteTableSchema().attributeValue(item, attributeName);
    }

    @Override
    public TableMetadata tableMetadata() {
        return concreteTableSchema().tableMetadata();
    }

    @Override
    public EnhancedType<T> itemType() {
        return concreteTableSchema().itemType();
    }

    @Override
    public List<String> attributeNames() {
        return concreteTableSchema().attributeNames();
    }

    @Override
    public boolean isAbstract() {
        return concreteTableSchema().isAbstract();
    }

    public void initialize(TableSchema<T> realTableSchema) {
        if (this.concreteTableSchema != null) {
            throw new IllegalStateException("A MetaTableSchema can only be initialized with a concrete TableSchema " +
                                                "instance once.");
        }

        this.concreteTableSchema = realTableSchema;
    }

    public TableSchema<T> concreteTableSchema() {
        if (this.concreteTableSchema == null) {
            throw new IllegalStateException("A MetaTableSchema must be initialized with a concrete TableSchema " +
                                                "instance by calling 'initialize' before it can be used as a " +
                                                "TableSchema itself");
        }

        return this.concreteTableSchema;
    }

    public boolean isInitialized() {
        return this.concreteTableSchema != null;
    }
}
