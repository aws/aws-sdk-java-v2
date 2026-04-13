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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OperationNameTest {

    @Test
    public void values_returnsExpectedConstantsInDeclarationOrder() {
        assertArrayEquals(new OperationName[] {
            OperationName.NONE,
            OperationName.BATCH_GET_ITEM,
            OperationName.BATCH_WRITE_ITEM,
            OperationName.CREATE_TABLE,
            OperationName.DELETE_ITEM,
            OperationName.DELETE_TABLE,
            OperationName.DESCRIBE_TABLE,
            OperationName.GET_ITEM,
            OperationName.QUERY,
            OperationName.PUT_ITEM,
            OperationName.SCAN,
            OperationName.TRANSACT_GET_ITEMS,
            OperationName.TRANSACT_WRITE_ITEMS,
            OperationName.UPDATE_ITEM
        }, OperationName.values());
    }

    @Test
    public void label_returnsExpectedLabelForEachConstant() {
        int operationNameCount = OperationName.values().length;
        assertEquals(14, operationNameCount);

        assertNull(OperationName.NONE.label());
        assertEquals("BatchGetItem", OperationName.BATCH_GET_ITEM.label());
        assertEquals("BatchWriteItem", OperationName.BATCH_WRITE_ITEM.label());
        assertEquals("CreateTable", OperationName.CREATE_TABLE.label());
        assertEquals("DeleteItem", OperationName.DELETE_ITEM.label());
        assertEquals("DeleteTable", OperationName.DELETE_TABLE.label());
        assertEquals("DescribeTable", OperationName.DESCRIBE_TABLE.label());
        assertEquals("GetItem", OperationName.GET_ITEM.label());
        assertEquals("Query", OperationName.QUERY.label());
        assertEquals("PutItem", OperationName.PUT_ITEM.label());
        assertEquals("Scan", OperationName.SCAN.label());
        assertEquals("TransactGetItems", OperationName.TRANSACT_GET_ITEMS.label());
        assertEquals("TransactWriteItems", OperationName.TRANSACT_WRITE_ITEMS.label());
        assertEquals("UpdateItem", OperationName.UPDATE_ITEM.label());
    }
}
