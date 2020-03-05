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

import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Shared interface components for {@link DynamoDbEnhancedClient} and {@link DynamoDbEnhancedAsyncClient}. Any common
 * methods implemented by both of those classes or their builders are declared here.
 */
@SdkPublicApi
public interface DynamoDbEnhancedResource {
    /**
     * Shared interface components for the builders of {@link DynamoDbEnhancedClient} and
     * {@link DynamoDbEnhancedAsyncClient}
     */
    interface Builder {
        /**
         * Specifies the extensions to load with the enhanced client. The extensions will be loaded in the strict order
         * they are supplied here. Calling this method will override any bundled extensions that are loaded by default,
         * namely the {@link software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension}, so this
         * extension must be included in the supplied list otherwise it will not be loaded. Providing an empty list here
         * will cause no extensions to get loaded, effectively dropping the default ones.
         *
         * @param dynamoDbEnhancedClientExtensions a list of extensions to load with the enhanced client
         */
        Builder extensions(DynamoDbEnhancedClientExtension... dynamoDbEnhancedClientExtensions);

        /**
         * Specifies the extensions to load with the enhanced client. The extensions will be loaded in the strict order
         * they are supplied here. Calling this method will override any bundled extensions that are loaded by default,
         * namely the {@link software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension}, so this
         * extension must be included in the supplied list otherwise it will not be loaded. Providing an empty list here
         * will cause no extensions to get loaded, effectively dropping the default ones.
         *
         * @param dynamoDbEnhancedClientExtensions a list of extensions to load with the enhanced client
         */
        Builder extensions(List<DynamoDbEnhancedClientExtension> dynamoDbEnhancedClientExtensions);
    }
}
