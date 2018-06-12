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

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.document.TableCollection;
import software.amazon.awssdk.services.dynamodb.document.api.ListTablesApi;
import software.amazon.awssdk.services.dynamodb.document.spec.ListTablesSpec;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

/**
 * The implementation for <code>ListTablesApi</code>.
 */
public class ListTablesImpl implements ListTablesApi {
    private final DynamoDbClient client;

    public ListTablesImpl(DynamoDbClient client) {
        this.client = client;
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(ListTablesSpec spec) {
        return doList(spec);
    }

    @Override
    public TableCollection<ListTablesResponse> listTables() {
        return doList(new ListTablesSpec());
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(String exclusiveStartTableName) {
        return doList(new ListTablesSpec()
                              .withExclusiveStartTableName(exclusiveStartTableName));
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(String exclusiveStartTableName,
                                                        int maxResultSize) {
        return doList(new ListTablesSpec()
                              .withExclusiveStartTableName(exclusiveStartTableName)
                              .withMaxResultSize(maxResultSize));
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(int maxResultSize) {
        return doList(new ListTablesSpec()
                              .withMaxResultSize(maxResultSize));
    }

    private TableCollection<ListTablesResponse> doList(ListTablesSpec spec) {
        return new ListTablesCollection(client, spec);
    }
}
