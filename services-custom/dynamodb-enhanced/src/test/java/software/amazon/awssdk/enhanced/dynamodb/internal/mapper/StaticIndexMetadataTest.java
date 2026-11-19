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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata.Builder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;

class StaticIndexMetadataTest {

    @Test
    void builder_shouldReturnNewBuilderInstance() {
        Builder builder = StaticIndexMetadata.builder();
        assertThat(builder).isNotNull();
    }

    @Test
    void builderFrom_withNullIndex_shouldReturnEmptyBuilder() {
        Builder builder = StaticIndexMetadata.builderFrom(null);

        assertThat(builder).isNotNull();
        assertThat(builder.build().name()).isNull();
        assertThat(builder.build().partitionKeys()).isEmpty();
        assertThat(builder.build().sortKeys()).isEmpty();
    }

    @Test
    void builderFrom_withValidIndex_shouldCopyAllProperties() {
        IndexMetadata sourceIndex = mock(IndexMetadata.class);
        KeyAttributeMetadata partitionKey = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata sortKey = createMockKeyAttribute(Order.FIRST);

        when(sourceIndex.name()).thenReturn("test-index");
        when(sourceIndex.partitionKeys()).thenReturn(Collections.singletonList(partitionKey));
        when(sourceIndex.sortKeys()).thenReturn(Collections.singletonList(sortKey));

        StaticIndexMetadata result = StaticIndexMetadata.builderFrom(sourceIndex).build();

        assertThat(result.name()).isEqualTo("test-index");
        assertThat(result.partitionKeys()).containsExactly(partitionKey);
        assertThat(result.sortKeys()).containsExactly(sortKey);
    }

