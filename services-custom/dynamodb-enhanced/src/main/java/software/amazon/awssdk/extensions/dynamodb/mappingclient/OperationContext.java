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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class OperationContext {
    private final String tableName;
    private final String indexName;

    private OperationContext(String tableName, String indexName) {
        this.tableName = tableName;
        this.indexName = indexName;
    }

    public static OperationContext create(String tableName, String indexName) {
        return new OperationContext(tableName, indexName);
    }

    public static OperationContext create(String tableName) {
        return new OperationContext(tableName, TableMetadata.primaryIndexName());
    }

    public String tableName() {
        return tableName;
    }

    public String indexName() {
        return indexName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OperationContext that = (OperationContext) o;

        if (tableName != null ? ! tableName.equals(that.tableName) : that.tableName != null) {
            return false;
        }
        return indexName != null ? indexName.equals(that.indexName) : that.indexName == null;
    }

    @Override
    public int hashCode() {
        int result = tableName != null ? tableName.hashCode() : 0;
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        return result;
    }
}
