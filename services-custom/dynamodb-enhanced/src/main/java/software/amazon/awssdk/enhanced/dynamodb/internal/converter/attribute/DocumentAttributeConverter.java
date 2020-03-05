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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeValueType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * {@link AttributeConverter} for converting nested table schemas
 */
@SdkInternalApi
public class DocumentAttributeConverter<T> implements AttributeConverter<T> {

    private final TableSchema<T> tableSchema;
    private final TypeToken<T> typeToken;

    private DocumentAttributeConverter(TableSchema<T> tableSchema,
                                       TypeToken<T> typeToken) {
        this.tableSchema = tableSchema;
        this.typeToken = typeToken;
    }

    public static <T> DocumentAttributeConverter create(TableSchema<T> tableSchema,
                                                        TypeToken<T> typeToken) {
        return new DocumentAttributeConverter(tableSchema, typeToken);
    }

    @Override
    public AttributeValue transformFrom(T input) {
        return AttributeValue.builder().m(tableSchema.itemToMap(input, false)).build();
    }

    @Override
    public T transformTo(AttributeValue input) {
        return tableSchema.mapToItem(input.m());
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }

    @Override
    public TypeToken<T> type() {
        return typeToken;
    }
}
