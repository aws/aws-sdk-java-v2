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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.readAndTransformSingleItem;

import java.util.Map;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
public final class TransactGetResultPage {
    private final Map<String, AttributeValue> itemMap;

    private TransactGetResultPage(Map<String, AttributeValue> itemMap) {
        this.itemMap = itemMap;
    }

    public static TransactGetResultPage create(Map<String, AttributeValue> itemMap) {
        return new TransactGetResultPage(itemMap);
    }

    public <T> T getItem(MappedTableResource<T> mappedTableResource) {
        return readAndTransformSingleItem(itemMap,
                                          mappedTableResource.tableSchema(),
                                          OperationContext.create(mappedTableResource.tableName()),
                                          mappedTableResource.mapperExtension());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactGetResultPage that = (TransactGetResultPage) o;

        return itemMap != null ? itemMap.equals(that.itemMap) : that.itemMap == null;
    }

    @Override
    public int hashCode() {
        return itemMap != null ? itemMap.hashCode() : 0;
    }
}
