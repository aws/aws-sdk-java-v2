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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBeginsWith;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBetween;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortGreaterThan;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortGreaterThanOrEqualTo;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortLessThan;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortLessThanOrEqualTo;

import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.CompositeKeyRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.FlattenedRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

abstract class QueryGSICompositeKeysIntegrationTestBase extends DynamoDbEnhancedIntegrationTestBase {

    protected static DynamoDbClient dynamoDbClient;
    protected static DynamoDbEnhancedClient enhancedClient;
    protected static DynamoDbTable<CompositeKeyRecord> mappedTable;

    @AfterAll
    public static void teardown() {
        try {
            dynamoDbClient.deleteTable(r -> r.tableName(mappedTable.tableName()));
        } finally {
            dynamoDbClient.close();
        }
    }

    private static final java.util.List<CompositeKeyRecord> COMPOSITE_RECORDS = Arrays.asList(
        createRecord("id1", "sort1", "pk1", 100, "pk3", Instant.parse("2025-01-01T00:00:00Z"), "sk1", "sk2", Instant.parse(
            "2025-01-01T00:00:00Z"), 1, 80.5, "flpk3", "flsk2", Instant.parse("2025-01-01T12:00:00Z")),
        createRecord("id2", "sort2", "pk1", 100, "pk3", Instant.parse("2025-01-01T00:00:00Z"), "sk1", "sk2", Instant.parse(
            "2025-06-01T11:21:00Z"), 2, 20.9, "flpk3_b", "flsk2", Instant.parse("2025-06-01T11:21:00Z")),
        createRecord("id3", "sort3", "pk1", 200, "pk3", Instant.parse("2025-01-02T00:00:00Z"), "sk1", "sk2", Instant.parse(
            "2025-01-05T00:00:00Z"), 3, 50.0, "flpk3", "flsk2", Instant.parse("2025-01-05T00:00:00Z")),
        createRecord("id4", "sort4", "pk1", 100, "pk3", Instant.parse("2025-07-01T00:10:00Z"), "sk1", "sk2", Instant.parse(
            "2025-09-01T05:28:00Z"), 4, 75.3, "flpk3", "flsk2", Instant.parse("2025-09-01T05:28:00Z")),
        createRecord("id5", "sort5", "pk1", 100, "pk3", Instant.parse("2025-01-08T05:12:32Z"), "sk1", "sk2", Instant.parse(
            "2025-01-02T00:00:00Z"), 1, 50.0, "flpk3", "flsk2", Instant.parse("2025-01-01T00:00:00Z")),
        createRecord("id6", "sort6", "pk1", 100, "pk3", Instant.parse("2025-01-01T00:00:00Z"), "sk1", "sk2_prefix",
                     Instant.parse("2025-01-01T00:00:00Z"), 1, 60.0, "flpk3", "flsk2_prefix", Instant.parse("2025-01-01T00:00:00Z")),
        createRecord("id7", "sort7", "pk1", 100, "pk3", Instant.parse("2025-06-01T00:00:00Z"), "sk1_prefix", "sk2",
                     Instant.parse("2025-06-01T10:22:02Z"), 1, 70.0, "flpk3", "flsk2", Instant.parse("2025-06-01T10:22:02Z")),
        createRecord("id8", "sort8", "pk1", 100, "pk3", Instant.parse("2025-01-01T00:00:00Z"), "sk1", "sk2", Instant.parse(
            "2025-01-01T00:00:00Z"), 5, 90.4, "flpk3", "flsk2", Instant.parse("2025-01-01T00:00:00Z")),
        createRecord("id9", "sort9", "different_pk1", 300, "pk3", Instant.parse("2025-01-03T00:00:00Z"), "sk1", "sk2",
                     Instant.parse("2025-01-03T20:12:00Z"), 10, 40.2, "flpk3", "flsk2", Instant.parse("2025-01-03T20:12:00Z")),
        createRecord("id10", "sort10", "pk1", 100, "pk3", Instant.parse("2025-01-09T12:15:00Z"), "sk1", "sk2_b", Instant.parse(
            "2025-02-01T00:00:00Z"), 1, 55.5, "flpk3", "flsk2", Instant.parse("2025-01-01T00:00:00Z")),
        createRecord("id11", "sort11", "pk1", 100, "pk3", Instant.parse("2025-03-01T16:35:00Z"), "sk1", "sk2_c", Instant.parse(
            "2025-03-01T23:15:32Z"), 1, 65.5, "flpk3", "flsk2", Instant.parse("2025-03-01T23:15:32Z")),
        createRecord("id12", "sort12", "pk1", 100, "pk3", Instant.parse("2025-01-01T00:00:00Z"), "sk1", "sk2", Instant.parse(
            "2025-01-03T00:00:00Z"), 1, 85.0, "flpk3", "flsk2", Instant.parse("2025-01-01T00:00:00Z"))
    );

