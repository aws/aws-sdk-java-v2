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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordForUpdateExpressions;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithUpdateBehaviors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class OptimisticLockingHelperTest {

    @Test
    public void withOptimisticLocking_onDelete_addsConditionExpression() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest originalRequest =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .withOptimisticLocking(versionValue, versionAttributeName)
                                     .build();

        DeleteItemEnhancedRequest result =
            OptimisticLockingHelper.withOptimisticLocking(originalRequest, versionValue, versionAttributeName);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void withOptimisticLocking_onTransactDelete_addsConditionExpression() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest originalRequest =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .withOptimisticLocking(versionValue, versionAttributeName)
                                             .build();

        TransactDeleteItemEnhancedRequest result =
            OptimisticLockingHelper.withOptimisticLocking(originalRequest, versionValue, versionAttributeName);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void conditionallyApplyOptimistic_onDelete_whenFlagFalse_returnsOriginalRequest() {
        boolean optimisticLockingEnabled = false;
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        // versioned record
        RecordWithUpdateBehaviors keyItem = new RecordWithUpdateBehaviors();
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);

        DeleteItemEnhancedRequest originalRequest =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .withOptimisticLocking(versionValue, versionAttributeName)
                                     .build();

        DeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertEquals(originalRequest, result);
    }

    @Test
    public void conditionallyApplyOptimistic_onDelete_whenFlagTrueAndVersionedRecord_returnsOriginalRequest() {
        boolean optimisticLockingEnabled = true;
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        // non-versioned record
        RecordForUpdateExpressions keyItem = new RecordForUpdateExpressions();
        TableSchema<RecordForUpdateExpressions> tableSchema = TableSchema.fromClass(RecordForUpdateExpressions.class);

        DeleteItemEnhancedRequest originalRequest =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .withOptimisticLocking(versionValue, versionAttributeName)
                                     .build();

        DeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void conditionallyApplyOptimistic_onDelete_whenFlagTrueAndVersionedRecordWithoutVersion_returnsOriginalRequest() {
        boolean optimisticLockingEnabled = true;
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = null;
        String versionAttributeName = "version";

        // versioned record
        RecordWithUpdateBehaviors keyItem = new RecordWithUpdateBehaviors();
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);

        DeleteItemEnhancedRequest originalRequest =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .withOptimisticLocking(versionValue, versionAttributeName)
                                     .build();

        DeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertEquals(originalRequest, result);
    }

    @Test
    public void conditionallyApplyOptimistic_onDelete_whenFlagTrueAndVersionedRecordWithVersion_addsConditionExpression() {
        boolean optimisticLockingEnabled = true;
        Key key = Key.builder().partitionValue("id").build();
        Long version = 1L;
        AttributeValue versionValue = AttributeValue.builder().n(String.valueOf(version)).build();
        String versionAttributeName = "version";

        // versioned record
        RecordWithUpdateBehaviors keyItem = new RecordWithUpdateBehaviors();
        keyItem.setVersion(version);
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);

        DeleteItemEnhancedRequest originalRequest =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .withOptimisticLocking(versionValue, versionAttributeName)
                                     .build();

        DeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void conditionallyApplyOptimistic_onTransactDelete_whenFlagFalse_returnsOriginalRequest() {
        boolean optimisticLockingEnabled = false;
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        // versioned record
        RecordWithUpdateBehaviors keyItem = new RecordWithUpdateBehaviors();
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);

        TransactDeleteItemEnhancedRequest originalRequest =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .withOptimisticLocking(versionValue, versionAttributeName)
                                             .build();

        TransactDeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertEquals(originalRequest, result);
    }

    @Test
    public void conditionallyApplyOptimistic_onTransactDelete_whenFlagTrueAndNonVersionedRecord_returnsOriginalRequest() {
        boolean optimisticLockingEnabled = true;
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        // non-versioned record
        RecordForUpdateExpressions keyItem = new RecordForUpdateExpressions();
        TableSchema<RecordForUpdateExpressions> tableSchema = TableSchema.fromClass(RecordForUpdateExpressions.class);

        TransactDeleteItemEnhancedRequest originalRequest =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .withOptimisticLocking(versionValue, versionAttributeName)
                                             .build();

        TransactDeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void conditionallyApplyOptimistic_onTransactDelete_whenFlagTrueAndVersionedRecord_addsConditionExpression() {
        boolean optimisticLockingEnabled = true;
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        // versioned record
        RecordWithUpdateBehaviors keyItem = new RecordWithUpdateBehaviors();
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);

        TransactDeleteItemEnhancedRequest originalRequest =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .withOptimisticLocking(versionValue, versionAttributeName)
                                             .build();

        TransactDeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertEquals(originalRequest, result);
    }

    @Test
    public void conditionallyApplyOptimistic_onTransactDelete_whenFlagTrueAndVersionedRecordWithVersion_addsConditionExpression() {
        boolean optimisticLockingEnabled = true;
        Key key = Key.builder().partitionValue("id").build();
        Long version = 1L;
        AttributeValue versionValue = AttributeValue.builder().n(String.valueOf(version)).build();
        String versionAttributeName = "version";

        // versioned record
        RecordWithUpdateBehaviors keyItem = new RecordWithUpdateBehaviors();
        keyItem.setVersion(version);
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);

        TransactDeleteItemEnhancedRequest originalRequest =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .withOptimisticLocking(versionValue, versionAttributeName)
                                             .build();

        TransactDeleteItemEnhancedRequest result = OptimisticLockingHelper.conditionallyApplyOptimisticLocking(
            originalRequest, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void createVersionCondition_shouldCreateCorrectExpression() {
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        Expression result = OptimisticLockingHelper.createVersionCondition(versionValue, versionAttributeName);

        assertThat(result.expression()).isEqualTo("version = :version_value");
        assertThat(result.expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void getVersionAttributeName_forVersionedRecord_returnsTheCorrectVersionValueFromTheTableSchema() {
        // versioned record
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);
        Optional<String> versionAttributeNameOpt = OptimisticLockingHelper.getVersionAttributeName(tableSchema);

        assertNotNull(versionAttributeNameOpt);
        assertTrue(versionAttributeNameOpt.isPresent());
        assertThat(versionAttributeNameOpt.get()).isEqualTo("version");
    }

    @Test
    public void getVersionAttributeName_forNonVersionedRecord_shouldNotReturnAVersionValue() {
        // non-versioned record
        TableSchema<RecordForUpdateExpressions> tableSchema = TableSchema.fromClass(RecordForUpdateExpressions.class);
        Optional<String> versionAttributeNameOpt = OptimisticLockingHelper.getVersionAttributeName(tableSchema);

        assertNotNull(versionAttributeNameOpt);
        assertFalse(versionAttributeNameOpt.isPresent());
    }

    @Test
    public void buildDeleteItemEnhancedRequest_addsCorrectExpressionOnRequest() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest result =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .withOptimisticLocking(versionValue, versionAttributeName)
                                     .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void buildDeleteItemEnhancedRequest_differentVersionAttributeNames_addsCorrectExpressionOnRequest() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();

        // Test with different attribute names
        String[] attributeNames = {"version", "recordVersion", "itemVersion", "v"};

        for (String attributeName : attributeNames) {
            DeleteItemEnhancedRequest result =
                DeleteItemEnhancedRequest.builder()
                                         .key(key)
                                         .withOptimisticLocking(versionValue, attributeName)
                                         .build();

            assertThat(result.conditionExpression().expression()).isEqualTo(attributeName + " = :version_value");
            assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
        }
    }

    @Test
    public void buildDeleteItemEnhancedRequest_differentVersionValues_addsCorrectExpressionOnRequest() {
        Key key = Key.builder().partitionValue("test-id").build();

        // Test with different version values
        AttributeValue[] versionValues = {
            AttributeValue.builder().n("0").build(),
            AttributeValue.builder().n("1").build(),
            AttributeValue.builder().n("999").build(),
            AttributeValue.builder().n("123456789").build()
        };

        for (AttributeValue versionValue : versionValues) {
            DeleteItemEnhancedRequest result =
                DeleteItemEnhancedRequest.builder()
                                         .key(key)
                                         .withOptimisticLocking(versionValue, "version")
                                         .build();

            assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
        }
    }

    @Test
    public void buildDeleteItemEnhancedRequest_preservesExistingRequestProperties() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();

        DeleteItemEnhancedRequest result =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .returnConsumedCapacity("TOTAL")
                                     .withOptimisticLocking(versionValue, "version")
                                     .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.returnConsumedCapacityAsString()).isEqualTo("TOTAL");
        assertThat(result.conditionExpression()).isNotNull();
    }

    @Test
    public void buildTransactDeleteItemEnhancedRequest_addsCorrectExpressionOnRequest() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "recordVersion";

        TransactDeleteItemEnhancedRequest result =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .withOptimisticLocking(versionValue, versionAttributeName)
                                             .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("recordVersion = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }
}