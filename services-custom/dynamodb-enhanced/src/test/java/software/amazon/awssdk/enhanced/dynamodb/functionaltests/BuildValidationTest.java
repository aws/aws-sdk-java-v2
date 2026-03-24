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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.AggregationFunction;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;

/**
 * Functional tests for build-time validation in {@link QueryExpressionBuilder#build()}. Verifies that incompatible option
 * combinations throw {@link IllegalStateException} with clear messages.
 */
public class BuildValidationTest extends LocalDynamoDbTestBase {

    private static class SimpleRecord {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    private static final TableSchema<SimpleRecord> SCHEMA =
        StaticTableSchema.builder(SimpleRecord.class)
                         .newItemSupplier(SimpleRecord::new)
                         .addAttribute(String.class,
                                       a -> a.name("id")
                                             .getter(SimpleRecord::getId)
                                             .setter(SimpleRecord::setId)
                                             .tags(primaryPartitionKey()))
                         .build();

    private DynamoDbTable<SimpleRecord> tableA;
    private DynamoDbTable<SimpleRecord> tableB;

    @Before
    public void setUp() {
        DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
                                                              .dynamoDbClient(localDynamoDb().createClient())
                                                              .build();
        tableA = client.table(getConcreteTableName("table_a"), SCHEMA);
        tableB = client.table(getConcreteTableName("table_b"), SCHEMA);
    }

    @Test
    public void groupByWithoutAggregate_throwsWithMessage() {
        assertThatThrownBy(() ->
                               QueryExpressionBuilder.from(tableA)
                                                     .groupBy("id")
                                                     .build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("groupBy() requires at least one aggregate()");
    }

    @Test
    public void filterBaseWithoutJoin_throwsWithMessage() {
        assertThatThrownBy(() ->
                               QueryExpressionBuilder.from(tableA)
                                                     .filterBase(Condition.eq("status", "ACTIVE"))
                                                     .build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("filterBase() is only applicable when a join is configured");
    }

    @Test
    public void filterJoinedWithoutJoin_throwsWithMessage() {
        assertThatThrownBy(() ->
                               QueryExpressionBuilder.from(tableA)
                                                     .filterJoined(Condition.eq("amount", 100))
                                                     .build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("filterJoined() is only applicable when a join is configured");
    }

    @Test
    public void validSingleTableQuery_doesNotThrow() {
        QueryExpressionBuilder.from(tableA)
                              .where(Condition.eq("status", "ACTIVE"))
                              .build();
    }

    @Test
    public void validJoinQuery_doesNotThrow() {
        QueryExpressionBuilder.from(tableA)
                              .join(tableB, JoinType.INNER, "id", "id")
                              .filterBase(Condition.eq("status", "ACTIVE"))
                              .filterJoined(Condition.gt("amount", 0))
                              .build();
    }

    @Test
    public void validAggregationQuery_doesNotThrow() {
        QueryExpressionBuilder.from(tableA)
                              .groupBy("status")
                              .aggregate(AggregationFunction.COUNT, "id", "total")
                              .build();
    }

    @Test
    public void validJoinWithAggregation_doesNotThrow() {
        QueryExpressionBuilder.from(tableA)
                              .join(tableB, JoinType.INNER, "id", "id")
                              .groupBy("id")
                              .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                              .build();
    }

    @Test
    public void aggregateWithoutGroupBy_doesNotThrow() {
        QueryExpressionBuilder.from(tableA)
                              .aggregate(AggregationFunction.COUNT, "id", "total")
                              .build();
    }
}
