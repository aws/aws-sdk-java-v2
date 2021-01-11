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
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;

/**
 * Interface for extending the DynamoDb Enhanced client. Two hooks are provided, one that is called just before a record
 * is written to the database, and one called just after a record is read from the database. This gives the extension the
 * opportunity to act as an invisible layer between the application and the database and transform the data accordingly.
 * <p>
 * Multiple extensions can be used with the enhanced client, but the order in which they are loaded is important. For
 * instance one extension may overwrite the value of an attribute that another extension then includes in a checksum
 * calculation.
 */
@SdkPublicApi
public interface DynamoDbEnhancedClientExtension {
    /**
     * This hook is called just before an operation is going to write data to the database. The extension that
     * implements this method can choose to transform the item itself, or add a condition to the write operation
     * or both.
     *
     * @param context The {@link DynamoDbExtensionContext.BeforeWrite} context containing the state of the execution.
     * @return A {@link WriteModification} object that can alter the behavior of the write operation.
     */
    default WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
        return WriteModification.builder().build();
    }

    /**
     * This hook is called just after an operation that has read data from the database. The extension that
     * implements this method can choose to transform the item, and then it is the transformed item that will be
     * mapped back to the application instead of the item that was actually read from the database.
     *
     * @param context The {@link DynamoDbExtensionContext.AfterRead} context containing the state of the execution.
     * @return A {@link ReadModification} object that can alter the results of a read operation.
     */
    default ReadModification afterRead(DynamoDbExtensionContext.AfterRead context) {
        return ReadModification.builder().build();
    }
}
