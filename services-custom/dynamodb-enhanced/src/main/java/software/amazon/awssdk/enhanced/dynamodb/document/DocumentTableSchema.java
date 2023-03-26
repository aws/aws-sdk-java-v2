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

package software.amazon.awssdk.enhanced.dynamodb.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterProviderResolver;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


/**
 * Implementation of {@link TableSchema} that builds a table schema based on DynamoDB Items.
 * <p>
 * In Amazon DynamoDB, an item is a collection of attributes. Each attribute has a name and a value. An attribute value can be a
 * scalar, a set, or a document type
 * <p>
 * A DocumentTableSchema is used to create a {@link DynamoDbTable} which provides read and writes access to DynamoDB table as
 * {@link EnhancedDocument}.
 * <p> DocumentTableSchema specifying primaryKey, sortKey and a customAttributeConverter can be created as below
 * {@snippet :
 * DocumentTableSchema documentTableSchema = DocumentTableSchema.builder()
 * .primaryKey("sampleHashKey", AttributeValueType.S)
 * .sortKey("sampleSortKey", AttributeValueType.S)
 * .attributeConverterProviders(customAttributeConverter, AttributeConverterProvider.defaultProvider())
 * .build();
 *}
 * <p> DocumentTableSchema can also be created without specifying primaryKey and sortKey in which cases the
 * {@link TableMetadata} of DocumentTableSchema will error if we try to access attributes from metaData. Also if
 * attributeConverterProviders are not provided then {@link DefaultAttributeConverterProvider} will be used
 * {@snippet :
 * DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
 *}
 *
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithItems.html" target="_top">Working
 * with items and attributes</a>
 */
@SdkPublicApi
public final class DocumentTableSchema implements TableSchema<EnhancedDocument> {
    private final TableMetadata tableMetadata;
    private final List<AttributeConverterProvider> attributeConverterProviders;

    private DocumentTableSchema(Builder builder) {
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
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder();
        attributeMap.forEach(builder::putObject);
        return builder.attributeConverterProviders(attributeConverterProviders)
                      .build();
    }

    /**
     * {@inheritDoc}
     *
     * This flag does not have significance for the Document API, unlike Java objects where the default value of an undefined
     * Object is null.In contrast to mapped classes, where a schema is present, the DocumentSchema is unaware of the entire
     * schema.Therefore, if an attribute is not present, it signifies that it is null, and there is no need to handle it in a
     * separate way.However, if the user explicitly wants to nullify certain attributes, then the user needs to set those
     * attributes as null in the Document that needs to be updated.
     *
     */
    @Override
    public Map<String, AttributeValue> itemToMap(EnhancedDocument item, boolean ignoreNulls) {
        if (item == null) {
            return null;
        }
        List<AttributeConverterProvider> providers = mergeAttributeConverterProviders(item);
        return item.toBuilder().attributeConverterProviders(providers).build().toMap();
    }

    private List<AttributeConverterProvider> mergeAttributeConverterProviders(EnhancedDocument item) {
        if (item.attributeConverterProviders() != null && !item.attributeConverterProviders().isEmpty()) {
            Set<AttributeConverterProvider> providers = new LinkedHashSet<>();
            providers.addAll(item.attributeConverterProviders());
            providers.addAll(attributeConverterProviders);
            return providers.stream().collect(Collectors.toList());
        }
        return attributeConverterProviders;
    }

    @Override
    public Map<String, AttributeValue> itemToMap(EnhancedDocument item, Collection<String> attributes) {
        if (item.toMap() == null) {
            return null;
        }
        List<AttributeConverterProvider> providers = mergeAttributeConverterProviders(item);
        return item.toBuilder().attributeConverterProviders(providers).build().toMap().entrySet()
                   .stream()
                   .filter(entry -> attributes.contains(entry.getKey()))
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                             (left, right) -> left, LinkedHashMap::new));
    }

    @Override
    public AttributeValue attributeValue(EnhancedDocument item, String attributeName) {
        if (item == null) {
            return null;
        }
        List<AttributeConverterProvider> providers = mergeAttributeConverterProviders(item);
        return item.toBuilder()
                   .attributeConverterProviders(providers)
                   .build()
                   .toMap()
                   .get(attributeName);
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
        return tableMetadata.keyAttributes().stream().map(key -> key.name()).collect(Collectors.toList());
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
         * @param indexName          the name of the index to associate the partition key with
         * @param attributeName      the name of the attribute that represents the partition key
         * @param attributeValueType the {@link AttributeValueType} of the partition key
         * @throws IllegalArgumentException if a partition key has already been defined for this index
         */
        public Builder addIndexPartitionKey(String indexName, String attributeName, AttributeValueType attributeValueType) {
            staticTableMetaDataBuilder.addIndexPartitionKey(indexName, attributeName, attributeValueType);
            return this;
        }

        /**
         * Adds information about a sort key associated with a specific index.
         *
         * @param indexName          the name of the index to associate the sort key with
         * @param attributeName      the name of the attribute that represents the sort key
         * @param attributeValueType the {@link AttributeValueType} of the sort key
         * @throws IllegalArgumentException if a sort key has already been defined for this index
         */
        public Builder addIndexSortKey(String indexName, String attributeName, AttributeValueType attributeValueType) {
            staticTableMetaDataBuilder.addIndexSortKey(indexName, attributeName, attributeValueType);
            return this;
        }

        /**
         * Specifies the {@link AttributeConverterProvider}s to use with the table schema. The list of attribute converter
         * providers must provide {@link AttributeConverter}s for Custom types. The attribute converter providers will be loaded
         * in the strict order they are supplied here.
         * <p>
         * By default, {@link DefaultAttributeConverterProvider} will be used, and it will provide standard converters for most
         * primitive and common Java types. Configuring this will override the default behavior, so it is recommended to always
         * append `DefaultAttributeConverterProvider` when you configure the custom attribute converter providers.
         * <p>
         * {@snippet :
         *     builder.attributeConverterProviders(customAttributeConverter, AttributeConverterProvider.defaultProvider());
         *}
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
         * By default, {@link DefaultAttributeConverterProvider} will be used, and it will provide standard converters for most
         * primitive and common Java types. Configuring this will override the default behavior, so it is recommended to always
         * append `DefaultAttributeConverterProvider` when you configure the custom attribute converter providers.
         * <p>
         * {@snippet :
         *     List<AttributeConverterProvider> providers = new ArrayList<>( customAttributeConverter,
         *     AttributeConverterProvider.defaultProvider());
         *     builder.attributeConverterProviders(providers);
         *}
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
