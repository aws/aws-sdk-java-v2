/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Default implementation of {@link SdkAutoConstructMap}.
 * <p>
 * This is an empty, unmodifiable map.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
@SdkProtectedApi
public final class DefaultSdkAutoConstructMap<K, V> implements SdkAutoConstructMap<K, V> {
    private static final DefaultSdkAutoConstructMap INSTANCE = new DefaultSdkAutoConstructMap();

    private final Map<K, V> impl = Collections.unmodifiableMap(Collections.emptyMap());

    private DefaultSdkAutoConstructMap() {
    }

    @SuppressWarnings("unchecked")
    public static <K, V> DefaultSdkAutoConstructMap<K, V> getInstance() {
        return (DefaultSdkAutoConstructMap<K, V>) INSTANCE;
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return impl.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return impl.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return impl.get(key);
    }

    @Override
    public V put(K key, V value) {
        return impl.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return impl.get(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        impl.putAll(m);
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public Set<K> keySet() {
        return impl.keySet();
    }

    @Override
    public Collection<V> values() {
        return impl.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return impl.entrySet();
    }
}
