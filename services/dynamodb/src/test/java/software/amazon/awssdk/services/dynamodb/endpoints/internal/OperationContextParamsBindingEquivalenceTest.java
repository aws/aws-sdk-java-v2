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

package software.amazon.awssdk.services.dynamodb.endpoints.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.jmespath.internal.JmesPathRuntime.Value;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.ImportTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TableCreationParameters;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Verifies that the codegen-lowered {@code operationContextParams} bindings for DynamoDB produce the same
 * values as the reflective {@code JmesPathRuntime} evaluation, which remains the fallback for unsupported
 * expressions and is used here as the equivalence oracle. Covers all four supported constructs:
 * {@code keys()}, a scalar field chain, a projection, and a multiselect-list + flatten (TransactWriteItems).
 *
 * <p>Each lowered binding is invoked directly via its generated, private
 * {@code setOperationContextParams(builder, request)} overload, so the check is isolated to the binding itself.
 */
public class OperationContextParamsBindingEquivalenceTest {

    private static List<String> loweredList(Object request) {
        return invokeBinding(request).resourceArnList();
    }

    private static String loweredScalar(Object request) {
        return invokeBinding(request).resourceArn();
    }

    private static DynamoDbEndpointParams invokeBinding(Object request) {
        try {
            Method binding = DynamoDbEndpointResolverUtils.class.getDeclaredMethod(
                "setOperationContextParams", DynamoDbEndpointParams.Builder.class, request.getClass());
            binding.setAccessible(true);
            DynamoDbEndpointParams.Builder builder = DynamoDbEndpointParams.builder();
            binding.invoke(null, builder, request);
            return builder.build();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertKeysEquivalent(List<String> reflectiveResult, List<String> loweredResult,
                                             List<String> expectedContent) {
        assertEquals(reflectiveResult, loweredResult, "lowered binding must equal reflective evaluation");
        // Key order is asserted positionally against the reflective oracle above; the expected content is
        // written in insertion order, so compare it as a set.
        assertEquals(new HashSet<>(expectedContent), new HashSet<>(loweredResult), "content must match");
    }

    @Test
    public void batchGetItemKeys() {
        BatchGetItemRequest empty = BatchGetItemRequest.builder().build();
        assertKeysEquivalent(new Value(empty).field("RequestItems").keys().stringValues(),
                             loweredList(empty), Collections.emptyList());

        // Keys whose insertion order differs from their HashMap iteration order: the endpoint ruleset reads
        // ResourceArnList positionally (getAttr(..., "[0]")) and the reflective runtime wraps maps as
        // new HashMap<>(map), so the lowered binding must reproduce that hash ordering.
        String[] keys = {"zebra", "mango", "apple", "delta", "foxtrot", "bravo", "yankee", "tango", "kilo", "echo"};
        Map<String, KeysAndAttributes> items = new LinkedHashMap<>();
        for (String key : keys) {
            items.put(key, KeysAndAttributes.builder().build());
        }
        BatchGetItemRequest req = BatchGetItemRequest.builder().requestItems(items).build();
        assertKeysEquivalent(new Value(req).field("RequestItems").keys().stringValues(),
                             loweredList(req), Arrays.asList(keys));
    }

    @Test
    public void batchGetItemNullKeyDropped() {
        Map<String, KeysAndAttributes> items = new LinkedHashMap<>();
        items.put(null, KeysAndAttributes.builder().build());
        items.put("table", KeysAndAttributes.builder().build());
        BatchGetItemRequest req = BatchGetItemRequest.builder().requestItems(items).build();
        assertKeysEquivalent(new Value(req).field("RequestItems").keys().stringValues(),
                             loweredList(req), Collections.singletonList("table"));
    }

    @Test
    public void batchWriteItemKeys() {
        Map<String, List<WriteRequest>> items = new LinkedHashMap<>();
        items.put("t1", Collections.emptyList());
        items.put("t2", Collections.singletonList(WriteRequest.builder().build()));
        BatchWriteItemRequest req = BatchWriteItemRequest.builder().requestItems(items).build();
        assertKeysEquivalent(new Value(req).field("RequestItems").keys().stringValues(),
                             loweredList(req), Arrays.asList("t1", "t2"));
    }

    private static void assertImportTableEquivalent(ImportTableRequest req, String expected) {
        String ref = new Value(req).field("TableCreationParameters").field("TableName").stringValue();
        String low = loweredScalar(req);
        assertEquals(ref, low, "lowered binding must equal reflective evaluation");
        assertEquals(expected, low, "lowered binding must equal the hand-computed expectation");
    }

    @Test
    public void importTableContainerNull() {
        assertImportTableEquivalent(ImportTableRequest.builder().build(), null);
    }

    @Test
    public void importTableScalarPresent() {
        assertImportTableEquivalent(
            ImportTableRequest.builder()
                              .tableCreationParameters(TableCreationParameters.builder().tableName("my-table").build())
                              .build(),
            "my-table");
    }

    @Test
    public void importTableScalarLeafNull() {
        assertImportTableEquivalent(
            ImportTableRequest.builder()
                              .tableCreationParameters(TableCreationParameters.builder().build())
                              .build(),
            null);
    }

    private static void assertGetEquivalent(TransactGetItemsRequest req, List<String> expected) {
        List<String> ref = new Value(req).field("TransactItems").wildcard().field("Get").field("TableName").stringValues();
        List<String> low = loweredList(req);
        assertEquals(ref, low, "lowered binding must equal reflective evaluation");
        assertEquals(expected, low, "lowered binding must equal the hand-computed expectation");
    }

    @Test
    public void transactGetEmpty() {
        assertGetEquivalent(TransactGetItemsRequest.builder().build(), Collections.emptyList());
    }

    @Test
    public void transactGetMultiple() {
        TransactGetItem g1 = TransactGetItem.builder().get(Get.builder().tableName("ga").build()).build();
        TransactGetItem g2 = TransactGetItem.builder().get(Get.builder().tableName("gb").build()).build();
        assertGetEquivalent(TransactGetItemsRequest.builder().transactItems(g1, g2).build(),
                            Arrays.asList("ga", "gb"));
    }

    @Test
    public void transactGetNullInnerAndLeaf() {
        TransactGetItem hasName = TransactGetItem.builder().get(Get.builder().tableName("ga").build()).build();
        TransactGetItem getNull = TransactGetItem.builder().build();
        TransactGetItem leafNull = TransactGetItem.builder().get(Get.builder().build()).build();
        assertGetEquivalent(TransactGetItemsRequest.builder().transactItems(hasName, getNull, leafNull).build(),
                            Collections.singletonList("ga"));
    }

    private static List<String> reflectiveWrite(TransactWriteItemsRequest req) {
        Function<Value, Value> cc = v -> v.field("ConditionCheck").field("TableName");
        Function<Value, Value> put = v -> v.field("Put").field("TableName");
        Function<Value, Value> del = v -> v.field("Delete").field("TableName");
        Function<Value, Value> upd = v -> v.field("Update").field("TableName");
        return new Value(req).field("TransactItems").wildcard()
                             .multiSelectList(cc, put, del, upd).flatten().stringValues();
    }

    private static void assertWriteEquivalent(TransactWriteItemsRequest req, List<String> expected) {
        List<String> low = loweredList(req);
        assertEquals(reflectiveWrite(req), low, "lowered binding must equal reflective evaluation");
        assertEquals(expected, low, "lowered binding must equal the hand-computed expectation");
    }

    @Test
    public void transactWriteEmpty() {
        assertWriteEquivalent(TransactWriteItemsRequest.builder().build(), Collections.emptyList());
    }

    @Test
    public void transactWriteSingleBranch() {
        TransactWriteItem putOnly = TransactWriteItem.builder().put(Put.builder().tableName("p1").build()).build();
        assertWriteEquivalent(TransactWriteItemsRequest.builder().transactItems(putOnly).build(),
                             Collections.singletonList("p1"));
    }

    @Test
    public void transactWriteAllBranchesOrdered() {
        // Within one item the multiselect order is [ConditionCheck, Put, Delete, Update].
        TransactWriteItem all = TransactWriteItem.builder()
            .conditionCheck(ConditionCheck.builder().tableName("cc").build())
            .put(Put.builder().tableName("p").build())
            .delete(Delete.builder().tableName("d").build())
            .update(Update.builder().tableName("u").build())
            .build();
        assertWriteEquivalent(TransactWriteItemsRequest.builder().transactItems(all).build(),
                             Arrays.asList("cc", "p", "d", "u"));
    }

    @Test
    public void transactWriteMixedItemsAndNullLeaf() {
        TransactWriteItem item1 = TransactWriteItem.builder().put(Put.builder().tableName("p1").build()).build();
        TransactWriteItem item2 = TransactWriteItem.builder()
            .conditionCheck(ConditionCheck.builder().tableName("cc2").build())
            .delete(Delete.builder().tableName("d2").build())
            .build();
        // Put present but its TableName null -> dropped.
        TransactWriteItem item3 = TransactWriteItem.builder().put(Put.builder().build()).build();
        assertWriteEquivalent(
            TransactWriteItemsRequest.builder().transactItems(item1, item2, item3).build(),
            Arrays.asList("p1", "cc2", "d2"));
    }

    @Test
    public void transactWriteNullItemGuarded() {
        List<TransactWriteItem> items = new ArrayList<>();
        items.add(TransactWriteItem.builder().put(Put.builder().tableName("p1").build()).build());
        items.add(null);
        assertWriteEquivalent(TransactWriteItemsRequest.builder().transactItems(items).build(),
                             Collections.singletonList("p1"));
    }
}
