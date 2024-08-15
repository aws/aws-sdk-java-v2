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

package software.amazon.awssdk.protocols.core;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Key for additional metadata in {@link OperationInfo}. Used to register protocol specific metadata about
 * an operation.
 *
 * @param <T> Type of metadata.
 */
@SdkProtectedApi
public final class OperationMetadataAttribute<T> extends AttributeMap.Key<T> {

    /**
     * Attribute for a protocol to configure extra headers for the operation.
     */
    public static final OperationMetadataAttribute<Map<String, String>> HTTP_EXTRA_HEADERS =
        new OperationMetadataAttribute<>(new UnsafeValueType(Map.class));

    public OperationMetadataAttribute(Class<T> valueType) {
        super(valueType);
    }

    public OperationMetadataAttribute(UnsafeValueType type) {
        super(type);
    }
}
