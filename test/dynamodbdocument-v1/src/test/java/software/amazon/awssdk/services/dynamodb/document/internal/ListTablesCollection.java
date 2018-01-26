/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document.internal;

import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.Page;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.TableCollection;
import software.amazon.awssdk.services.dynamodb.document.spec.ListTablesSpec;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

class ListTablesCollection extends TableCollection<ListTablesResponse> {

    private final DynamoDBClient client;
    private final ListTablesSpec spec;
    private final String startKey;

    ListTablesCollection(DynamoDBClient client, ListTablesSpec spec) {
        this.client = client;
        this.spec = spec;
        this.startKey = spec.getExclusiveStartTableName();
    }

    @Override
    public Page<Table, ListTablesResponse> firstPage() {
        ListTablesRequest request = spec.getRequest()
                .toBuilder()
                .exclusiveStartTableName(startKey)
                .limit(InternalUtils.minimum(
                    spec.maxResultSize(),
                    spec.maxPageSize()))
                .build();
        spec.setRequest(request);
        ListTablesResponse result = client.listTables(request);
        setLastLowLevelResult(result);
        return new ListTablesPage(client, spec, request, 0, result);
    }

    @Override
    public Integer getMaxResultSize() {
        return spec.maxResultSize();
    }
}
