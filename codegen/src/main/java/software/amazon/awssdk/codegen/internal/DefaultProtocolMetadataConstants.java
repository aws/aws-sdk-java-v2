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

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.protocols.core.OperationMetadataAttribute;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Default implementation of {@link ProtocolMetadataConstants}.
 */
public final class DefaultProtocolMetadataConstants implements ProtocolMetadataConstants {
    private final Set<Map.Entry<Class<?>, OperationMetadataAttribute<?>>> knownKeys = new LinkedHashSet<>();
    private final AttributeMap.Builder map = AttributeMap.builder();

    @Override
    public List<Map.Entry<Class<?>, OperationMetadataAttribute<?>>> keys() {
        return knownKeys.stream().filter(x -> map.get(x.getValue()) != null).collect(Collectors.toList());
    }

    @Override
    public <T> T put(Class<?> containingClass, OperationMetadataAttribute<T> key, T value) {
        knownKeys.add(new AbstractMap.SimpleEntry<>(containingClass, key));
        T oldValue = map.get(key);
        map.put(key, value);
        return oldValue;
    }

    @Override
    public <T> T get(OperationMetadataAttribute<T> key) {
        return map.get(key);
    }
}
