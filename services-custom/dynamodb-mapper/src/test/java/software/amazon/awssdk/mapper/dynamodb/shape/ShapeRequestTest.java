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
package software.amazon.awssdk.mapper.dynamodb.shape;

import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.HASH_KEY;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.n;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.s;
import static software.amazon.awssdk.mapper.dynamodb.shape.ShapeSupport.verify;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.Request;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.Select;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBDeleteExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBQueryExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBSaveExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBScanExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTransactionWriteExpression;
import software.amazon.awssdk.mapper.dynamodb.TransactionLoadRequest;
import software.amazon.awssdk.mapper.dynamodb.TransactionWriteRequest;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.Address;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.AllTypesItem;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.Color;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.DatedItem;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.FlattenedItem;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.InheritedItem;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.Name;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.RangeItem;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.StringItem;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.VersionedItem;
import software.amazon.awssdk.utils.IoUtils;

/** Captures each mapper call's marshalled request (X-Amz-Target + JSON body) and asserts it against a committed fixture. */
@RunWith(Parameterized.class)
public class ShapeRequestTest {

    private static final String TARGET_HEADER = "X-Amz-Target";

    /** The DynamoDB operation a case targets, and the fixture file it asserts against. */
    enum Operation {
        PUT_ITEM("put_item_fixture.json"),
        UPDATE_ITEM("update_item_fixture.json"),
        DELETE_ITEM("delete_item_fixture.json"),
        GET_ITEM("get_item_fixture.json"),
        QUERY("query_fixture.json"),
        SCAN("scan_fixture.json"),
        BATCH_WRITE_ITEM("batch_write_item_fixture.json"),
        BATCH_GET_ITEM("batch_get_item_fixture.json"),
        TRANSACT_WRITE_ITEMS("transaction_write_fixture.json"),
        TRANSACT_GET_ITEMS("transaction_load_fixture.json");

        final String fixture;

        Operation(String fixture) {
            this.fixture = fixture;
        }
    }

    interface MapperAction {
        void run(DynamoDBMapper mapper);
    }

    static final class Case {
        final Operation operation;
        final String name;
        final MapperAction action;

        Case(Operation operation, String name, MapperAction action) {
            this.operation = operation;
            this.name = name;
            this.action = action;
        }

        @Override
        public String toString() {
            return operation.fixture.replace("_fixture.json", "") + ":" + name;
        }
    }

