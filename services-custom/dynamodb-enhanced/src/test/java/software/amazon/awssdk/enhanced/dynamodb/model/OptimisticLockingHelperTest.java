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

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class OptimisticLockingHelperTest {

    @Test
    public void builderWithOptimisticLocking_deleteItemEnhancedRequest_shouldAddCondition() {
        Key key = Key.builder().partitionValue("test-id").build();
        AttributeValue versionValue = AttributeValue.builder().n("5").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest result = DeleteItemEnhancedRequest.builder()
                                                                    .key(key)
                                                                    .withOptimisticLocking(versionValue, versionAttributeName)
                                                                    .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("version = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }



    @Test
    public void builderWithOptimisticLocking_transactDeleteItemEnhancedRequest_shouldAddCondition() {
        Key key = Key.builder().partitionValue("test-id").build();
        AttributeValue versionValue = AttributeValue.builder().n("10").build();
        String versionAttributeName = "recordVersion";

        TransactDeleteItemEnhancedRequest result = TransactDeleteItemEnhancedRequest.builder()
                                                                                    .key(key)
                                                                                    .withOptimisticLocking(versionValue, versionAttributeName)
                                                                                    .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.conditionExpression()).isNotNull();
        assertThat(result.conditionExpression().expression()).isEqualTo("recordVersion = :version_value");
        assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
    }

    @Test
    public void builderWithOptimisticLocking_preservesExistingRequestProperties() {
        Key key = Key.builder().partitionValue("test-id").build();
        AttributeValue versionValue = AttributeValue.builder().n("3").build();

        DeleteItemEnhancedRequest result = DeleteItemEnhancedRequest.builder()
                                                                    .key(key)
                                                                    .returnConsumedCapacity("TOTAL")
                                                                    .withOptimisticLocking(versionValue, "version")
                                                                    .build();

        assertThat(result.key()).isEqualTo(key);
        assertThat(result.returnConsumedCapacityAsString()).isEqualTo("TOTAL");
        assertThat(result.conditionExpression()).isNotNull();
    }

    @Test
    public void builderWithOptimisticLocking_differentVersionAttributeNames_shouldWork() {
        Key key = Key.builder().partitionValue("test-id").build();
        AttributeValue versionValue = AttributeValue.builder().n("1").build();

        // Test with different attribute names
        String[] attributeNames = {"version", "recordVersion", "itemVersion", "v"};

        for (String attributeName : attributeNames) {
            DeleteItemEnhancedRequest result = DeleteItemEnhancedRequest.builder()
                                                                        .key(key)
                                                                        .withOptimisticLocking(versionValue, attributeName)
                                                                        .build();

            assertThat(result.conditionExpression().expression()).isEqualTo(attributeName + " = :version_value");
            assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
        }
    }

    @Test
    public void builderWithOptimisticLocking_differentVersionValues_shouldWork() {
        Key key = Key.builder().partitionValue("test-id").build();

        // Test with different version values
        AttributeValue[] versionValues = {
            AttributeValue.builder().n("0").build(),
            AttributeValue.builder().n("1").build(),
            AttributeValue.builder().n("999").build(),
            AttributeValue.builder().n("123456789").build()
        };

        for (AttributeValue versionValue : versionValues) {
            DeleteItemEnhancedRequest result = DeleteItemEnhancedRequest.builder()
                                                                        .key(key)
                                                                        .withOptimisticLocking(versionValue, "version")
                                                                        .build();

            assertThat(result.conditionExpression().expressionValues()).containsEntry(":version_value", versionValue);
        }
    }
}