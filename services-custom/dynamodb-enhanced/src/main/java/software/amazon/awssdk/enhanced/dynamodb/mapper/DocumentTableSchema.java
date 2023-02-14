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

import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterProviderResolver;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


/**
 * Implementation of {@link TableSchema} that builds a table schema based on DynamoDB Items. This class always maps the DynamoDB
 * items as {@link EnhancedDocument}
 */
@SdkPublicApi
public class DocumentTableSchema implements TableSchema<EnhancedDocument> {

    private final TableMetadata tableMetadata;
    private final List<AttributeConverterProvider> attributeConverterProviders;

    public DocumentTableSchema(Builder builder) {
        this.attributeConverterProviders = builder.attributeConverterProviders;
        this.tableMetadata = builder.staticTableMetaDataBuilder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EnhancedDocument mapToItem(Map<String, AttributeValue> attributeMap) {
        if (attributeMap == null) {
            return null;
        }
        DefaultEnhancedDocument.DefaultBuilder builder = DefaultEnhancedDocument.builder();
        return builder.attributeValueMap(attributeMap)
                      .attributeConverterProviders(attributeConverterProviders)
                      .build();
    }

    @Override
    public Map<String, AttributeValue> itemToMap(EnhancedDocument item, boolean ignoreNulls) {
        if (item instanceof DefaultEnhancedDocument) {
            Map<String, AttributeValue> attributeValueMap = ((DefaultEnhancedDocument) item).getAttributeValueMap();
            return attributeValueMap;
        }
        throw new IllegalArgumentException("EnhancedDocument item is not instance of DefaultEnhancedDocument");
    }

    @Override
    public Map<String, AttributeValue> itemToMap(EnhancedDocument item, Collection<String> attributes) {
        if (item instanceof DefaultEnhancedDocument) {
            Map<String, AttributeValue> result = new HashMap<>();
            attributes.forEach(attribute ->
                                   result.put(attribute, ((DefaultEnhancedDocument) item).getAttributeValueMap().get(attribute)));
            return result;
        }
        throw new IllegalArgumentException("EnhancedDocument item is not instance of DefaultEnhancedDocument");
    }

    @Override
    public AttributeValue attributeValue(EnhancedDocument item, String attributeName) {
        if (item instanceof DefaultEnhancedDocument) {
            return ((DefaultEnhancedDocument) item).getAttributeValueMap().get(attributeName);
        }
        throw new IllegalArgumentException("EnhancedDocument item is not instance of DefaultEnhancedDocument");
    }

    @Override
    public TableMetadata tableMetadata() {
        return tableMetadata;
    }

    @Override
    public EnhancedType<EnhancedDocument> itemType() {
        return EnhancedType.of(EnhancedDocument.class);
    }

    @Override
    public List<String> attributeNames() {
        return tableMetadata.primaryKeys().stream().collect(Collectors.toList());
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @NotThreadSafe
    public static final class Builder {

        private final StaticTableMetadata.Builder staticTableMetaDataBuilder = StaticTableMetadata.builder();

        /**
         * By Default the defaultConverterProvider is used for converting AttributeValue to primitive types.
         */
        private List<AttributeConverterProvider> attributeConverterProviders =
            Collections.singletonList(ConverterProviderResolver.defaultConverterProvider());

        /**
         * Adds information about a partition key associated with a specific index.
         *
         * @param attributeName      the name of the attribute that represents the partition key
         * @param attributeValueType the {@link AttributeValueType} of the partition key
         */
        public Builder primaryKey(String attributeName, AttributeValueType attributeValueType) {
            staticTableMetaDataBuilder.addIndexPartitionKey(primaryIndexName(), attributeName, attributeValueType);
            return this;
        }

        /**
         * Adds information about a sort key associated with a specific index.
         *
         * @param attributeName      the name of the attribute that represents the sort key
         * @param attributeValueType the {@link AttributeValueType} of the sort key
         */
        public Builder sortKey(String attributeName, AttributeValueType attributeValueType) {
            staticTableMetaDataBuilder.addIndexSortKey(primaryIndexName(), attributeName, attributeValueType);
            return this;
        }

        /**
         * Specifies the {@link AttributeConverterProvider}s to use with the table schema. The list of attribute converter
         * providers must provide {@link AttributeConverter}s for Custom types. The attribute converter providers will be loaded
         * in the strict order they are supplied here.
         * <p>
         * If no AttributeConverterProvider are provided then  {@link DefaultAttributeConverterProvider} is used, which provides
         * standard converters for most primitive and common Java types, so that provider must be included in the supplied list if
         * it is to be used. Providing an empty list here will cause no providers to get loaded.
         * <p>
         * Adding one custom attribute converter provider and using the default as fallback:
         * {@code builder.attributeConverterProviders(customAttributeConverter, AttributeConverterProvider.defaultProvider()) }
         *
         * @param attributeConverterProviders a list of attribute converter providers to use with the table schema
         */
        public Builder attributeConverterProviders(AttributeConverterProvider... attributeConverterProviders) {
            this.attributeConverterProviders = Arrays.asList(attributeConverterProviders);
            return this;
        }

        /**
         * Specifies the {@link AttributeConverterProvider}s to use with the table schema. The list of attribute converter
         * providers must provide {@link AttributeConverter}s for all types used in the schema. The attribute converter providers
         * will be loaded in the strict order they are supplied here.
         * <p>
         * If no AttributeConverterProvider are provided then {@link DefaultAttributeConverterProvider} will be used, which
         * provides standard converters for most primitive and common Java types, so that provider must be included in the
         * supplied list if it is to be used. Providing an empty list here will cause no providers to get loaded.
         * <p>
         * Adding one custom attribute converter provider and using the default as fallback:
         * {@code List<AttributeConverterProvider> providers = new ArrayList<>( customAttributeConverter,
         * AttributeConverterProvider.defaultProvider()); builder.attributeConverterProviders(providers); }
         *
         * @param attributeConverterProviders a list of attribute converter providers to use with the table schema
         */
        public Builder attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders) {
            this.attributeConverterProviders = new ArrayList<>(attributeConverterProviders);
            return this;
        }

        /**
         * Builds a {@link StaticImmutableTableSchema} based on the values this builder has been configured with
         */
        public DocumentTableSchema build() {
            return new DocumentTableSchema(this);
        }
    }
}