    @Parameters(name = "{0}")
    public static List<Case> cases() {
        List<Case> c = new ArrayList<>();

        // PutItem value matrix: one field per case, saved with PUT so each fixture isolates a single encoding.
        add(c, "string/normal", m -> m.save(stringItem("hello"), put()));
        add(c, "string/unicode", m -> m.save(stringItem("héllo-世界-😀"), put()));
        add(c, "string/whitespace", m -> m.save(stringItem("  pad \t\n"), put()));
        add(c, "number/positive", m -> m.save(at().withNumber(42L), put()));
        add(c, "number/zero", m -> m.save(at().withNumber(0L), put()));
        add(c, "number/negative", m -> m.save(at().withNumber(-7L), put()));
        add(c, "number/max-long", m -> m.save(at().withNumber(Long.MAX_VALUE), put()));
        add(c, "number/min-long", m -> m.save(at().withNumber(Long.MIN_VALUE), put()));
        add(c, "double/fractional", m -> m.save(at().withDouble(1.5d), put()));
        add(c, "double/whole", m -> m.save(at().withDouble(1.0d), put()));
        add(c, "double/negative", m -> m.save(at().withDouble(-0.25d), put()));
        add(c, "bool-numeric/true", m -> m.save(at().withNumericBool(true), put()));
        add(c, "bool-numeric/false", m -> m.save(at().withNumericBool(false), put()));
        add(c, "bool-native/true", m -> m.save(at().withNativeBool(true), put()));
        add(c, "bool-native/false", m -> m.save(at().withNativeBool(false), put()));
        add(c, "string-set/single", m -> m.save(at().withStringSet(set("a")), put()));
        add(c, "string-set/multi", m -> m.save(at().withStringSet(set("c", "a", "b")), put()));
        add(c, "number-set/single", m -> m.save(at().withNumberSet(set(1L)), put()));
        add(c, "number-set/multi", m -> m.save(at().withNumberSet(set(3L, 1L, 2L)), put()));
        add(c, "list/single", m -> m.save(at().withList(list("only")), put()));
        add(c, "list/multi", m -> m.save(at().withList(list("first", "second", "third")), put()));
        add(c, "map/single", m -> m.save(at().withMap(map("k", "v")), put()));
        add(c, "map/multi", m -> m.save(at().withMap(map("b", "2", "a", "1")), put()));
        // Single-attribute document: a multi-field @DynamoDBDocument marshals in reflection order, which varies across
        // JVMs and can't be byte-pinned. One field proves the M encoding without ordering ambiguity.
        add(c, "document/populated", m -> m.save(at().withDocument(city("Seattle")), put()));
        add(c, "enum/value", m -> m.save(at().withEnumValue(Color.GREEN), put()));

        // Single-field flattened doc: the nested Name.first is hoisted to a sibling "firstName" attribute.
        c.add(new Case(Operation.PUT_ITEM, "flattened", m -> m.save(flattenedItem("Ada"), put())));
        // Hash key declared on a base class still marshals into the key envelope.
        c.add(new Case(Operation.PUT_ITEM, "inherited-key", m -> m.save(inheritedItem("tag"), put())));
        // Default Date with no epoch/pattern annotation marshals as an ISO-8601 S. Fixed instant for determinism.
        c.add(new Case(Operation.PUT_ITEM, "date-iso8601",
            m -> m.save(datedItem(new Date(1_600_000_000_000L)), put())));

        c.add(new Case(Operation.UPDATE_ITEM, "save-default-update", m -> m.save(stringItem("hello"))));
        c.add(new Case(Operation.UPDATE_ITEM, "versioned-first-save", m -> m.save(versionedItem())));

        c.add(new Case(Operation.DELETE_ITEM, "simple", m -> m.delete(stringItem("hello"))));
        c.add(new Case(Operation.DELETE_ITEM, "versioned", m -> m.delete(versionedItemWithVersion())));

        c.add(new Case(Operation.GET_ITEM, "by-key", m -> m.load(StringItem.class, HASH_KEY)));

        c.add(new Case(Operation.QUERY, "hash-key-equals", m -> {
            StringItem key = new StringItem();
            key.setId(HASH_KEY);
            m.query(StringItem.class, new DynamoDBQueryExpression<StringItem>().withHashKeyValues(key));
        }));

        c.add(new Case(Operation.SCAN, "unfiltered", m -> m.scan(StringItem.class, new DynamoDBScanExpression())));

        c.add(new Case(Operation.BATCH_WRITE_ITEM, "two-puts",
            m -> m.batchWrite(Arrays.asList(stringItem("a"), stringItem("b")), Collections.emptyList())));

        c.add(new Case(Operation.BATCH_GET_ITEM, "two-keys",
            m -> m.batchLoad(Arrays.asList(keyOnly("a"), keyOnly("b")))));

        // CLOBBER vs PUT diverge only on a versioned item: PUT keeps the Expected version guard, CLOBBER suppresses it.
        c.add(new Case(Operation.PUT_ITEM, "clobber-suppresses-version-check",
            m -> m.save(versionedItem(), new DynamoDBMapperConfig(SaveBehavior.CLOBBER))));
        c.add(new Case(Operation.PUT_ITEM, "put-keeps-version-check",
            m -> m.save(versionedItem(), new DynamoDBMapperConfig(SaveBehavior.PUT))));
        c.add(new Case(Operation.UPDATE_ITEM, "append-set-behavior",
            m -> m.save(at().withStringSet(set("a", "b")), new DynamoDBMapperConfig(SaveBehavior.APPEND_SET))));

        // UPDATE emits Action:DELETE for a null attribute; SKIP_NULL omits it.
        c.add(new Case(Operation.UPDATE_ITEM, "null-attr-delete",
            m -> m.save(stringItem(null), new DynamoDBMapperConfig(SaveBehavior.UPDATE))));
        c.add(new Case(Operation.UPDATE_ITEM, "null-attr-skip",
            m -> m.save(stringItem(null), new DynamoDBMapperConfig(SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES))));

        c.add(new Case(Operation.GET_ITEM, "composite-key", m -> m.load(RangeItem.class, HASH_KEY, "r")));

        c.add(new Case(Operation.UPDATE_ITEM, "save-with-expected-expression",
            m -> m.save(stringItem("hello"), new DynamoDBSaveExpression()
                .withExpectedEntry("value", new ExpectedAttributeValue().withExists(false)))));

        c.add(new Case(Operation.DELETE_ITEM, "delete-with-expected-expression",
            m -> m.delete(stringItem("hello"), new DynamoDBDeleteExpression()
                .withExpectedEntry("value", new ExpectedAttributeValue().withValue(s("hello"))))));
        c.add(new Case(Operation.DELETE_ITEM, "delete-with-condition-expression",
            m -> m.delete(stringItem("hello"), new DynamoDBDeleteExpression()
                .withConditionExpression("attribute_exists(#v)")
                .withExpressionAttributeNames(Collections.singletonMap("#v", "value")))));

        c.add(new Case(Operation.QUERY, "range-condition-limit-desc", m -> {
            RangeItem key = new RangeItem();
            key.setId(HASH_KEY);
            Condition rangeCond = new Condition()
                .withComparisonOperator(ComparisonOperator.GT)
                .withAttributeValueList(n("5"));
            m.query(RangeItem.class, new DynamoDBQueryExpression<RangeItem>()
                .withHashKeyValues(key)
                .withRangeKeyCondition("range", rangeCond)
                .withLimit(10)
                .withScanIndexForward(false));
        }));
        c.add(new Case(Operation.QUERY, "consistent-read-select", m -> {
            StringItem key = new StringItem();
            key.setId(HASH_KEY);
            m.query(StringItem.class, new DynamoDBQueryExpression<StringItem>()
                .withHashKeyValues(key)
                .withConsistentRead(true)
                .withSelect(Select.ALL_ATTRIBUTES));
        }));
        c.add(new Case(Operation.QUERY, "query-filter", m -> {
            StringItem key = new StringItem();
            key.setId(HASH_KEY);
            m.query(StringItem.class, new DynamoDBQueryExpression<StringItem>()
                .withHashKeyValues(key)
                .withQueryFilterEntry("value", new Condition()
                    .withComparisonOperator(ComparisonOperator.EQ)
                    .withAttributeValueList(s("hello"))));
        }));
        c.add(new Case(Operation.QUERY, "exclusive-start-key", m -> {
            StringItem key = new StringItem();
            key.setId(HASH_KEY);
            Map<String, AttributeValue> start = new LinkedHashMap<>();
            start.put("id", s(HASH_KEY));
            m.query(StringItem.class, new DynamoDBQueryExpression<StringItem>()
                .withHashKeyValues(key)
                .withExclusiveStartKey(start));
        }));
        c.add(new Case(Operation.QUERY, "projection-expression", m -> {
            StringItem key = new StringItem();
            key.setId(HASH_KEY);
            m.query(StringItem.class, new DynamoDBQueryExpression<StringItem>()
                .withHashKeyValues(key)
                .withProjectionExpression("#v")
                .withExpressionAttributeNames(Collections.singletonMap("#v", "value")));
        }));
        // keyConditionExpression is mutually exclusive with hashKeyValues; the mapper throws if both are set.
        c.add(new Case(Operation.QUERY, "key-condition-expression",
            m -> m.query(StringItem.class, new DynamoDBQueryExpression<StringItem>()
                .withKeyConditionExpression("#i = :v")
                .withExpressionAttributeNames(Collections.singletonMap("#i", "id"))
                .withExpressionAttributeValues(Collections.singletonMap(":v", s(HASH_KEY))))));

        c.add(new Case(Operation.SCAN, "filtered-limited", m -> m.scan(StringItem.class,
            new DynamoDBScanExpression()
                .withFilterConditionEntry("value", new Condition()
                    .withComparisonOperator(ComparisonOperator.EQ)
                    .withAttributeValueList(s("hello")))
                .withLimit(25))));
        c.add(new Case(Operation.SCAN, "consistent-read", m -> m.scan(StringItem.class,
            new DynamoDBScanExpression().withConsistentRead(true))));
        c.add(new Case(Operation.SCAN, "projection-expression", m -> m.scan(StringItem.class,
            new DynamoDBScanExpression()
                .withProjectionExpression("#v")
                .withExpressionAttributeNames(Collections.singletonMap("#v", "value")))));
        c.add(new Case(Operation.SCAN, "filter-expression", m -> m.scan(StringItem.class,
            new DynamoDBScanExpression()
                .withFilterExpression("#v = :v")
                .withExpressionAttributeNames(Collections.singletonMap("#v", "value"))
                .withExpressionAttributeValues(Collections.singletonMap(":v", s("hello"))))));
        c.add(new Case(Operation.SCAN, "segment", m -> m.scan(StringItem.class,
            new DynamoDBScanExpression().withTotalSegments(4).withSegment(1))));

        c.add(new Case(Operation.BATCH_WRITE_ITEM, "mixed-put-delete",
            m -> m.batchWrite(Arrays.asList(stringItem("p")), Arrays.asList(keyOnly("d")))));

        // Fixed idempotency token: an unset one makes the core SDK auto-fill ClientRequestToken with a random UUID.
        c.add(new Case(Operation.TRANSACT_WRITE_ITEMS, "put-update-delete", m -> {
            TransactionWriteRequest req = new TransactionWriteRequest()
                .addPut(stringItem("p"))
                .addUpdate(stringItem("u"))
                .addDelete(keyOnly("d"))
                .withIdempotencyToken("fixed-token");
            m.transactionWrite(req);
        }));
        c.add(new Case(Operation.TRANSACT_WRITE_ITEMS, "condition-check", m -> {
            TransactionWriteRequest req = new TransactionWriteRequest()
                .addConditionCheck(keyOnly("c"), new DynamoDBTransactionWriteExpression()
                    .withConditionExpression("attribute_exists(#i)")
                    .withExpressionAttributeNames(Collections.singletonMap("#i", "id")))
                .withIdempotencyToken("fixed-token");
            m.transactionWrite(req);
        }));
        // Versioned put with no user expression: the mapper auto-generates the version condition.
        c.add(new Case(Operation.TRANSACT_WRITE_ITEMS, "versioned-auto-condition", m -> {
            TransactionWriteRequest req = new TransactionWriteRequest()
                .addPut(versionedItem())
                .withIdempotencyToken("fixed-token");
            m.transactionWrite(req);
        }));

        c.add(new Case(Operation.TRANSACT_GET_ITEMS, "two-loads", m -> {
            TransactionLoadRequest req = new TransactionLoadRequest()
                .addLoad(keyOnly("a"))
                .addLoad(keyOnly("b"));
            m.transactionLoad(req);
        }));

        return c;
    }

