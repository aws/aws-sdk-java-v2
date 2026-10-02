package software.amazon.awssdk.enhanced.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.TableIndices;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;

public class TableIndicesTest {

    @Test
    public void testLocalSecondaryIndices_onlyIncludesLSIs() {
        List<IndexMetadata> indices = Arrays.asList(StaticIndexMetadata.builder()
                                                                       .name("lsi-1")
                                                                       .build(),
                                                    StaticIndexMetadata.builder()
                                                                       .name("lsi-2")
                                                                       .build(),
                                                    StaticIndexMetadata.builder()
                                                                       .name("gsi-1")
                                                                       .partitionKey(StaticKeyAttributeMetadata.create(
                                                                           "GlobalIndexPartitionKey",
                                                                           AttributeValueType.N))
                                                                       .build());

        TableIndices tableIndices = new TableIndices(indices);

        List<EnhancedLocalSecondaryIndex> lsiList = tableIndices.localSecondaryIndices();

        assertEquals(2, lsiList.size());
        assertTrue(lsiList.stream().anyMatch(i -> "lsi-1".equals(i.indexName())));
        assertTrue(lsiList.stream().anyMatch(i -> "lsi-2".equals(i.indexName())));
    }

    @Test
    public void testGlobalSecondaryIndices_onlyIncludesGSIs() {
        List<IndexMetadata> indices = Arrays.asList(StaticIndexMetadata.builder()
                                                                       .name("lsi-1")
                                                                       .build(),
                                                    StaticIndexMetadata.builder()
                                                                       .name("gsi-1")
                                                                       .partitionKey(StaticKeyAttributeMetadata.create(
                                                                           "GlobalIndexPartitionKey1",
                                                                           AttributeValueType.N))
                                                                       .build(),
                                                    StaticIndexMetadata.builder()
                                                                       .name("gsi-2")
                                                                       .partitionKey(StaticKeyAttributeMetadata.create(
                                                                           "GlobalIndexPartitionKey2",
                                                                           AttributeValueType.N))
                                                                       .build());

        TableIndices tableIndices = new TableIndices(indices);

        List<EnhancedGlobalSecondaryIndex> gsiList = tableIndices.globalSecondaryIndices();

        assertEquals(2, gsiList.size());
        assertTrue(gsiList.stream().anyMatch(i -> "gsi-1".equals(i.indexName())));
        assertTrue(gsiList.stream().anyMatch(i -> "gsi-2".equals(i.indexName())));
    }

    @Test
    public void testPrimaryIndexIsExcluded() {
        List<IndexMetadata> indices = Arrays.asList(StaticIndexMetadata.builder()
                                                                       .name(TableMetadata.primaryIndexName())
                                                                       .partitionKey(StaticKeyAttributeMetadata.create("pk",
                                                                                                                       AttributeValueType.S))
                                                                       .build(),
                                                    StaticIndexMetadata.builder()
                                                                       .name("lsi-1")
                                                                       .build(),
                                                    StaticIndexMetadata.builder()
                                                                       .name("gsi-1")
                                                                       .partitionKey(StaticKeyAttributeMetadata.create(
                                                                           "GlobalIndexPartitionKey",
                                                                           AttributeValueType.N))
                                                                       .build());

        TableIndices tableIndices = new TableIndices(indices);

        List<EnhancedGlobalSecondaryIndex> gsiList = tableIndices.globalSecondaryIndices();
        List<EnhancedLocalSecondaryIndex> lsiList = tableIndices.localSecondaryIndices();

        assertEquals(1, gsiList.size());
        assertEquals("gsi-1", gsiList.get(0).indexName());

        assertEquals(1, lsiList.size());
        assertEquals("lsi-1", lsiList.get(0).indexName());
    }

    @Test
    public void testEmptyIndexList() {
        TableIndices tableIndices = new TableIndices(Collections.emptyList());

        assertTrue(tableIndices.globalSecondaryIndices().isEmpty());
        assertTrue(tableIndices.localSecondaryIndices().isEmpty());
    }
}
