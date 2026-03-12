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

package software.amazon.awssdk.codegen.internal;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.protocols.core.OperationMetadataAttribute;

/**
 * Keeps the set of {@link OperationMetadataAttribute} constants attributes per operation/protocol. This is used to codegen
 * those constant values.
 */
public interface ProtocolMetadataConstants {

    /**
     * Returns the list of keys sets. The {@link Map.Entry} contains as key the class containing the key field and the value
     * contains the key constant itself. The class is needed to properly codegen a reference to the key.
     * @return
     */
    List<Map.Entry<Class<?>, OperationMetadataAttribute<?>>> keys();

    /**
     * Adds an operation metadata to the set of constants.
     */
    <T> T put(Class<?> containingClass, OperationMetadataAttribute<T> key, T value);

    /**
     * Adds an operation metadata to the set of constants.
     */
    default <T> T put(OperationMetadataAttribute<T> key, T value) {
        return put(key.getClass(), key, value);
    }

    /**
     * Gets the constant value for the operation metadata key.
     */
    <T> T get(OperationMetadataAttribute<T> key);
}
