/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.services.dynamodb.document.Index;
import software.amazon.awssdk.services.dynamodb.document.ItemCollection;
import software.amazon.awssdk.services.dynamodb.document.ScanOutcome;
import software.amazon.awssdk.services.dynamodb.document.spec.ScanSpec;

/**
 * The implementation for <code>ScanApi</code> for an index.
 */
public class IndexScanImpl extends ScanImpl {
    private final Index index;

    public IndexScanImpl(DynamoDBClient client, Index index) {
        super(client, index.getTable());
        this.index = index;
    }

    @Override
    protected ItemCollection<ScanOutcome> doScan(ScanSpec spec) {
        spec.setRequest(spec.getRequest().toBuilder().indexName(index.getIndexName()).build());
        return super.doScan(spec);
    }
}
