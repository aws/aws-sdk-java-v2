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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithCompositeGsi;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class KeyTest {
    private final Key key = Key.builder().partitionValue("id123").sortValue("id456").build();
    private final Key partitionOnlyKey = Key.builder().partitionValue("id123").build();

    @Test
    public void getKeyMap() {
        Map<String, AttributeValue> expectedResult = new HashMap<>();
        expectedResult.put("gsi_id", AttributeValue.builder().s("id123").build());
        expectedResult.put("gsi_sort", AttributeValue.builder().s("id456").build());
        assertThat(key.keyMap(FakeItemWithIndices.getTableSchema(), "gsi_1"), is(expectedResult));
    }

    @Test
    public void getPrimaryKeyMap() {
        Map<String, AttributeValue> expectedResult = new HashMap<>();
        expectedResult.put("id", AttributeValue.builder().s("id123").build());
        expectedResult.put("sort", AttributeValue.builder().s("id456").build());
        assertThat(key.primaryKeyMap(FakeItemWithIndices.getTableSchema()), is(expectedResult));
    }

    @Test
    public void getPartitionKeyValue() {
        assertThat(key.partitionKeyValue(),
                   is(AttributeValue.builder().s("id123").build()));
    }

    @Test
    public void getSortKeyValue() {
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().s("id456").build())));
    }

    @Test
    public void getKeyMap_partitionOnly() {
        Map<String, AttributeValue> expectedResult = new HashMap<>();
        expectedResult.put("gsi_id", AttributeValue.builder().s("id123").build());
        assertThat(partitionOnlyKey.keyMap(FakeItemWithIndices.getTableSchema(), "gsi_1"), is(expectedResult));
    }

    @Test
    public void getPrimaryKeyMap_partitionOnly() {
        Map<String, AttributeValue> expectedResult = new HashMap<>();
        expectedResult.put("id", AttributeValue.builder().s("id123").build());
        assertThat(partitionOnlyKey.primaryKeyMap(FakeItemWithIndices.getTableSchema()), is(expectedResult));
    }

    @Test
    public void getPartitionKeyValue_partitionOnly() {
        assertThat(partitionOnlyKey.partitionKeyValue(),
                   is(AttributeValue.builder().s("id123").build()));
    }

    @Test
    public void getSortKeyValue_partitionOnly() {
        assertThat(partitionOnlyKey.sortKeyValue(), is(Optional.empty()));
    }

    @Test
    public void numericKeys_convertsToCorrectAttributeValue() {
        Key key = Key.builder().partitionValue(123).sortValue(45.6).build();

        assertThat(key.partitionKeyValue(), is(AttributeValue.builder().n("123").build()));
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().n("45.6").build())));
    }

    @Test
    public void stringKeys_convertsToCorrectAttributeValue() {
        Key key = Key.builder().partitionValue("one").sortValue("two").build();

        assertThat(key.partitionKeyValue(), is(AttributeValue.builder().s("one").build()));
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().s("two").build())));
    }

    @Test
    public void binaryKeys_convertsToCorrectAttributeValue() {
        SdkBytes partition = SdkBytes.fromString("one", StandardCharsets.UTF_8);
        SdkBytes sort = SdkBytes.fromString("two", StandardCharsets.UTF_8);

        Key key = Key.builder().partitionValue(partition).sortValue(sort).build();

        assertThat(key.partitionKeyValue(), is(AttributeValue.builder().b(partition).build()));
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().b(sort).build())));
    }

    @Test
    public void toBuilder() {
        Key keyClone = key.toBuilder().build();

        assertThat(key, is(equalTo(keyClone)));
    }

    @Test
    public void nullPartitionKey_shouldThrowException() {
        AttributeValue attributeValue = null;
        assertThatThrownBy(() ->  Key.builder().partitionValue(attributeValue).build())
         .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("partitionValue should not be null");

        assertThatThrownBy(() ->  Key.builder().partitionValue(AttributeValue.builder().nul(true).build()).build())
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("partitionValue should not be null");
    }

    @Test
    public void compositePartitionKeys_buildsCorrectly() {
        List<AttributeValue> partitionValues = Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build()
        );
        
        Key key = Key.builder().partitionValues(partitionValues).build();
        
        assertThat(key.partitionKeyValues(), is(Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build()
        )));
        assertThat(key.sortKeyValues(), is(Collections.emptyList()));
    }

    @Test
    public void compositeSortKeys_buildsCorrectly() {
        List<AttributeValue> sortValues = Arrays.asList(
            AttributeValue.builder().s("sk1").build(),
            AttributeValue.builder().s("sk2").build()
        );
        
        Key key = Key.builder()
            .partitionValue("pk1")
            .sortValues(sortValues)
            .build();

        assertThat(key.partitionKeyValues(), is(Collections.singletonList(
            AttributeValue.builder().s("pk1").build()
        )));

        assertThat(key.sortKeyValues(), is(Arrays.asList(
            AttributeValue.builder().s("sk1").build(),
            AttributeValue.builder().s("sk2").build()
        )));
    }

    @Test
    public void compositeKeys_maxFourPartitionKeys() {
        List<AttributeValue> partitionValues = Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build(),
            AttributeValue.builder().s("pk3").build(),
            AttributeValue.builder().s("pk4").build()
        );
        
        Key key = Key.builder().partitionValues(partitionValues).build();
        assertThat(key.partitionKeyValues().size(), is(4));
    }

    @Test
    public void compositeKeys_exceedingMaxPartitionKeys_throwsException() {
        List<AttributeValue> partitionValues = Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build(),
            AttributeValue.builder().s("pk3").build(),
            AttributeValue.builder().s("pk4").build(),
            AttributeValue.builder().s("pk5").build()
        );
        
        assertThatThrownBy(() -> Key.builder().partitionValues(partitionValues).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum 4 partition keys supported");
    }

    @Test
    public void compositeKeys_maxFourSortKeys() {
        List<AttributeValue> sortValues = Arrays.asList(
            AttributeValue.builder().s("sk1").build(),
            AttributeValue.builder().s("sk2").build(),
            AttributeValue.builder().s("sk3").build(),
            AttributeValue.builder().s("sk4").build()
        );
        
        Key key = Key.builder()
            .partitionValue("pk1")
            .sortValues(sortValues)
            .build();
        assertThat(key.sortKeyValues().size(), is(4));
    }

    @Test
    public void compositeKeys_exceedingMaxSortKeys_throwsException() {
        List<AttributeValue> sortValues = Arrays.asList(
            AttributeValue.builder().s("sk1").build(),
            AttributeValue.builder().s("sk2").build(),
            AttributeValue.builder().s("sk3").build(),
            AttributeValue.builder().s("sk4").build(),
            AttributeValue.builder().s("sk5").build()
        );
        
        assertThatThrownBy(() -> Key.builder().partitionValue("pk1").sortValues(sortValues).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum 4 sort keys supported");
    }

    @Test
    public void compositeKeys_emptyPartitionValues_throwsException() {
        assertThatThrownBy(() -> Key.builder().partitionValues(Collections.emptyList()).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("partitionValues should not be null or empty");
    }

    @Test
    public void compositeKeys_nullPartitionValues_throwsException() {
        assertThatThrownBy(() -> Key.builder().partitionValues(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("partitionValues should not be null or empty");
    }

    @Test
    public void compositeKeys_nullSortValues_createsEmptyList() {
        Key key = Key.builder()
            .partitionValue("pk1")
            .sortValues(null)
            .build();
        
        assertThat(key.sortKeyValues(), is(Collections.emptyList()));
    }

    @Test
    public void compositeKeys_backwardCompatibility_partitionValue() {
        Key key = Key.builder().partitionValue("pk1").build();
        
        assertThat(key.partitionKeyValues(), is(Collections.singletonList(
            AttributeValue.builder().s("pk1").build()
        )));
        
        assertThat(key.partitionKeyValue(), is(
            AttributeValue.builder().s("pk1").build()
        ));
    }

    @Test
    public void compositeKeys_backwardCompatibility_sortValue() {
        Key key = Key.builder()
            .partitionValue("pk1")
            .sortValue("sk1")
            .build();
        
        assertThat(key.sortKeyValues(), is(Collections.singletonList(
            AttributeValue.builder().s("sk1").build()
        )));
        
        assertThat(key.sortKeyValue(), is(
            Optional.of(AttributeValue.builder().s("sk1").build())
        ));
    }

    @Test
    public void compositeKeys_toBuilder_preservesValues() {
        List<AttributeValue> partitionValues = Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build()
        );
        List<AttributeValue> sortValues = Collections.singletonList(
            AttributeValue.builder().s("sk1").build()
        );
        
        Key original = Key.builder()
            .partitionValues(partitionValues)
            .sortValues(sortValues)
            .build();
        
        Key rebuilt = original.toBuilder().build();
        
        assertThat(rebuilt.partitionKeyValues(), is(original.partitionKeyValues()));
        assertThat(rebuilt.sortKeyValues(), is(original.sortKeyValues()));
    }

    @Test
    public void compositeKeys_keyMap_withCompositeGsi() {
        List<AttributeValue> partitionValues = Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build()
        );

        List<AttributeValue> sortValues = Arrays.asList(
            AttributeValue.builder().s("sk1").build(),
            AttributeValue.builder().s("sk2").build()
        );

        Key key = Key.builder()
                     .partitionValues(partitionValues)
                     .sortValues(sortValues)
                     .build();
        
        Map<String, AttributeValue> keyMap = key.keyMap(FakeItemWithCompositeGsi.getTableSchema(), "composite_gsi");

        assertThat(keyMap.size(), is(4));
        assertThat(keyMap.get("gsi_pk1"), is(AttributeValue.builder().s("pk1").build()));
        assertThat(keyMap.get("gsi_pk2"), is(AttributeValue.builder().s("pk2").build()));
        assertThat(keyMap.get("gsi_sk1"), is(AttributeValue.builder().s("sk1").build()));
        assertThat(keyMap.get("gsi_sk2"), is(AttributeValue.builder().s("sk2").build()));
    }

    @Test
    public void compositeKeys_keyMap_moreValuesThanKeys_usesAvailableKeys() {
        List<AttributeValue> partitionValues = Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build(),  // Extra value
            AttributeValue.builder().s("pk3").build()   // Extra value
        );
        
        Key key = Key.builder().partitionValues(partitionValues).build();
        
        Map<String, AttributeValue> keyMap = key.keyMap(FakeItemWithIndices.getTableSchema(), "gsi_1");
        
        assertThat(keyMap.size(), is(1));
        assertThat(keyMap.get("gsi_id"), is(AttributeValue.builder().s("pk1").build()));
    }

    @Test
    public void compositeKeys_keyMap_sortValuesWithoutSortKeys_throwsException() {
        Key key = Key.builder()
            .partitionValue("pk1")
            .sortValues(Collections.singletonList(AttributeValue.builder().s("sk1").build()))
            .build();
        
        assertThatThrownBy(() -> key.keyMap(FakeItemWithIndices.getTableSchema(), "gsi_2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sort key values were supplied for an index that does not support sort keys");
    }

    @Test
    public void addPartitionValuesFromStrings_convertsCorrectly() {
        Key key = Key.builder().addPartitionValue("pk1")
                     .addPartitionValue("pk2")
                     .addPartitionValue("pk3")
                     .build();
        
        assertThat(key.partitionKeyValues(), is(Arrays.asList(
            AttributeValue.builder().s("pk1").build(),
            AttributeValue.builder().s("pk2").build(),
            AttributeValue.builder().s("pk3").build()
        )));
        assertThat(key.partitionKeyValue(), is(AttributeValue.builder().s("pk1").build()));
    }

    @Test
    public void addPartitionValue_null_throwsException() {
        assertThatThrownBy(() -> Key.builder().addPartitionValue(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Partition key value cannot be null");
    }

    @Test
    public void addSortValuesFromStrings_convertsCorrectly() {
        Key key = Key.builder()
            .partitionValue("pk1")
            .addSortValue("sk1")
            .addSortValue("sk2")
            .build();
        
        assertThat(key.sortKeyValues(), is(Arrays.asList(
            AttributeValue.builder().s("sk1").build(),
            AttributeValue.builder().s("sk2").build()
        )));
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().s("sk1").build())));
    }

    @Test
    public void addPartitionValuesFromNumbers_convertsCorrectly() {
        Key key = Key.builder()
            .addPartitionValue(123)
            .addPartitionValue(45.6)
            .addPartitionValue(789L)
            .build();

        assertThat(key.partitionKeyValues(), is(Arrays.asList(
            AttributeValue.builder().n("123").build(),
            AttributeValue.builder().n("45.6").build(),
            AttributeValue.builder().n("789").build()
        )));
        assertThat(key.partitionKeyValue(), is(AttributeValue.builder().n("123").build()));
    }

    @Test
    public void addSortValuesFromNumbers_convertsCorrectly() {
        Key key = Key.builder()
            .partitionValue("pk1")
            .addSortValue(100)
            .addSortValue(200.5)
            .build();
        
        assertThat(key.sortKeyValues(), is(Arrays.asList(
            AttributeValue.builder().n("100").build(),
            AttributeValue.builder().n("200.5").build()
        )));
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().n("100").build())));
    }

    @Test
    public void addPartitionValuesFromBinary_convertsCorrectly() {
        SdkBytes bytes1 = SdkBytes.fromString("data1", StandardCharsets.UTF_8);
        SdkBytes bytes2 = SdkBytes.fromString("data2", StandardCharsets.UTF_8);

        Key key = Key.builder()
                     .addPartitionValue(bytes1)
                     .addPartitionValue(bytes2)
                     .build();
        
        assertThat(key.partitionKeyValues(), is(Arrays.asList(
            AttributeValue.builder().b(bytes1).build(),
            AttributeValue.builder().b(bytes2).build()
        )));
        assertThat(key.partitionKeyValue(), is(AttributeValue.builder().b(bytes1).build()));
    }

    @Test
    public void addSortValuesFromBinary_convertsCorrectly() {
        SdkBytes bytes1 = SdkBytes.fromString("sort1", StandardCharsets.UTF_8);
        SdkBytes bytes2 = SdkBytes.fromString("sort2", StandardCharsets.UTF_8);

        Key key = Key.builder()
            .partitionValue("pk1")
            .addSortValue(bytes1)
            .addSortValue(bytes2)
            .build();
        
        assertThat(key.sortKeyValues(), is(Arrays.asList(
            AttributeValue.builder().b(bytes1).build(),
            AttributeValue.builder().b(bytes2).build()
        )));
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().b(bytes1).build())));
    }

    @Test
    public void addCompositeKeys_mixedTypes_partitionFromStrings_sortFromNumbers() {
        Key key = Key.builder()
            .addPartitionValue("tenant1")
            .addPartitionValue("region1")
            .addSortValue(2023)
            .addSortValue(1)
            .build();
        
        assertThat(key.partitionKeyValues(), is(Arrays.asList(
            AttributeValue.builder().s("tenant1").build(),
            AttributeValue.builder().s("region1").build()
        )));
        assertThat(key.sortKeyValues(), is(Arrays.asList(
            AttributeValue.builder().n("2023").build(),
            AttributeValue.builder().n("1").build()
        )));
    }

    @Test
    public void addCompositeKeys_mixedTypes_partitionFromNumbers_sortFromBinary() {
        SdkBytes sortBytes = SdkBytes.fromString("sortdata", StandardCharsets.UTF_8);
        
        Key key = Key.builder()
            .addPartitionValue(100)
            .addPartitionValue(200)
            .addSortValue(sortBytes)
            .build();
        
        assertThat(key.partitionKeyValues(), is(Arrays.asList(
            AttributeValue.builder().n("100").build(),
            AttributeValue.builder().n("200").build()
        )));
        assertThat(key.sortKeyValues(), is(Collections.singletonList(
            AttributeValue.builder().b(sortBytes).build()
        )));
    }

    @Test
    public void addCompositeKeys_fromStrings_maxFourPartitionKeys() {
        Key key = Key.builder()
            .addPartitionValue("pk1")
            .addPartitionValue("pk2")
            .addPartitionValue("pk3")
            .addPartitionValue("pk4")
            .build();
        assertThat(key.partitionKeyValues().size(), is(4));
    }

    @Test
    public void addCompositeKeys_moreThanMaxPartitionKeys_throwsException() {
        assertThatThrownBy(() -> Key.builder()
                                    .addPartitionValue("pk1")
                                    .addPartitionValue("pk2")
                                    .addPartitionValue("pk3")
                                    .addPartitionValue("pk4")
                                    .addPartitionValue("pk5")
                                    .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum 4 partition keys supported");
    }

    @Test
    public void addCompositeKeys_backwardCompatibility() {
        Key key = Key.builder()
            .addPartitionValue("pk1")
            .addPartitionValue("pk2")
            .addSortValue("sk1")
            .build();
        
        assertThat(key.partitionKeyValues().get(0), is(AttributeValue.builder().s("pk1").build()));
        assertThat(key.partitionKeyValues().get(1), is(AttributeValue.builder().s("pk2").build()));
        assertThat(key.sortKeyValues().get(0), is(AttributeValue.builder().s("sk1").build()));
        
        assertThat(key.partitionKeyValue(), is(AttributeValue.builder().s("pk1").build()));
        assertThat(key.sortKeyValue(), is(Optional.of(AttributeValue.builder().s("sk1").build())));
    }

    @Test
    public void addCompositeKeys_toBuilder_preservesValues() {
        Key original = Key.builder()
            .addPartitionValue("pk1")
            .addPartitionValue("pk2")
            .addSortValue("sk1")
            .build();
        
        Key rebuilt = original.toBuilder().build();
        
        assertThat(rebuilt.partitionKeyValues(), is(original.partitionKeyValues()));
        assertThat(rebuilt.sortKeyValues(), is(original.sortKeyValues()));
        assertThat(rebuilt.partitionKeyValue(), is(original.partitionKeyValue()));
        assertThat(rebuilt.sortKeyValue(), is(original.sortKeyValue()));
    }
}