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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.flattenmap.FlattenMapInvalidBean;

public class InvalidFlattenMapTest extends LocalDynamoDbSyncTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void updateItemWithFlattenMap_withMultipleAnnotatedMaps_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("More than one @DynamoDbFlattenMap annotation found on the same record");

        TableSchema.fromClass(FlattenMapInvalidBean.class);
    }
}