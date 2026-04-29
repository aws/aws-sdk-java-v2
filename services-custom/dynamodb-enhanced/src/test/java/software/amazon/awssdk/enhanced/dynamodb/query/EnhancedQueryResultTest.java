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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.query.result.DefaultEnhancedQueryResult;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryResult;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;

/**
 * Unit tests for {@link DefaultEnhancedQueryResult}.
 */
public class EnhancedQueryResultTest {

    @Test
    public void fromList_iterationReturnsRowsInOrder() {
        EnhancedQueryRow row1 = EnhancedQueryRow.builder().build();
        EnhancedQueryRow row2 = EnhancedQueryRow.builder().build();
        List<EnhancedQueryRow> list = new ArrayList<>();
        list.add(row1);
        list.add(row2);

        EnhancedQueryResult result = new DefaultEnhancedQueryResult(list);

        List<EnhancedQueryRow> collected = new ArrayList<>();
        result.forEach(collected::add);
        assertThat(collected).hasSize(2);
        assertThat(collected.get(0)).isSameAs(row1);
        assertThat(collected.get(1)).isSameAs(row2);
    }

    @Test
    public void stream_collectsSameRows() {
        EnhancedQueryRow row1 = EnhancedQueryRow.builder().build();
        List<EnhancedQueryRow> list = new ArrayList<>();
        list.add(row1);

        EnhancedQueryResult result = new DefaultEnhancedQueryResult(list);

        List<EnhancedQueryRow> fromStream = result.stream().collect(Collectors.toList());
        assertThat(fromStream).hasSize(1);
        assertThat(fromStream.get(0)).isSameAs(row1);
    }

    @Test
    public void emptyList_iterationReturnsNothing() {
        EnhancedQueryResult result = new DefaultEnhancedQueryResult(new ArrayList<>());

        List<EnhancedQueryRow> collected = new ArrayList<>();
        result.forEach(collected::add);
        assertThat(collected).isEmpty();
    }
}
