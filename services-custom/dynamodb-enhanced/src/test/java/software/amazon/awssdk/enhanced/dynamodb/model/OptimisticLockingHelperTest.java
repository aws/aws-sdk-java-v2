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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.OptimisticLockingHelper.conditionallyApplyOptimisticLocking;
import static software.amazon.awssdk.enhanced.dynamodb.internal.OptimisticLockingHelper.createVersionCondition;
import static software.amazon.awssdk.enhanced.dynamodb.internal.OptimisticLockingHelper.getVersionAttributeName;
import static software.amazon.awssdk.enhanced.dynamodb.internal.OptimisticLockingHelper.optimisticLocking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    public void optimisticLocking_onDelete_addsConditionExpression() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest.Builder originalRequestBuilder =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .optimisticLocking(versionValue, versionAttributeName);

        DeleteItemEnhancedRequest result = optimisticLocking(originalRequestBuilder, versionValue, versionAttributeName);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
    }

    @Test
    public void optimisticLocking_onTransactDelete_addsConditionExpression() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest.Builder originalRequestBuilder =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .optimisticLocking(versionValue, versionAttributeName);

        TransactDeleteItemEnhancedRequest result = optimisticLocking(originalRequestBuilder, versionValue, versionAttributeName);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
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

        DeleteItemEnhancedRequest.Builder requestBuilder =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .optimisticLocking(versionValue, versionAttributeName);

        DeleteItemEnhancedRequest result = conditionallyApplyOptimisticLocking(
            requestBuilder, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertEquals(requestBuilder.build(), result);
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

        DeleteItemEnhancedRequest.Builder originalRequestBuilder =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .optimisticLocking(versionValue, versionAttributeName);

        DeleteItemEnhancedRequest result = conditionallyApplyOptimisticLocking(
            originalRequestBuilder, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
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

        DeleteItemEnhancedRequest.Builder originalRequestBuilder =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .optimisticLocking(versionValue, versionAttributeName);

        DeleteItemEnhancedRequest result = conditionallyApplyOptimisticLocking(
            originalRequestBuilder, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
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

        TransactDeleteItemEnhancedRequest.Builder originalRequestBuilder =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .optimisticLocking(versionValue, versionAttributeName);

        TransactDeleteItemEnhancedRequest result = conditionallyApplyOptimisticLocking(
            originalRequestBuilder, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertEquals(originalRequestBuilder.build(), result);
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

        TransactDeleteItemEnhancedRequest.Builder originalRequestBuilder =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .optimisticLocking(versionValue, versionAttributeName);

        TransactDeleteItemEnhancedRequest result = conditionallyApplyOptimisticLocking(
            originalRequestBuilder, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
    }

    @Test
    public void conditionallyApplyOptimistic_onTransactDelete_whenFlagTrueAndVersionedRecordWithNullVersion_returnsOriginalRequest() {
        boolean optimisticLockingEnabled = true;
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        // versioned record
        RecordWithUpdateBehaviors keyItem = new RecordWithUpdateBehaviors();
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);

        TransactDeleteItemEnhancedRequest.Builder originalRequestBuilder =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .optimisticLocking(versionValue, versionAttributeName);

        TransactDeleteItemEnhancedRequest result = conditionallyApplyOptimisticLocking(
            originalRequestBuilder, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertEquals(originalRequestBuilder.build(), result);
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

        TransactDeleteItemEnhancedRequest.Builder originalRequestBuilder =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(key)
                                             .optimisticLocking(versionValue, versionAttributeName);

        TransactDeleteItemEnhancedRequest result = conditionallyApplyOptimisticLocking(
            originalRequestBuilder, keyItem, tableSchema, optimisticLockingEnabled);

        assertThat(result).isNotNull();
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
    }

    @Test
    public void createVersionCondition_shouldCreateCorrectExpression() {
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        Expression result = createVersionCondition(versionValue, versionAttributeName);

        assertThat(result.expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
    }

    @Test
    public void createVersionCondition_nullVersionAttributeName_throwsIllegalArgumentException() {
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = null;

        assertThatThrownBy(() -> createVersionCondition(versionValue, versionAttributeName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Version attribute name must not be null or empty.");
    }

    @Test
    public void createVersionCondition_emptyVersionAttributeName_throwsIllegalArgumentException() {
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "   ";

        assertThatThrownBy(() -> createVersionCondition(versionValue, versionAttributeName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Version attribute name must not be null or empty.");
    }

    @Test
    public void createVersionCondition_nullVersionValue_throwsIllegalArgumentException() {
        AttributeValue versionValue = null;
        String versionAttributeName = "version";

        assertThatThrownBy(() -> createVersionCondition(versionValue, versionAttributeName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Version value must not be null or empty.");
    }

    @Test
    public void createVersionCondition_nullVersionAttributeValue_throwsIllegalArgumentException() {
        AttributeValue versionValue = AttributeValue.fromN(null);
        String versionAttributeName = "version";

        assertThatThrownBy(() -> createVersionCondition(versionValue, versionAttributeName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Version value must not be null or empty.");
    }

    @Test
    public void createVersionCondition_emptyVersionAttributeValue_throwsIllegalArgumentException() {
        AttributeValue versionValue = AttributeValue.fromN("   ");
        String versionAttributeName = "version";

        assertThatThrownBy(() -> createVersionCondition(versionValue, versionAttributeName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Version value must not be null or empty.");
    }

    @Test
    public void getVersionAttributeName_forVersionedRecord_returnsTheCorrectVersionValueFromTheTableSchema() {
        // versioned record
        TableSchema<RecordWithUpdateBehaviors> tableSchema = TableSchema.fromClass(RecordWithUpdateBehaviors.class);
        Optional<String> versionAttributeNameOpt = getVersionAttributeName(tableSchema);

        assertNotNull(versionAttributeNameOpt);
        assertTrue(versionAttributeNameOpt.isPresent());
        assertThat(versionAttributeNameOpt.get()).isEqualTo("version");
    }

    @Test
    public void getVersionAttributeName_forNonVersionedRecord_shouldNotReturnAVersionValue() {
        // non-versioned record
        TableSchema<RecordForUpdateExpressions> tableSchema = TableSchema.fromClass(RecordForUpdateExpressions.class);
        Optional<String> versionAttributeNameOpt = getVersionAttributeName(tableSchema);

        assertNotNull(versionAttributeNameOpt);
        assertFalse(versionAttributeNameOpt.isPresent());
    }

    @Test
    public void buildDeleteItemEnhancedRequest_withOptimisticLocking_addsOptimisticLockingCondition() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest result =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .optimisticLocking(versionValue, versionAttributeName)
                                     .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_version", "version"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_version", versionValue));
    }

    @Test
    public void buildDeleteItemEnhancedRequest_withOptimisticLockingAndCustomCondition_mergesConditions() {
        Key key = Key.builder().partitionValue("id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#key1", "key1");
        expressionNames.put("#key2", "key2");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":value1", numberValue(10));
        expressionValues.put(":value2", numberValue(20));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#key1 = :value1 OR #key2 = :value2")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        DeleteItemEnhancedRequest result =
            DeleteItemEnhancedRequest.builder()
                                     .key(key)
                                     .conditionExpression(conditionExpression)
                                     .optimisticLocking(versionValue, versionAttributeName)
                                     .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.conditionExpression()).isNotNull();

        assertThat(result.conditionExpression().expression()).isEqualTo(
            "(#key1 = :value1 OR #key2 = :value2) AND (#AMZN_MAPPED_version = :AMZN_MAPPED_version)");

        Map<String, String> expectedExpressionNames = new HashMap<>();
        expectedExpressionNames.put("#AMZN_MAPPED_version", "version");
        expectedExpressionNames.put("#key1", "key1");
        expectedExpressionNames.put("#key2", "key2");
        assertThat(result.conditionExpression().expressionNames()).containsExactlyInAnyOrderEntriesOf(expectedExpressionNames);

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":AMZN_MAPPED_version", AttributeValue.builder().n("1").build());
        expectedExpressionValues.put(":value1", AttributeValue.builder().n("10").build());
        expectedExpressionValues.put(":value2", AttributeValue.builder().n("20").build());
        assertThat(result.conditionExpression().expressionValues()).containsExactlyInAnyOrderEntriesOf(expectedExpressionValues);
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
                                         .optimisticLocking(versionValue, attributeName)
                                         .build();

            assertThat(result.conditionExpression().expression()).isEqualTo(
                "#AMZN_MAPPED_" + attributeName + " = :AMZN_MAPPED_" + attributeName);
            assertThat(result.conditionExpression().expressionNames()).containsExactly(
                entry("#AMZN_MAPPED_" + attributeName, attributeName));
            assertThat(result.conditionExpression().expressionValues()).containsExactly(
                entry(":AMZN_MAPPED_" + attributeName, versionValue));
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
                                         .optimisticLocking(versionValue, "version")
                                         .build();

            assertThat(result.conditionExpression().expression()).isEqualTo(
                "#AMZN_MAPPED_version = :AMZN_MAPPED_version");
            assertThat(result.conditionExpression().expressionNames()).containsExactly(
                entry("#AMZN_MAPPED_version", "version"));
            assertThat(result.conditionExpression().expressionValues()).containsExactly(
                entry(":AMZN_MAPPED_version",
                      versionValue));
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
                                     .optimisticLocking(versionValue, "version")
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
                                             .optimisticLocking(versionValue, versionAttributeName)
                                             .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo(
            "#AMZN_MAPPED_recordVersion = :AMZN_MAPPED_recordVersion");
        assertThat(result.conditionExpression().expressionNames()).containsExactly(
            entry("#AMZN_MAPPED_recordVersion", "recordVersion"));
        assertThat(result.conditionExpression().expressionValues()).containsExactly(
            entry(":AMZN_MAPPED_recordVersion", versionValue));
    }
}