    @Parameter
    public Case testCase;

    @Test
    public void matchesFixture() {
        verify(testCase.operation.fixture, testCase.name, captureRequest(testCase.action));
    }

    // Captures the marshalled request as "target\nbody"; the v2 port swaps this for an ExecutionInterceptor.
    private static String captureRequest(MapperAction action) {
        String[] captured = new String[1];
        RequestHandler2 handler = new RequestHandler2() {
            @Override
            public void beforeRequest(Request<?> request) {
                String target = request.getHeaders().get(TARGET_HEADER);
                captured[0] = target + "\n" + readContent(request.getContent());
                throw new StopSignal();
            }
        };
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("akid", "skid")))
            .withEndpointConfiguration(new EndpointConfiguration("http://localhost:8000", "us-east-1"))
            .withRequestHandlers(handler)
            .build();
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        try {
            action.run(mapper);
        } catch (StopSignal expected) {
            // request captured; network intentionally aborted
        } catch (AmazonServiceException e) {
            throw new IllegalStateException("Request reached the network instead of being captured", e);
        }
        if (captured[0] == null) {
            throw new IllegalStateException("No DynamoDB request was marshalled by the action");
        }
        return captured[0];
    }

    private static final class StopSignal extends RuntimeException {
        StopSignal() {
            super(null, null, false, false);
        }
    }

    private static String readContent(InputStream content) {
        if (content == null) {
            throw new IllegalStateException("Marshalled request had no body");
        }
        try {
            return new String(IoUtils.toByteArray(content), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void add(List<Case> c, String name, MapperAction action) {
        c.add(new Case(Operation.PUT_ITEM, name, action));
    }

    private static DynamoDBMapperConfig put() {
        return new DynamoDBMapperConfig(SaveBehavior.PUT);
    }

    private static AllTypesItem at() {
        return new AllTypesItem().withId(HASH_KEY);
    }

    private static StringItem stringItem(String v) {
        StringItem i = new StringItem();
        i.setId(HASH_KEY);
        i.setValue(v);
        return i;
    }

    private static StringItem keyOnly(String id) {
        StringItem i = new StringItem();
        i.setId(id);
        return i;
    }

    private static VersionedItem versionedItem() {
        VersionedItem i = new VersionedItem();
        i.setId(HASH_KEY);
        return i;
    }

    private static VersionedItem versionedItemWithVersion() {
        VersionedItem i = versionedItem();
        i.setVersion(3L);
        return i;
    }

    private static Address city(String city) {
        Address a = new Address();
        a.setCity(city);
        return a;
    }

    private static FlattenedItem flattenedItem(String first) {
        Name name = new Name();
        name.setFirst(first);
        FlattenedItem i = new FlattenedItem();
        i.setId(HASH_KEY);
        i.setName(name);
        return i;
    }

    private static InheritedItem inheritedItem(String label) {
        InheritedItem i = new InheritedItem();
        i.setId(HASH_KEY);
        i.setLabel(label);
        return i;
    }

    private static DatedItem datedItem(Date createdAt) {
        DatedItem i = new DatedItem();
        i.setId(HASH_KEY);
        i.setCreatedAt(createdAt);
        return i;
    }

    @SafeVarargs
    private static <T> java.util.Set<T> set(T... v) {
        return new LinkedHashSet<>(Arrays.asList(v));
    }

    private static List<String> list(String... v) {
        return new ArrayList<>(Arrays.asList(v));
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int j = 0; j < kv.length; j += 2) {
            map.put(kv[j], kv[j + 1]);
        }
        return map;
    }
}
