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

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public enum OperationName {
    NONE(null),
    BATCH_GET_ITEM("BatchGetItem"),
    BATCH_WRITE_ITEM("BatchWriteItem"),
    CREATE_TABLE("CreateTable"),
    DELETE_ITEM("DeleteItem"),
    DELETE_TABLE("DeleteTable"),
    DESCRIBE_TABLE("DescribeTable"),
    GET_ITEM("GetItem"),
    QUERY("Query"),
    PUT_ITEM("PutItem"),
    SCAN("Scan"),
    TRANSACT_GET_ITEMS("TransactGetItems"),
    TRANSACT_WRITE_ITEMS("TransactWriteItems"),
    UPDATE_ITEM("UpdateItem");

    private final String label;

    OperationName() {
        this.label = null;
    }

    OperationName(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
