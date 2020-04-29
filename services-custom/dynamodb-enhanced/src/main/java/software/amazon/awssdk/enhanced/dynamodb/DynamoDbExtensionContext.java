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

import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A wrapper for the immutable context objects that are visible to the {@link DynamoDbEnhancedClientExtension}s.
 */
@SdkPublicApi
public final class DynamoDbExtensionContext {
    private DynamoDbExtensionContext() {
    }

    @SdkPublicApi
    public interface Context {
        /**
         * @return The {@link AttributeValue} map of the items that is about to be written or has just been read.
         */
        Map<String, AttributeValue> items();

        /**
         * @return The context under which the operation to be modified is taking place.
         */
        OperationContext operationContext();

        /**
         * @return A {@link TableMetadata} object describing the structure of the modelled table.
         */
        TableMetadata tableMetadata();
    }

    /**
     * The state of the execution when the {@link DynamoDbEnhancedClientExtension#beforeWrite} method is invoked.
     */
    @SdkPublicApi
    public interface BeforeWrite extends Context {
    }

    /**
     * The state of the execution when the {@link DynamoDbEnhancedClientExtension#afterRead} method is invoked.
     */
    @SdkPublicApi
    public interface AfterRead extends Context {
    }
}
