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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A cache that can store lazily initialized MetaTableSchema objects used by the TableSchema creation classes to
 * facilitate self-referencing recursive builds.
 */
@SdkInternalApi
@SuppressWarnings("unchecked")
public class MetaTableSchemaCache {
    private final Map<Class<?>, MetaTableSchema<?>> cacheMap = new HashMap<>();

    public <T> MetaTableSchema<T> getOrCreate(Class<T> mappedClass) {
        return (MetaTableSchema<T>) cacheMap().computeIfAbsent(
            mappedClass, ignored -> MetaTableSchema.create(mappedClass));
    }

    public <T> Optional<MetaTableSchema<T>> get(Class<T> mappedClass) {
        return Optional.ofNullable((MetaTableSchema<T>) cacheMap().get(mappedClass));
    }

    private Map<Class<?>, MetaTableSchema<?>> cacheMap() {
        return this.cacheMap;
    }
}
