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

import java.util.Map;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ChainMapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.WriteModification;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Interface for extending the mapper. Two hooks are provided, one that is called just before a record is written to
 * the database, and one called just after a record is read from the database. This gives the extension the
 * opportunity to act as an invisible layer between the application and the database and transform the data accordingly.
 * <p>
 * Only one extension can be loaded with a mapped table. In order to combine multiple extensions, the
 * {@link ChainMapperExtension} should be used and initialized with all the component extensions to combine together
 * into a chain.
 */
@SdkPublicApi
public interface MapperExtension {
    /**
     * This hook is called just before an operation is going to write data to the database. The extension that
     * implements this method can choose to transform the item itself, or add a condition to the write operation
     * or both.
     * @param item The item that is about to be written.
     * @param operationContext The context under which the operation to be modified is taking place.
     * @param tableMetadata The structure of the table.
     * @return A {@link WriteModification} object that can alter the behavior of the write operation.
     */
    default WriteModification beforeWrite(Map<String, AttributeValue> item,
                                          OperationContext operationContext,
                                          TableMetadata tableMetadata) {
        return WriteModification.builder().build();
    }

    /**
     * This hook is called just after an operation that has read data from the database. The extension that
     * implements this method can choose to transform the item, and then it is the transformed item that will be
     * mapped back to the application instead of the item that was actually read from the database.
     * @param item The item that has just been read.
     * @param operationContext The context under which the operation to be modified is taking place.
     * @param tableMetadata The structure of the table.
     * @return A {@link ReadModification} object that can alter the results of a read operation.
     */
    default ReadModification afterRead(Map<String, AttributeValue> item,
                                       OperationContext operationContext,
                                       TableMetadata tableMetadata) {
        return ReadModification.builder().build();
    }
}
