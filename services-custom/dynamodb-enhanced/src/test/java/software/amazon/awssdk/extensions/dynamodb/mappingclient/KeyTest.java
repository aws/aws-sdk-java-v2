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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class KeyTest {
    private final Key key = Key.create(stringValue("id123"), stringValue("id456"));
    private final Key partitionOnlyKey = Key.create(stringValue("id123"));

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
}