    @Test
    void build_shouldSortPartitionKeysByOrder() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.THIRD);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key3 = createMockKeyAttribute(Order.SECOND);

        StaticIndexMetadata result = StaticIndexMetadata.builder()
                                                        .partitionKeys(Arrays.asList(key1, key2, key3))
                                                        .build();

        assertThat(result.partitionKeys()).containsExactly(key2, key3, key1);
    }

    @Test
    void build_shouldSortSortKeysByOrder() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.THIRD);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key3 = createMockKeyAttribute(Order.SECOND);

        StaticIndexMetadata result = StaticIndexMetadata.builder()
                                                        .sortKeys(Arrays.asList(key1, key2, key3))
                                                        .build();

        assertThat(result.sortKeys()).containsExactly(key2, key3, key1);
    }

    @Test
    void name_shouldReturnConfiguredName() {
        StaticIndexMetadata metadata = StaticIndexMetadata.builder()
                                                          .name("test-name")
                                                          .build();

        assertThat(metadata.name()).isEqualTo("test-name");
    }

    @Test
    void name_shouldReturnNullWhenNotSet() {
        StaticIndexMetadata metadata = StaticIndexMetadata.builder().build();
        assertThat(metadata.name()).isNull();
    }

    @Test
    void partitionKeys_shouldReturnUnmodifiableList() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);
        StaticIndexMetadata metadata = StaticIndexMetadata.builder()
                                                          .addPartitionKey(key)
                                                          .build();

        List<KeyAttributeMetadata> partitionKeys = metadata.partitionKeys();
        assertThat(partitionKeys).containsExactly(key);
    }

    @Test
    void sortKeys_shouldReturnUnmodifiableList() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);
        StaticIndexMetadata metadata = StaticIndexMetadata.builder()
                                                          .addSortKey(key)
                                                          .build();

        List<KeyAttributeMetadata> sortKeys = metadata.sortKeys();
        assertThat(sortKeys).containsExactly(key);
    }

    @Test
    void builderName_shouldSetName() {
        Builder builder = StaticIndexMetadata.builder().name("test");
        assertThat(builder.build().name()).isEqualTo("test");
    }

    @Test
    void builderPartitionKeys_shouldReplaceExistingKeys() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        Builder builder = StaticIndexMetadata.builder()
                                             .addPartitionKey(key1)
                                             .partitionKeys(Collections.singletonList(key2));

        assertThat(builder.build().partitionKeys()).containsExactly(key2);
    }

    @Test
    void builderSortKeys_shouldReplaceExistingKeys() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        Builder builder = StaticIndexMetadata.builder()
                                             .addSortKey(key1)
                                             .sortKeys(Collections.singletonList(key2));

        assertThat(builder.build().sortKeys()).containsExactly(key2);
    }

    @Test
    void builderAddPartitionKey_shouldAppendKey() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        Builder builder = StaticIndexMetadata.builder()
                                             .addPartitionKey(key1)
                                             .addPartitionKey(key2);

        assertThat(builder.build().partitionKeys()).containsExactly(key1, key2);
    }

    @Test
    void builderAddSortKey_shouldAppendKey() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        Builder builder = StaticIndexMetadata.builder()
                                             .addSortKey(key1)
                                             .addSortKey(key2);

        assertThat(builder.build().sortKeys()).containsExactly(key1, key2);
    }

    @Test
    void builderGetPartitionKeys_shouldReturnCopyOfKeys() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);
        Builder builder = StaticIndexMetadata.builder().addPartitionKey(key);

        List<KeyAttributeMetadata> keys = builder.getPartitionKeys();

        assertThat(keys).containsExactly(key);
        keys.clear();
        assertThat(builder.getPartitionKeys()).containsExactly(key);
    }

    @Test
    void builderGetSortKeys_shouldReturnCopyOfKeys() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);
        Builder builder = StaticIndexMetadata.builder().addSortKey(key);

        List<KeyAttributeMetadata> keys = builder.getSortKeys();

        assertThat(keys).containsExactly(key);
        keys.clear();
        assertThat(builder.getSortKeys()).containsExactly(key);
    }

    @Test
    void builderPartitionKey_shouldReplaceWithSingleKey() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        Builder builder = StaticIndexMetadata.builder()
                                             .addPartitionKey(key1)
                                             .partitionKey(key2);

        assertThat(builder.build().partitionKeys()).containsExactly(key2);
    }

    @Test
    void builderPartitionKey_withNull_shouldClearKeys() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);

        Builder builder = StaticIndexMetadata.builder()
                                             .addPartitionKey(key)
                                             .partitionKey(null);

        assertThat(builder.build().partitionKeys()).isEmpty();
    }

    @Test
    void builderSortKey_shouldReplaceWithSingleKey() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        Builder builder = StaticIndexMetadata.builder()
                                             .addSortKey(key1)
                                             .sortKey(key2);

        assertThat(builder.build().sortKeys()).containsExactly(key2);
    }

    @Test
    void builderSortKey_withNull_shouldClearKeys() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);

        Builder builder = StaticIndexMetadata.builder()
                                             .addSortKey(key)
                                             .sortKey(null);

        assertThat(builder.build().sortKeys()).isEmpty();
    }

    @Test
    void equals_withSameInstance_shouldReturnTrue() {
        StaticIndexMetadata metadata = StaticIndexMetadata.builder().build();
        assertThat(metadata.equals(metadata)).isTrue();
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        StaticIndexMetadata metadata = StaticIndexMetadata.builder().build();
        assertThat(metadata.equals(null)).isFalse();
    }

    @Test
    void equals_withDifferentClass_shouldReturnFalse() {
        StaticIndexMetadata metadata = StaticIndexMetadata.builder().build();
        assertThat(metadata.equals("string")).isFalse();
    }

    @Test
    void equals_withSameProperties_shouldReturnTrue() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);
        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder()
                                                           .name("test")
                                                           .addPartitionKey(key)
                                                           .addSortKey(key)
                                                           .build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder()
                                                           .name("test")
                                                           .addPartitionKey(key)
                                                           .addSortKey(key)
                                                           .build();

        assertThat(metadata1.equals(metadata2)).isTrue();
    }

    @Test
    void equals_withDifferentName_shouldReturnFalse() {
        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder().name("test1").build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder().name("test2").build();
        assertThat(metadata1.equals(metadata2)).isFalse();
    }

    @Test
    void equals_withNullNameVsNonNull_shouldReturnFalse() {
        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder().build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder().name("test").build();
        assertThat(metadata1.equals(metadata2)).isFalse();
    }

    @Test
    void equals_withNonNullNameVsNull_shouldReturnFalse() {
        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder().name("test").build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder().build();
        assertThat(metadata1.equals(metadata2)).isFalse();
    }

    @Test
    void equals_withBothNullNames_shouldReturnTrue() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);
        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder().addPartitionKey(key).build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder().addPartitionKey(key).build();
        assertThat(metadata1.equals(metadata2)).isTrue();
    }

    @Test
    void equals_withDifferentPartitionKeys_shouldReturnFalse() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder().addPartitionKey(key1).build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder().addPartitionKey(key2).build();
        assertThat(metadata1.equals(metadata2)).isFalse();
    }

    @Test
    void equals_withDifferentSortKeys_shouldReturnFalse() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder().addSortKey(key1).build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder().addSortKey(key2).build();
        assertThat(metadata1.equals(metadata2)).isFalse();
    }

    @Test
    void hashCode_withSameProperties_shouldReturnSameValue() {
        KeyAttributeMetadata key = createMockKeyAttribute(Order.FIRST);
        StaticIndexMetadata metadata1 = StaticIndexMetadata.builder()
                                                           .name("test")
                                                           .addPartitionKey(key)
                                                           .addSortKey(key)
                                                           .build();
        StaticIndexMetadata metadata2 = StaticIndexMetadata.builder()
                                                           .name("test")
                                                           .addPartitionKey(key)
                                                           .addSortKey(key)
                                                           .build();

        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
    }

    @Test
    void hashCode_withNullName_shouldNotThrow() {
        StaticIndexMetadata metadata = StaticIndexMetadata.builder().build();
        assertThat(metadata.hashCode()).isNotNull();
    }

    @Test
    void hashCode_withMultipleKeys_shouldIncludeAllKeys() {
        KeyAttributeMetadata key1 = createMockKeyAttribute(Order.FIRST);
        KeyAttributeMetadata key2 = createMockKeyAttribute(Order.SECOND);

        StaticIndexMetadata metadata = StaticIndexMetadata.builder()
                                                          .addPartitionKey(key1)
                                                          .addPartitionKey(key2)
                                                          .addSortKey(key1)
                                                          .addSortKey(key2)
                                                          .build();

        assertThat(metadata.hashCode()).isNotNull();
    }

    private KeyAttributeMetadata createMockKeyAttribute(Order order) {
        KeyAttributeMetadata mock = mock(KeyAttributeMetadata.class);
        when(mock.order()).thenReturn(order);
        return mock;
    }
}