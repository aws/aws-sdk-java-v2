/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.auth.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.test.util.UnorderedCollectionComparator;
import utils.test.util.DynamoDBTestBase;

/**
 * Tests on the DynamoDBMapper.generateCreateTableRequest method.
 */
@Ignore // FIXME: setup fails with "region cannot be null"
public class GenerateCreateTableRequestTest extends DynamoDBTestBase {

    private static DynamoDbMapper mapper;

    @BeforeClass
    public static void setUp() {
        dynamo = DynamoDBClient.builder()
                .credentialsProvider(new AnonymousCredentialsProvider())
                .region(Region.US_WEST_2)
                .build();
        mapper = new DynamoDbMapper(dynamo);
    }

    private static boolean equalLsi(Collection<LocalSecondaryIndex> a, Collection<LocalSecondaryIndex> b) {
        return UnorderedCollectionComparator.equalUnorderedCollections(a, b, new LocalSecondaryIndexDefinitionComparator());
    }

    private static boolean equalGsi(Collection<GlobalSecondaryIndex> a, Collection<GlobalSecondaryIndex> b) {
        return UnorderedCollectionComparator.equalUnorderedCollections(a, b, new GlobalSecondaryIndexDefinitionComparator());
    }

    @Test
    public void testParseIndexRangeKeyClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(IndexRangeKeyClass.class);

        assertEquals("aws-java-sdk-index-range-test", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("rangeKey").keyType(KeyType.RANGE).build()
                                                                  );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                AttributeDefinition.builder().attributeName("key").attributeType(ScalarAttributeType.N).build(),
                AttributeDefinition.builder().attributeName("rangeKey").attributeType(ScalarAttributeType.N).build(),
                AttributeDefinition.builder().attributeName("indexFooRangeKey").attributeType(ScalarAttributeType.N).build(),
                AttributeDefinition.builder().attributeName("indexBarRangeKey").attributeType(ScalarAttributeType.N).build(),
                AttributeDefinition.builder().attributeName("multipleIndexRangeKey").attributeType(ScalarAttributeType.N).build()
                                                                         );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                LocalSecondaryIndex.builder()
                        .indexName("index_foo")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("indexFooRangeKey").keyType(KeyType.RANGE).build()).build(),
                LocalSecondaryIndex.builder()
                        .indexName("index_bar")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("indexBarRangeKey").keyType(KeyType.RANGE).build()).build(),
                LocalSecondaryIndex.builder()
                        .indexName("index_foo_copy")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("multipleIndexRangeKey").keyType(KeyType.RANGE).build()).build(),
                LocalSecondaryIndex.builder()
                        .indexName("index_bar_copy")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("multipleIndexRangeKey").keyType(KeyType.RANGE).build()).build());
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        assertNull(request.globalSecondaryIndexes());
        assertNull(request.provisionedThroughput());
    }

    @Test
    public void testComplexIndexedHashRangeClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(MapperQueryExpressionTest.HashRangeClass.class);

        assertEquals("table_name", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                KeySchemaElement.builder().attributeName("primaryHashKey").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("primaryRangeKey").keyType(KeyType.RANGE).build()
                                                                  );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                AttributeDefinition.builder().attributeName("primaryHashKey").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("indexHashKey").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("primaryRangeKey").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("indexRangeKey").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("anotherIndexRangeKey").attributeType(ScalarAttributeType.S).build()
                                                                         );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                LocalSecondaryIndex.builder()
                        .indexName("LSI-primary-range")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("primaryHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("primaryRangeKey").keyType(KeyType.RANGE).build()).build(),
                LocalSecondaryIndex.builder()
                        .indexName("LSI-index-range-1")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("primaryHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("indexRangeKey").keyType(KeyType.RANGE).build()).build(),
                LocalSecondaryIndex.builder()
                        .indexName("LSI-index-range-2")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("primaryHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("indexRangeKey").keyType(KeyType.RANGE).build()).build(),
                LocalSecondaryIndex.builder()
                        .indexName("LSI-index-range-3")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("primaryHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("anotherIndexRangeKey").keyType(KeyType.RANGE).build()).build());
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        List<GlobalSecondaryIndex> expectedGsi = Arrays.asList(
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-primary-hash-index-range-1")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("primaryHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("indexRangeKey").keyType(KeyType.RANGE).build()).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-primary-hash-index-range-2")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("primaryHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("anotherIndexRangeKey").keyType(KeyType.RANGE).build()).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-index-hash-primary-range")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("indexHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("primaryRangeKey").keyType(KeyType.RANGE).build()).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-index-hash-index-range-1")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("indexHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("indexRangeKey").keyType(KeyType.RANGE).build()).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-index-hash-index-range-2")
                        .keySchema(
                                KeySchemaElement.builder().attributeName("indexHashKey").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("indexRangeKey").keyType(KeyType.RANGE).build()).build());
        assertTrue(equalGsi(expectedGsi, request.globalSecondaryIndexes()));

        assertNull(request.provisionedThroughput());
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
