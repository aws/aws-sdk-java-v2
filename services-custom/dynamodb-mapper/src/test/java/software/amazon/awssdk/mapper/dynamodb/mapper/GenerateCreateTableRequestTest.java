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

package software.amazon.awssdk.mapper.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.test.util.DynamoDBTestBase;
import software.amazon.awssdk.mapper.dynamodb.test.util.UnorderedCollectionComparator;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Tests on the DynamoDBMapper.generateCreateTableRequest method.
 */
public class GenerateCreateTableRequestTest extends DynamoDBTestBase {

    private static DynamoDBMapper mapper;

    @BeforeClass
    public static void setUp() {
        mapper = new DynamoDBMapper(getClient());
    }

    private static KeySchemaElement key(String name, KeyType type) {
        return KeySchemaElement.builder().attributeName(name).keyType(type).build();
    }

    private static AttributeDefinition attr(String name, ScalarAttributeType type) {
        return AttributeDefinition.builder().attributeName(name).attributeType(type).build();
    }

    private static LocalSecondaryIndex lsi(String name, KeySchemaElement... keys) {
        return LocalSecondaryIndex.builder().indexName(name).keySchema(keys).build();
    }

    private static GlobalSecondaryIndex gsi(String name, KeySchemaElement... keys) {
        return GlobalSecondaryIndex.builder().indexName(name).keySchema(keys).build();
    }

    @Test
    public void testParseIndexRangeKeyClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(IndexRangeKeyClass.class);

        assertEquals("aws-java-sdk-index-range-test", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                key("key", KeyType.HASH),
                key("rangeKey", KeyType.RANGE)
                );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                attr("key", ScalarAttributeType.N),
                attr("rangeKey", ScalarAttributeType.N),
                attr("indexFooRangeKey", ScalarAttributeType.N),
                attr("indexBarRangeKey", ScalarAttributeType.N),
                attr("multipleIndexRangeKey", ScalarAttributeType.N)
                );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                lsi("index_foo", key("key", KeyType.HASH), key("indexFooRangeKey", KeyType.RANGE)),
                lsi("index_bar", key("key", KeyType.HASH), key("indexBarRangeKey", KeyType.RANGE)),
                lsi("index_foo_copy", key("key", KeyType.HASH), key("multipleIndexRangeKey", KeyType.RANGE)),
                lsi("index_bar_copy", key("key", KeyType.HASH), key("multipleIndexRangeKey", KeyType.RANGE)));
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        assertTrue(request.globalSecondaryIndexes().isEmpty());
        assertNull(request.provisionedThroughput());
    }

    @Test
    public void testComplexIndexedHashRangeClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(MapperQueryExpressionTest.HashRangeClass.class);

        assertEquals("table_name", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                key("primaryHashKey", KeyType.HASH),
                key("primaryRangeKey", KeyType.RANGE)
                );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                attr("primaryHashKey", ScalarAttributeType.S),
                attr("indexHashKey", ScalarAttributeType.S),
                attr("primaryRangeKey", ScalarAttributeType.S),
                attr("indexRangeKey", ScalarAttributeType.S),
                attr("anotherIndexRangeKey", ScalarAttributeType.S)
                );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                lsi("LSI-primary-range", key("primaryHashKey", KeyType.HASH), key("primaryRangeKey", KeyType.RANGE)),
                lsi("LSI-index-range-1", key("primaryHashKey", KeyType.HASH), key("indexRangeKey", KeyType.RANGE)),
                lsi("LSI-index-range-2", key("primaryHashKey", KeyType.HASH), key("indexRangeKey", KeyType.RANGE)),
                lsi("LSI-index-range-3", key("primaryHashKey", KeyType.HASH), key("anotherIndexRangeKey", KeyType.RANGE)));
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        List<GlobalSecondaryIndex> expectedGsi = Arrays.asList(
                gsi("GSI-primary-hash-index-range-1", key("primaryHashKey", KeyType.HASH), key("indexRangeKey", KeyType.RANGE)),
                gsi("GSI-primary-hash-index-range-2",
                        key("primaryHashKey", KeyType.HASH), key("anotherIndexRangeKey", KeyType.RANGE)),
                gsi("GSI-index-hash-primary-range", key("indexHashKey", KeyType.HASH), key("primaryRangeKey", KeyType.RANGE)),
                gsi("GSI-index-hash-index-range-1", key("indexHashKey", KeyType.HASH), key("indexRangeKey", KeyType.RANGE)),
                gsi("GSI-index-hash-index-range-2", key("indexHashKey", KeyType.HASH), key("indexRangeKey", KeyType.RANGE)));
        assertTrue(equalGsi(expectedGsi, request.globalSecondaryIndexes()));

        assertNull(request.provisionedThroughput());
    }

    private static boolean equalLsi(Collection<LocalSecondaryIndex> a, Collection<LocalSecondaryIndex> b) {
        return UnorderedCollectionComparator.equalUnorderedCollections(a, b, new LocalSecondaryIndexDefinitionComparator());
    }

    private static boolean equalGsi(Collection<GlobalSecondaryIndex> a, Collection<GlobalSecondaryIndex> b) {
        return UnorderedCollectionComparator.equalUnorderedCollections(a, b, new GlobalSecondaryIndexDefinitionComparator());
    }

    private static class LocalSecondaryIndexDefinitionComparator
            implements
            UnorderedCollectionComparator.CrossTypeComparator<LocalSecondaryIndex, LocalSecondaryIndex> {

        @Override
        public boolean equals(LocalSecondaryIndex a, LocalSecondaryIndex b) {
            return a.indexName().equals(b.indexName())
                    && a.keySchema().equals(b.keySchema());
        }

    }

    private static class GlobalSecondaryIndexDefinitionComparator
            implements
            UnorderedCollectionComparator.CrossTypeComparator<GlobalSecondaryIndex, GlobalSecondaryIndex> {

        @Override
        public boolean equals(GlobalSecondaryIndex a, GlobalSecondaryIndex b) {
            return a.indexName().equals(b.indexName())
                    && a.keySchema().equals(b.keySchema());
        }
    }
}
