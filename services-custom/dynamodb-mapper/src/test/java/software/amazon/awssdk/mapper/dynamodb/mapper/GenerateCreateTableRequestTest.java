package software.amazon.awssdk.mapper.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.mapper.dynamodb.test.util.DynamoDBTestBase;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.mapper.dynamodb.test.util.UnorderedCollectionComparator;

/**
 * Tests on the DynamoDBMapper.generateCreateTableRequest method.
 */
public class GenerateCreateTableRequestTest extends DynamoDBTestBase {

    private static DynamoDBMapper mapper;

    @BeforeClass
    public static void setUp() {
        // generateCreateTableRequest is a pure client-side operation, so no service client is needed.
        mapper = new DynamoDBMapper(null);
    }

    @Test
    public void testParseIndexRangeKeyClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(IndexRangeKeyClass.class);

        assertEquals("aws-java-sdk-index-range-test", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                keySchema("key", KeyType.HASH),
                keySchema("rangeKey", KeyType.RANGE)
                );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                attrDefinition("key", ScalarAttributeType.N),
                attrDefinition("rangeKey", ScalarAttributeType.N),
                attrDefinition("indexFooRangeKey", ScalarAttributeType.N),
                attrDefinition("indexBarRangeKey", ScalarAttributeType.N),
                attrDefinition("multipleIndexRangeKey", ScalarAttributeType.N)
                );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                LocalSecondaryIndex.builder()
                        .indexName("index_foo")
                        .keySchema(
                                keySchema("key", KeyType.HASH),
                                keySchema("indexFooRangeKey", KeyType.RANGE)).build(),
                LocalSecondaryIndex.builder()
                        .indexName("index_bar")
                        .keySchema(
                                keySchema("key", KeyType.HASH),
                                keySchema("indexBarRangeKey", KeyType.RANGE)).build(),
                LocalSecondaryIndex.builder()
                        .indexName("index_foo_copy")
                        .keySchema(
                                keySchema("key", KeyType.HASH),
                                keySchema("multipleIndexRangeKey", KeyType.RANGE)).build(),
                LocalSecondaryIndex.builder()
                        .indexName("index_bar_copy")
                        .keySchema(
                                keySchema("key", KeyType.HASH),
                                keySchema("multipleIndexRangeKey", KeyType.RANGE)).build());
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        assertTrue(request.globalSecondaryIndexes().isEmpty());
        assertNull(request.provisionedThroughput());
    }

    @Test
    public void testComplexIndexedHashRangeClass() {
        CreateTableRequest request = mapper.generateCreateTableRequest(MapperQueryExpressionTest.HashRangeClass.class);

        assertEquals("table_name", request.tableName());
        List<KeySchemaElement> expectedKeyElements = Arrays.asList(
                keySchema("primaryHashKey", KeyType.HASH),
                keySchema("primaryRangeKey", KeyType.RANGE)
                );
        assertEquals(expectedKeyElements, request.keySchema());

        List<AttributeDefinition> expectedAttrDefinitions = Arrays.asList(
                attrDefinition("primaryHashKey", ScalarAttributeType.S),
                attrDefinition("indexHashKey", ScalarAttributeType.S),
                attrDefinition("primaryRangeKey", ScalarAttributeType.S),
                attrDefinition("indexRangeKey", ScalarAttributeType.S),
                attrDefinition("anotherIndexRangeKey", ScalarAttributeType.S)
                );
        assertTrue(UnorderedCollectionComparator.equalUnorderedCollections(
                expectedAttrDefinitions,
                request.attributeDefinitions()));

        List<LocalSecondaryIndex> expectedLsi = Arrays.asList(
                LocalSecondaryIndex.builder()
                        .indexName("LSI-primary-range")
                        .keySchema(
                                keySchema("primaryHashKey", KeyType.HASH),
                                keySchema("primaryRangeKey", KeyType.RANGE)).build(),
                LocalSecondaryIndex.builder()
                        .indexName("LSI-index-range-1")
                        .keySchema(
                                keySchema("primaryHashKey", KeyType.HASH),
                                keySchema("indexRangeKey", KeyType.RANGE)).build(),
                LocalSecondaryIndex.builder()
                        .indexName("LSI-index-range-2")
                        .keySchema(
                                keySchema("primaryHashKey", KeyType.HASH),
                                keySchema("indexRangeKey", KeyType.RANGE)).build(),
                LocalSecondaryIndex.builder()
                        .indexName("LSI-index-range-3")
                        .keySchema(
                                keySchema("primaryHashKey", KeyType.HASH),
                                keySchema("anotherIndexRangeKey", KeyType.RANGE)).build());
        assertTrue(equalLsi(expectedLsi, request.localSecondaryIndexes()));

        List<GlobalSecondaryIndex> expectedGsi = Arrays.asList(
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-primary-hash-index-range-1")
                        .keySchema(
                                keySchema("primaryHashKey", KeyType.HASH),
                                keySchema("indexRangeKey", KeyType.RANGE)).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-primary-hash-index-range-2")
                        .keySchema(
                                keySchema("primaryHashKey", KeyType.HASH),
                                keySchema("anotherIndexRangeKey", KeyType.RANGE)).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-index-hash-primary-range")
                        .keySchema(
                                keySchema("indexHashKey", KeyType.HASH),
                                keySchema("primaryRangeKey", KeyType.RANGE)).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-index-hash-index-range-1")
                        .keySchema(
                                keySchema("indexHashKey", KeyType.HASH),
                                keySchema("indexRangeKey", KeyType.RANGE)).build(),
                GlobalSecondaryIndex.builder()
                        .indexName("GSI-index-hash-index-range-2")
                        .keySchema(
                                keySchema("indexHashKey", KeyType.HASH),
                                keySchema("indexRangeKey", KeyType.RANGE)).build());
        assertTrue(equalGsi(expectedGsi, request.globalSecondaryIndexes()));

        assertNull(request.provisionedThroughput());
    }

    private static KeySchemaElement keySchema(String attributeName, KeyType keyType) {
        return KeySchemaElement.builder().attributeName(attributeName).keyType(keyType).build();
    }

    private static AttributeDefinition attrDefinition(String attributeName, ScalarAttributeType type) {
        return AttributeDefinition.builder().attributeName(attributeName).attributeType(type).build();
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
