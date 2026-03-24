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

package software.amazon.awssdk.enhanced.dynamodb.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;

/**
 * Unit tests for {@link EnhancedQueryRow} and its builder.
 */
public class EnhancedQueryRowTest {

    @Test
    public void emptyRow_returnsEmptyMapsAndNullAggregate() {
        EnhancedQueryRow row = EnhancedQueryRow.builder().build();

        assertThat(row.getItem("base")).isEmpty();
        assertThat(row.itemsByAlias()).isEmpty();
        assertThat(row.aggregates()).isEmpty();
        assertThat(row.getAggregate("any")).isNull();
    }

    @Test
    public void itemsByAlias_getItemReturnsCorrectMap() {
        Map<String, Object> baseMap = new HashMap<>();
        baseMap.put("customerId", "c1");
        baseMap.put("name", "Alice");
        Map<String, Map<String, Object>> byAlias = new HashMap<>();
        byAlias.put("base", baseMap);

        EnhancedQueryRow row = EnhancedQueryRow.builder()
                                               .itemsByAlias(byAlias)
                                               .build();

        assertThat(row.getItem("base"))
            .containsEntry("customerId", "c1")
            .containsEntry("name", "Alice");
        assertThat(row.itemsByAlias()).containsKey("base");
        assertThat(row.getItem("unknown")).isEmpty();
    }

    @Test
    public void aggregates_getAggregateReturnsValue() {
        Map<String, Object> aggs = new HashMap<>();
        aggs.put("orderCount", 5);
        aggs.put("totalAmount", 100.50);

        EnhancedQueryRow row = EnhancedQueryRow.builder()
                                               .aggregates(aggs)
                                               .build();

        assertThat(row.getAggregate("orderCount")).isEqualTo(5);
        assertThat(row.getAggregate("totalAmount")).isEqualTo(100.50);
        assertThat(row.aggregates())
            .containsEntry("orderCount", 5)
            .containsEntry("totalAmount", 100.50);
        assertThat(row.getAggregate("missing")).isNull();
    }

    @Test
    public void rowWithItemsAndAggregates_returnsBoth() {
        Map<String, Object> base = Collections.singletonMap("customerId", "c1");
        Map<String, Object> joined = Collections.singletonMap("orderId", "o1");
        Map<String, Map<String, Object>> byAlias = new HashMap<>();
        byAlias.put("base", base);
        byAlias.put("joined", joined);
        Map<String, Object> aggs = Collections.singletonMap("orderCount", 3);

        EnhancedQueryRow row = EnhancedQueryRow.builder()
                                               .itemsByAlias(byAlias)
                                               .aggregates(aggs)
                                               .build();

        assertThat(row.getItem("base")).containsEntry("customerId", "c1");
        assertThat(row.getItem("joined")).containsEntry("orderId", "o1");
        assertThat(row.getAggregate("orderCount")).isEqualTo(3);
    }
}
