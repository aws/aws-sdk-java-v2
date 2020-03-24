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

package software.amazon.awssdk.enhanced.dynamodb;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A document representing a table item in the form of a map containing attributes and values.
 * <p>
 * Use the {@link #getItem(MappedTableResource)} method to transform the collection of attributes into a typed item.
 */
@SdkPublicApi
public interface Document {

    /**
     * Get the table item associated with the table schema in the mapped table resource.
     *
     * @param mappedTableResource the mapped table resource this item was retrieved from
     * @param <T> the type of items in the mapped table resource
     * @return the item constructed from the document
     */
    <T> T getItem(MappedTableResource<T> mappedTableResource);
}