    protected static void insertRecords() {
        COMPOSITE_RECORDS.forEach(record -> mappedTable.putItem(r -> r.item(record)));
    }

    protected static void waitForGsiConsistency() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static CompositeKeyRecord createRecord(String id, String sort, String pk1, Integer pk2, String pk3,
                                                   Instant pk4, String sk1, String sk2, Instant sk3, Integer sk4,
                                                   Double flpk2, String flpk3, String flsk2, Instant flsk3) {
        CompositeKeyRecord record = new CompositeKeyRecord();
        record.setId(id);
        record.setSort(sort);
        record.setPk1(pk1);
        record.setPk2(pk2);
        record.setPk3(pk3);
        record.setPk4(pk4);
        record.setSk1(sk1);
        record.setSk2(sk2);
        record.setSk3(sk3);
        record.setSk4(sk4);
        record.setData("test-data");

        FlattenedRecord flattenedRecord = new FlattenedRecord();
        flattenedRecord.setFlpk2(flpk2);
        flattenedRecord.setFlpk3(flpk3);
        flattenedRecord.setFlsk2(flsk2);
        flattenedRecord.setFlsk3(flsk3);
        flattenedRecord.setFldata("fl-data");
        record.setFlattenedRecord(flattenedRecord);

        return record;
    }

