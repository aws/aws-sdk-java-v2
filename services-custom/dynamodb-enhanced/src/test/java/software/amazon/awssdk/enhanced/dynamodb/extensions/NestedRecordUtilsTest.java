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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static software.amazon.awssdk.enhanced.dynamodb.internal.extensions.utility.NestedRecordUtils.getTableSchemaForListElement;
import static software.amazon.awssdk.enhanced.dynamodb.internal.extensions.utility.NestedRecordUtils.resolveSchemasPerPath;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.TimestampListElement;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedRecordWithUpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithUpdateBehaviors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class NestedRecordUtilsTest {

    @Test
    public void getTableSchemaForListElement_shouldReturnElementSchema() {
        TableSchema<NestedRecordWithUpdateBehavior> parentSchema = TableSchema.fromBean(NestedRecordWithUpdateBehavior.class);

        TableSchema<?> childSchema = getTableSchemaForListElement(parentSchema, "nestedRecordList");

        Assertions.assertNotNull(childSchema);
        Assertions.assertEquals(TableSchema.fromBean(TimestampListElement.class), childSchema);
    }

    @Test
    public void resolveSchemasPerPath_shouldResolveNestedPaths() {
        TableSchema<RecordWithUpdateBehaviors> rootSchema = TableSchema.fromBean(RecordWithUpdateBehaviors.class);

        Map<String, AttributeValue> attributesToSet = new HashMap<>();
        attributesToSet.put("nestedRecord_NESTED_ATTR_UPDATE_nestedRecord_NESTED_ATTR_UPDATE_attribute",
                            AttributeValue.builder().s("attributeValue").build());

        Map<String, TableSchema<?>> result = resolveSchemasPerPath(attributesToSet, rootSchema);

        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.containsKey(""));
        Assertions.assertTrue(result.containsKey("nestedRecord"));
        Assertions.assertTrue(result.containsKey("nestedRecord.nestedRecord"));
    }
}
