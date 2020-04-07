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

package software.amazon.awssdk.enhanced.dynamodb.internal.client;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.ChainExtension;

/**
 * Static module to assist with the initialization of an extension for a DynamoDB Enhanced Client based on supplied
 * configuration.
 */
@SdkInternalApi
public final class ExtensionResolver {
    private static final DynamoDbEnhancedClientExtension DEFAULT_VERSIONED_RECORD_EXTENSION =
        VersionedRecordExtension.builder().build();
    private static final List<DynamoDbEnhancedClientExtension> DEFAULT_EXTENSIONS =
        Collections.singletonList(DEFAULT_VERSIONED_RECORD_EXTENSION);

    private ExtensionResolver() {
    }

    /**
     * Static provider for the default extensions that are bundled with the DynamoDB Enhanced Client. Currently this is
     * just the {@link software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension}.
     *
     * These extensions will be used by default unless overridden in the enhanced client builder.
     */
    public static List<DynamoDbEnhancedClientExtension> defaultExtensions() {
        return DEFAULT_EXTENSIONS;
    }

    /**
     * Resolves a list of extensions into a single extension. If the list is a singleton, will just return that extension
     * otherwise it will combine them with the {@link software.amazon.awssdk.enhanced.dynamodb.internal.extensions.ChainExtension}
     * meta-extension using the order provided in the list.
     *
     * @param extensions A list of extensions to be combined in strict order
     * @return A single extension that combines all the supplied extensions or null if no extensions were provided
     */
    public static DynamoDbEnhancedClientExtension resolveExtensions(List<DynamoDbEnhancedClientExtension> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return null;
        }

        if (extensions.size() == 1) {
            return extensions.get(0);
        }

        return ChainExtension.create(extensions);
    }
}