    // GSI1: 1 partition key, 1 sort key
    @Test
    void queryGsi1_keyEqualTo() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi1")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(10));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(2)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi1_sortBeginsWith() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi1")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addSortValue("sk1_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi1_sortBetween() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi1")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1").addSortValue("sk1_"),
                                                                                               k -> k.addPartitionValue("pk1").addSortValue("sk1_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
    }

    @Test
    void queryGsi1_sortGreaterThan() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi1")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(k -> k.addPartitionValue("pk1")
                                                                                                                                   .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi1_sortGreaterThanOrEqual() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi1")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                                            .addSortValue("sk1_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi1_sortLessThan() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi1")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(k -> k.addPartitionValue("pk1")
                                                                                                                                .addSortValue("sk1_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(10));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(false));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(8)), is(false));
    }

    @Test
    void queryGsi1_sortLessThanOrEqual() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi1")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                                         .addSortValue("sk1_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(11));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(8)), is(false));
    }

    // GSI2: 2 partition keys, 2 sort keys
    @Test
    void queryGsi2_keyEqualTo_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("sk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(6));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi2_keyEqualTo_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addSortValue("sk1_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), is(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi2_sortBeginsWith_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("sk2_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi2_sortBeginsWith_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addSortValue("sk1_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), is(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi2_sortBetween_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_a"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_d")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi2_sortBetween_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addSortValue("sk1_"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addSortValue("sk1_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), is(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi2_sortGreaterThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(k -> k.addPartitionValue("pk1")
                                                                                                                                   .addPartitionValue(100)
                                                                                                                                   .addSortValue("sk1")
                                                                                                                                   .addSortValue("sk2_a")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi2_sortGreaterThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(k -> k.addPartitionValue("pk1")
                                                                                                                                   .addPartitionValue(100)
                                                                                                                                   .addSortValue("sk1_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), is(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi2_sortGreaterThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                                            .addPartitionValue(100)
                                                                                                                                            .addSortValue("sk1")
                                                                                                                                            .addSortValue("sk2_b")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi2_sortGreaterThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                                            .addPartitionValue(100)
                                                                                                                                            .addSortValue("sk1_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi2_sortLessThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(k -> k.addPartitionValue("pk1")
                                                                                                                                .addPartitionValue(100)
                                                                                                                                .addSortValue("sk1")
                                                                                                                                .addSortValue("sk2_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(6));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi2_sortLessThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(k -> k.addPartitionValue("pk1")
                                                                                                                                .addPartitionValue(100)
                                                                                                                                .addSortValue("sk1_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(9));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(2)), is(false));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(false));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(8)), is(false));
    }

    @Test
    void queryGsi2_sortLessThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                                         .addPartitionValue(100)
                                                                                                                                         .addSortValue("sk1")
                                                                                                                                         .addSortValue("sk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(6));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi2_sortLessThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi2")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                                         .addPartitionValue(100)
                                                                                                                                         .addSortValue("sk1_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(10));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(2)), is(false));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(8)), is(false));
    }

    // GSI3: 3 partition keys, 3 sort keys
    @Test
    void queryGsi3_keyEqualTo_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addPartitionValue("pk3")
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("sk2")
                                                                                                                              .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi3_keyEqualTo_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addPartitionValue("pk3")
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("sk2_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), is(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi3_keyEqualTo_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addPartitionValue("pk3")
                                                                                                                              .addSortValue("sk1_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), is(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi3_sortBeginsWith_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addPartitionValue("pk3")
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("sk2")
                                                                                                                                  .addSortValue(Instant.parse("2025-01-02T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(4)));
    }

    @Test
    void queryGsi3_sortBeginsWith_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addPartitionValue("pk3")
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("sk2_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi3_sortBeginsWith_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addPartitionValue("pk3")
                                                                                                                                  .addSortValue("sk1_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi3_sortBetween_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2024-12-31T00:00:00Z")),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addSortValue(Instant.parse("2025-01-03T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(4));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi3_sortBetween_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_a"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addSortValue("sk2_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi3_sortBetween_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1_"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addSortValue("sk1_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
    }

    @Test
    void queryGsi3_sortGreaterThan_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(k -> k.addPartitionValue("pk1")
                                                                                                                                   .addPartitionValue(100)
                                                                                                                                   .addPartitionValue("pk3")
                                                                                                                                   .addSortValue("sk1")
                                                                                                                                   .addSortValue("sk2")
                                                                                                                                   .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(4));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi3_sortGreaterThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_a")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi3_sortGreaterThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), is(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi3_sortGreaterThanOrEqual_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-02T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(4));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi3_sortGreaterThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_a")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
    }

    @Test
    void queryGsi3_sortGreaterThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(10));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi3_sortLessThan_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-02T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi3_sortLessThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_b")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(6));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi3_sortLessThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(10));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi3_sortLessThanOrEqual_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-02T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi3_sortLessThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_b")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(7));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi3_sortLessThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi3")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(9));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    // GSI4: 4 partition keys, 4 sort keys
    @Test
    void queryGsi4_keyEqualTo_fourSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addPartitionValue("pk3")
                                                                                                                              .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("sk2")
                                                                                                                              .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                              .addSortValue(1)))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(0)));
    }

    @Test
    void queryGsi4_keyEqualTo_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addPartitionValue("pk3")
                                                                                                                              .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("sk2")
                                                                                                                              .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_keyEqualTo_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addPartitionValue("pk3")
                                                                                                                              .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("sk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(4));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_keyEqualTo_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(100)
                                                                                                                              .addPartitionValue("pk3")
                                                                                                                              .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                              .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortBeginsWith_fourSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addPartitionValue("pk3")
                                                                                                                                  .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("sk2")
                                                                                                                                  .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_sortBeginsWith_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addPartitionValue("pk3")
                                                                                                                                  .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("sk2")
                                                                                                                                  .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_sortBeginsWith_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addPartitionValue("pk3")
                                                                                                                                  .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("sk2_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi4_sortBeginsWith_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(100)
                                                                                                                                  .addPartitionValue("pk3")
                                                                                                                                  .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                                  .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortBetween_fourSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue(1),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addSortValue(4)))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(0)));
    }

    @Test
    void queryGsi4_sortBetween_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z")),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_sortBetween_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortBetween_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortGreaterThan_fourSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(k -> k.addPartitionValue("pk1")
                                                                                                                                   .addPartitionValue(100)
                                                                                                                                   .addPartitionValue("pk3")
                                                                                                                                   .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                                   .addSortValue("sk1")
                                                                                                                                   .addSortValue("sk2")
                                                                                                                                   .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                                                   .addSortValue(3)))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_sortGreaterThan_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortGreaterThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_a")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi4_sortGreaterThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk0")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortGreaterThanOrEqual_fourSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue(1)))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_sortGreaterThanOrEqual_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(4));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortGreaterThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortGreaterThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortLessThan_fourSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue(3)))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(0)));
    }

    @Test
    void queryGsi4_sortLessThan_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-02T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_sortLessThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_b")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(4));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortLessThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortLessThanOrEqual_fourSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue(1)))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(0)));
    }

    @Test
    void queryGsi4_sortLessThanOrEqual_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi4_sortLessThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("sk2_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi4_sortLessThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi4")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addPartitionValue(Instant.parse("2025-01-01T00:00:00Z"))
                                                                                                     .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(5)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    // GSI5: 3 partition keys, 3 sort keys from the main table and flattened
    @Test
    void queryGsi5_keyEqualTo_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(50.0)
                                                                                                                              .addPartitionValue("flpk3")
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("flsk2")
                                                                                                                              .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
    }

    @Test
    void queryGsi5_keyEqualTo_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(60.0)
                                                                                                                              .addPartitionValue("flpk3")
                                                                                                                              .addSortValue("sk1")
                                                                                                                              .addSortValue("flsk2_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi5_keyEqualTo_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(k -> k.addPartitionValue("pk1")
                                                                                                                              .addPartitionValue(80.5)
                                                                                                                              .addPartitionValue("flpk3")
                                                                                                                              .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(0)));
    }

    @Test
    void queryGsi5_sortBeginsWith_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(50.0)
                                                                                                                                  .addPartitionValue("flpk3")
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("flsk2")
                                                                                                                                  .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(4)));
    }

    @Test
    void queryGsi5_sortBeginsWith_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(60.0)
                                                                                                                                  .addPartitionValue("flpk3")
                                                                                                                                  .addSortValue("sk1")
                                                                                                                                  .addSortValue("flsk2_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi5_sortBeginsWith_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(k -> k.addPartitionValue("pk1")
                                                                                                                                  .addPartitionValue(75.3)
                                                                                                                                  .addPartitionValue("flpk3")
                                                                                                                                  .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(3)));
    }

    @Test
    void queryGsi5_sortBetween_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(50.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z")),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(50.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-05T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(2)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
    }

    @Test
    void queryGsi5_sortBetween_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(85.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(85.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(11)));
    }

    @Test
    void queryGsi5_sortBetween_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(90.4)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1"),
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(90.4)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(7)));
    }

    @Test
    void queryGsi5_sortGreaterThan_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(50.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-02T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(2)));
    }

    @Test
    void queryGsi5_sortGreaterThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(60.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2_a")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi5_sortGreaterThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(70.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk0")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(6)));
    }

    @Test
    void queryGsi5_sortGreaterThanOrEqual_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(50.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(2)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
    }

    @Test
    void queryGsi5_sortGreaterThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(60.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue(
                                                                                                         "flsk2_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi5_sortGreaterThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(75.3)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(3)));
    }

    @Test
    void queryGsi5_sortLessThan_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(50.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-05T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(4)));
    }

    @Test
    void queryGsi5_sortLessThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(55.5)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2_z")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(9)));
    }

    @Test
    void queryGsi5_sortLessThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(65.5)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(10)));
    }

    @Test
    void queryGsi5_sortLessThanOrEqual_threeSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(50.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(4)));
    }

    @Test
    void queryGsi5_sortLessThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(60.0)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")
                                                                                                     .addSortValue(
                                                                                                         "flsk2_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi5_sortLessThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi5")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue("pk1")
                                                                                                     .addPartitionValue(90.4)
                                                                                                     .addPartitionValue("flpk3")
                                                                                                     .addSortValue("sk1")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(7)));
    }

    // GSI6: 2 partition keys, 2 sort keys from the main table and flattened, using multiple annotations and different order on
    // same attributes
    @Test
    void queryGsi6_keyEqualTo_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi6_keyEqualTo_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(keyEqualTo(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi6_sortBeginsWith_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-03T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(11)));
    }

    @Test
    void queryGsi6_sortBeginsWith_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBeginsWith(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2_p")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi6_sortBetween_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z")),
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-03-01T11:21:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi6_sortBetween_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortBetween(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2_p"),
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2_x")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));

    }

    @Test
    void queryGsi6_sortGreaterThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-05-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
    }

    @Test
    void queryGsi6_sortGreaterThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThan(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2_a")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi6_sortGreaterThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-06-01T10:22:02Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(3));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
    }

    @Test
    void queryGsi6_sortGreaterThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortGreaterThanOrEqualTo(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2_prefix")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(1));
        assertThat(page1.items().get(0), equalTo(COMPOSITE_RECORDS.get(5)));
    }

    @Test
    void queryGsi6_sortLessThan_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-03-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(5));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi6_sortLessThan_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThan(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2_")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(9));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }

    @Test
    void queryGsi6_sortLessThanOrEqual_twoSortKeys() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")
                                                                                                     .addSortValue(Instant.parse("2025-01-01T00:00:00Z"))))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(2));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
    }

    @Test
    void queryGsi6_sortLessThanOrEqual_oneSortKey() {
        Iterator<Page<CompositeKeyRecord>> results = mappedTable.index("gsi6")
                                                                .query(QueryEnhancedRequest.builder()
                                                                                           .queryConditional(sortLessThanOrEqualTo(
                                                                                               k -> k.addPartitionValue(100)
                                                                                                     .addPartitionValue("pk3")
                                                                                                     .addSortValue("flsk2")))
                                                                                           .build())
                                                                .iterator();

        Page<CompositeKeyRecord> page1 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items(), hasSize(9));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(0)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(1)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(3)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(4)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(6)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(7)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(9)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(10)), is(true));
        assertThat(page1.items().contains(COMPOSITE_RECORDS.get(11)), is(true));
    }
}