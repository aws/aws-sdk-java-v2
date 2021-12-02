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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class EnhancedClientUtilsTest {
    private static final AttributeValue PARTITION_VALUE = AttributeValue.builder().s("id123").build();
    private static final AttributeValue SORT_VALUE = AttributeValue.builder().s("sort123").build();

    @Test
    public void createKeyFromMap_partitionOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);

        Key key = EnhancedClientUtils.createKeyFromMap(itemMap,
                                                       FakeItem.getTableSchema(),
                                                       TableMetadata.primaryIndexName());

        assertThat(key.partitionKeyValue()).isEqualTo(PARTITION_VALUE);
        assertThat(key.sortKeyValue()).isEmpty();
    }

    @Test
    public void createKeyFromMap_partitionAndSort() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);
        itemMap.put("sort", SORT_VALUE);

        Key key = EnhancedClientUtils.createKeyFromMap(itemMap,
                                                       FakeItemWithSort.getTableSchema(),
                                                       TableMetadata.primaryIndexName());

        assertThat(key.partitionKeyValue()).isEqualTo(PARTITION_VALUE);
        assertThat(key.sortKeyValue()).isEqualTo(Optional.of(SORT_VALUE));
    }

    @Test
    public void cleanAttributeName_cleansSpecialCharacters() {
        String result = EnhancedClientUtils.cleanAttributeName("a*b.c-d:e#f");
        
        assertThat(result).isEqualTo("a_b_c_d_e_f");
    }
}