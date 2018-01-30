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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.Page;
import software.amazon.awssdk.services.dynamodb.document.Table;
import software.amazon.awssdk.services.dynamodb.document.spec.ListTablesSpec;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

class ListTablesPage extends Page<Table, ListTablesResponse> {
    private final DynamoDBClient client;
    private final ListTablesSpec spec;
    private ListTablesRequest request;
    private final int index;
    private final String lastEvaluatedKey;

    ListTablesPage(
        DynamoDBClient client,
        ListTablesSpec spec,
        ListTablesRequest request,
        int index,
        ListTablesResponse result) {
        super(Collections.unmodifiableList(
                toTableList(client, result.tableNames())),
              result);
        this.client = client;
        this.spec = spec;
        this.request = request;
        this.index = index;
        Integer max = spec.maxResultSize();
        if (max != null && (index + result.tableNames().size()) > max) {
            this.lastEvaluatedKey = null;
        } else {
            this.lastEvaluatedKey = result.lastEvaluatedTableName();
        }
    }

    private static List<Table> toTableList(DynamoDBClient client, List<String> tableNames) {
        if (tableNames == null) {
            return null;
        }
        List<Table> result = new ArrayList<Table>(tableNames.size());
        for (String tableName : tableNames) {
            result.add(new Table(client, tableName));
        }
        return result;
    }

    @Override
    public boolean hasNextPage() {
        if (lastEvaluatedKey == null) {
            return false;
        }
        Integer max = spec.maxResultSize();
        if (max == null) {
            return true;
        }
        return nextRequestLimit(max.intValue()) > 0;
    }

    private int nextRequestLimit(int max) {
        int nextIndex = index + this.size();
        return InternalUtils.minimum(
                max - nextIndex,
                spec.maxPageSize());
    }

    @Override
    public Page<Table, ListTablesResponse> nextPage() {
        if (lastEvaluatedKey == null) {
            throw new NoSuchElementException("No more pages");
        }
        final Integer max = spec.maxResultSize();
        if (max != null) {
            int nextLimit = nextRequestLimit(max.intValue());
            if (nextLimit == 0) {
                throw new NoSuchElementException("No more pages");
            }
            request = request.toBuilder().limit(nextLimit).build();
        }
        request = request.toBuilder().exclusiveStartTableName(lastEvaluatedKey).build();
        ListTablesResponse result = client.listTables(request);
        final int nextIndex = index + this.size();
        return new ListTablesPage(client, spec, request, nextIndex, result);
    }
}
