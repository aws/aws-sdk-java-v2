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

package software.amazon.awssdk.enhanced.dynamodb.internal.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.KeyResolution;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class QueryConditionalUtilsTest {

    private static final TableSchema<FakeItem> SIMPLE_SCHEMA = FakeItem.getTableSchema();

    @Test
    void resolveKeys_singlePartitionKey_returnsCorrectResolution() {
        Key key = Key.builder().partitionValue("pk1").build();

        KeyResolution resolution = QueryConditionalUtils.resolveKeys(key, SIMPLE_SCHEMA, "$PRIMARY_INDEX");

        assertThat(resolution.partitionKeys).containsExactly("id");
        assertThat(resolution.partitionValues).containsExactly(AttributeValue.builder().s("pk1").build());
        assertThat(resolution.sortKeys).isEmpty();
        assertThat(resolution.sortValues).isEmpty();
    }

    @Test
    void keyResolution_constructor_validatesInputs() {
        List<String> partitionKeys = Collections.singletonList("pk1");
        List<AttributeValue> partitionValues = Collections.singletonList(AttributeValue.builder().s("val1").build());
        List<String> sortKeys = Collections.singletonList("sk1");
        List<AttributeValue> sortValues = Collections.singletonList(AttributeValue.builder().s("val2").build());

        KeyResolution resolution = new KeyResolution(partitionKeys, partitionValues, sortKeys, sortValues);

        assertThat(resolution.partitionKeys).isEqualTo(partitionKeys);
        assertThat(resolution.partitionValues).isEqualTo(partitionValues);
        assertThat(resolution.sortKeys).isEqualTo(sortKeys);
        assertThat(resolution.sortValues).isEqualTo(sortValues);
    }

    @Test
    void keyResolution_hasSortKeys_returnsTrueWhenBothPresent() {
        KeyResolution resolution = new KeyResolution(
            Collections.singletonList("pk1"),
            Collections.singletonList(AttributeValue.builder().s("val1").build()),
            Collections.singletonList("sk1"),
            Collections.singletonList(AttributeValue.builder().s("val2").build())
        );

        assertThat(resolution.hasSortKeys()).isTrue();
    }

    @Test
    void keyResolution_fieldsAreImmutable() {
        List<String> partitionKeys = Collections.singletonList("pk1");
        List<AttributeValue> partitionValues = Collections.singletonList(AttributeValue.builder().s("val1").build());

        KeyResolution resolution = new KeyResolution(partitionKeys, partitionValues, Collections.emptyList(),
                                                     Collections.emptyList());

        assertThatThrownBy(() -> resolution.partitionKeys.add("newKey"))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> resolution.partitionValues.add(AttributeValue.builder().s("newVal").build()))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}