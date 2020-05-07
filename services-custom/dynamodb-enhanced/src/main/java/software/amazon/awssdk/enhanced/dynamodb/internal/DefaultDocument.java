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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.readAndTransformSingleItem;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Document;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class DefaultDocument implements Document {
    private final Map<String, AttributeValue> itemMap;

    private DefaultDocument(Map<String, AttributeValue> itemMap) {
        this.itemMap = itemMap;
    }

    public static DefaultDocument create(Map<String, AttributeValue> itemMap) {
        return new DefaultDocument(itemMap);
    }

    public <T> T getItem(MappedTableResource<T> mappedTableResource) {
        return readAndTransformSingleItem(itemMap,
                                          mappedTableResource.tableSchema(),
                                          DefaultOperationContext.create(mappedTableResource.tableName()),
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

        DefaultDocument that = (DefaultDocument) o;

        return itemMap != null ? itemMap.equals(that.itemMap) : that.itemMap == null;
    }

    @Override
    public int hashCode() {
        return itemMap != null ? itemMap.hashCode() : 0;
    }
}
