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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
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
}
