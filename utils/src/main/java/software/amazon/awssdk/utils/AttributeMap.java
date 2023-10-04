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

package software.amazon.awssdk.utils;

import static java.util.Collections.emptySet;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ToBuilderIgnoreField;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A map from {@code AttributeMap.Key<T>} to {@code T} that ensures the values stored with a key matches the type associated with
 * the key. This does not implement {@link Map} because it has more strict typing requirements, but a {@link Map} can be
 * converted
 * to an {code AttributeMap} via the type-unsafe {@link AttributeMap} method.
 *
 * This can be used for storing configuration values ({@code OptionKey.LOG_LEVEL} to {@code Boolean.TRUE}), attaching
 * arbitrary attributes to a request chain ({@code RequestAttribute.CONFIGURATION} to {@code ClientConfiguration}) or similar
 * use-cases.
 */
@SdkProtectedApi
@Immutable
public final class AttributeMap implements ToCopyableBuilder<AttributeMap.Builder, AttributeMap>, SdkAutoCloseable {
    private static final AttributeMap EMPTY = AttributeMap.builder().build();

    private final Map<Key<?>, Value<?>> attributes;
    private final DependencyGraph dependencyGraph;

    private AttributeMap(Builder builder) {
        this.attributes = builder.attributes;
        this.dependencyGraph = builder.dependencyGraph;

        // Resolve all of our attributes ahead-of-time, so that we're properly thread-safe
        this.attributes.values().forEach(v -> getAndRecordDependencies(dependencyGraph, v, this::get));
    }

    /**
     * Return true if the provided key is configured in this map. Useful for differentiating between whether the provided key was
     * not configured in the map or if it is configured, but its value is null.
     */
    public <T> boolean containsKey(Key<T> typedKey) {
        return attributes.containsKey(typedKey);
    }

    /**
     * Get the value associated with the provided key from this map. This will return null if the value is not set or if the
     * value stored is null. These cases can be disambiguated using {@link #containsKey(Key)}.
     */
    public <T> T get(Key<T> key) {
        Validate.notNull(key, "Key to retrieve must not be null.");
        Value<?> value = attributes.get(key);
        if (value == null) {
            return null;
        }
        return key.convertValue(getAndRecordDependencies(dependencyGraph, value, this::get));
    }

    /**
     * Merges two AttributeMaps into one. This object is given higher precedence then the attributes passed in as a parameter.
     *
     * @param lowerPrecedence Options to merge into 'this' AttributeMap object. Any attribute already specified in 'this' object
     *                        will be left as is since it has higher precedence.
     * @return New options with values merged.
     */
    public AttributeMap merge(AttributeMap lowerPrecedence) {
        Builder resultBuilder = new AttributeMap.Builder(this);
        lowerPrecedence.attributes.forEach((k, v) -> {
            resultBuilder.internalPutIfAbsent(k, () -> {
                Value<?> result = v.copy();
                result.clearCache();
                return result;
            });
        });
        return resultBuilder.build();
    }

    public static AttributeMap empty() {
        return EMPTY;
    }

    public AttributeMap copy() {
        return toBuilder().build();
    }

    @Override
    public void close() {
        attributes.values().forEach(Value::close);
    }

    /**
     * An abstract class extended by pseudo-enums defining the key for data that is stored in the {@link AttributeMap}. For
     * example, a {@code ClientOption<T>} may extend this to define options that can be stored in an {@link AttributeMap}.
     */
    public abstract static class Key<T> {
        private final Class<?> valueType;
        private final Function<Object, T> convertMethod;

        protected Key(Class<T> valueType) {
            this.valueType = valueType;
            this.convertMethod = valueType::cast;
        }

        protected Key(UnsafeValueType unsafeValueType) {
            this.valueType = unsafeValueType.valueType;
            this.convertMethod = v -> (T) v; // 🙏
        }

        @Override
        public String toString() {
            return "Key(" + valueType.getName() + ")";
        }

        /**
         * Useful for parameterized types.
         */
        protected static class UnsafeValueType {
            private final Class<?> valueType;

            public UnsafeValueType(Class<?> valueType) {
                this.valueType = valueType;
            }
        }

        /**
         * Validate the provided value is of the correct type and convert it to the proper type for this option.
         */
        public final T convertValue(Object value) {
            return convertMethod.apply(value);
        }
    }

    @Override
    public String toString() {
        return attributes.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AttributeMap)) {
            return false;
        }
        AttributeMap rhs = (AttributeMap) obj;
        if (attributes.size() != rhs.attributes.size()) {
            return false;
        }

        for (Key<?> lhsKey : attributes.keySet()) {
            Object lhsValue = get(lhsKey);
            Object rhsValue = rhs.get(lhsKey);
            if (!Objects.equals(lhsValue, rhsValue)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Key<?> key : attributes.keySet()) {
            hashCode = 31 * hashCode + Objects.hashCode(get(key));
        }
        return hashCode;
    }

    @Override
    @ToBuilderIgnoreField("configuration")
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final class DependencyGraph {
        /**
         * Inverted adjacency list of dependencies between derived keys. Mapping from a key to what depends on that key.
         */
        private final Map<Key<?>, Set<Value<?>>> dependents;

        private DependencyGraph() {
            this.dependents = new HashMap<>();
        }

        private DependencyGraph(Map<Key<?>, Set<Value<?>>> dependents,
                                Function<Value<?>, Value<?>> valueRemapping) {
            this.dependents = new HashMap<>(dependents.size());
            dependents.forEach((key, values) -> {
                Set<Value<?>> newValues = new HashSet<>(values.size());
                this.dependents.put(key, newValues);
                values.forEach(v -> newValues.add(valueRemapping.apply(v)));
            });
        }

        private void addDependency(Value<?> consumer, Key<?> dependency) {
            dependents.computeIfAbsent(dependency, k -> new HashSet<>()).add(consumer);
        }

        private Set<Value<?>> dependents(Key<?> key) {
            return dependents.getOrDefault(key, emptySet());
        }

        private void invalidateConsumerCaches(Key<?> key) {
            Set<Value<?>> dependents = dependents(key);
            if (dependents.isEmpty()) {
                return;
            }

            Queue<Value<?>> unloadQueue = new ArrayDeque<>(dependents);
            dependents.clear();
            while (!unloadQueue.isEmpty()) {
                Value<?> toUnload = unloadQueue.poll();

                Set<Value<?>> toUnloadDependents = dependents(toUnload.key());
                unloadQueue.addAll(toUnloadDependents);
                toUnloadDependents.clear();

                toUnload.clearCache();
            }
        }

        public DependencyGraph copy(Function<Value<?>, Value<?>> valueRemapping) {
            return new DependencyGraph(dependents, valueRemapping);
        }
    }

    public static final class Builder implements CopyableBuilder<Builder, AttributeMap> {
        private Map<Key<?>, Value<?>> attributes;
        private DependencyGraph dependencyGraph;
        private boolean copyOnUpdate;

        private Builder() {
            this.attributes = new HashMap<>();
            this.dependencyGraph = new DependencyGraph();
            this.copyOnUpdate = false;
        }

        private Builder(AttributeMap attributeMap) {
            this.attributes = attributeMap.attributes;
            this.dependencyGraph = attributeMap.dependencyGraph;
            this.copyOnUpdate = true;
        }

        public <T> T get(Key<T> key) {
            Validate.notNull(key, "Key to retrieve must not be null.");
            Value<?> value = attributes.get(key);
            if (value == null) {
                return null;
            }
            return key.convertValue(getAndRecordDependencies(dependencyGraph, value, this::get));
        }

        /**
         * Add a mapping between the provided key and value.
         */
        public <T> Builder put(Key<T> key, T value) {
            Validate.notNull(key, "Key to set must not be null.");
            internalPut(key, new ConstantValue<>(key, value));
            return this;
        }

        public <T> Builder putLazy(Key<T> key, LazyValue<T> lazyValue) {
            Validate.notNull(key, "Key to set must not be null.");
            internalPut(key, new DerivedValue<>(key, lazyValue));
            return this;
        }

        public <T> Builder putLazyIfAbsent(Key<T> key, LazyValue<T> lazyValue) {
            Validate.notNull(key, "Key to set must not be null.");
            internalPutIfAbsent(key, () -> new DerivedValue<>(key, lazyValue));
            return this;
        }

        /**
         * Adds all the attributes from the map provided. This is not type safe, and will throw an exception during creation if
         * a value in the map is not of the correct type for its key.
         */
        public Builder putAll(Map<? extends Key<?>, ?> attributes) {
            attributes.forEach(this::internalUnsafePutConstant);
            return this;
        }

        private <T> void internalUnsafePutConstant(Key<T> key, Object value) {
            try {
                T tValue = key.convertValue(value);
                internalPut(key, new ConstantValue<>(key, tValue));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Cannot write " + value.getClass() + " type to key " + key, e);
            }
        }

        private void internalPut(Key<?> key, Value<?> value) {
            checkCopyOnUpdate();
            dependencyGraph.invalidateConsumerCaches(key);
            attributes.put(key, value);
        }

        private Builder internalPutIfAbsent(Key<?> key, Supplier<Value<?>> value) {
            checkCopyOnUpdate();
            attributes.compute(key, (k, v) -> {
                if (v == null || getAndRecordDependencies(dependencyGraph, v, this::get) == null) {
                    Value<?> newValue = value.get();
                    dependencyGraph.invalidateConsumerCaches(key);
                    return newValue;
                }
                return v;
            });
            return this;
        }

        private void checkCopyOnUpdate() {
            if (copyOnUpdate) {
                Map<Key<?>, Value<?>> attributesToCopy = attributes;
                attributes = new HashMap<>(attributesToCopy.size());
                Map<Value<?>, Value<?>> valueRemapping = new HashMap<>(attributesToCopy.size());
                attributesToCopy.forEach((k, v) -> {
                    Value<?> newValue = v.copy();
                    valueRemapping.put(v, newValue);
                    attributes.put(k, newValue);
                });
                dependencyGraph = dependencyGraph.copy(valueRemapping::get);
                copyOnUpdate = false;
            }
        }

        @Override
        public AttributeMap build() {
            AttributeMap result = new AttributeMap(this);
            copyOnUpdate = true;
            return result;
        }
    }

    @FunctionalInterface
    public interface LazyValue<T> {
        T get(LazyValueSource source);
    }

    @FunctionalInterface
    public interface LazyValueSource {
        <T> T get(Key<T> sourceKey);
    }

    private interface Value<T> extends SdkAutoCloseable {
        Key<T> key();

        T get(LazyValueSource source);

        Value<T> copy();

        void clearCache();
    }

    private static class ConstantValue<T> implements Value<T> {
        private final Key<T> key;
        private final T value;

        private ConstantValue(Key<T> key, T value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Key<T> key() {
            return key;
        }

        @Override
        public T get(LazyValueSource source) {
            return value;
        }

        @Override
        public Value<T> copy() {
            return this;
        }

        @Override
        public void clearCache() {
        }

        @Override
        public void close() {
            IoUtils.closeIfCloseable(value, null);
            shutdownIfExecutorService(value);
        }

        @Override
        public String toString() {
            return "Value(" + value + ")";
        }
    }

    private static final class DerivedValue<T> implements Value<T> {
        private final Key<T> key;
        private final LazyValue<T> lazyValue;
        private boolean valueCached = false;
        private T value;

        private boolean onStack = false;

        private DerivedValue(Key<T> key, LazyValue<T> lazyValue) {
            this.key = key;
            this.lazyValue = lazyValue;
        }

        private DerivedValue(Key<T> key, LazyValue<T> lazyValue, boolean valueCached, T value) {
            this(key, lazyValue);
            this.valueCached = valueCached;
            this.value = value;
        }

        @Override
        public Key<T> key() {
            return key;
        }

        @Override
        public T get(LazyValueSource source) {
            primeCache(source);
            return value;
        }

        private void primeCache(LazyValueSource source) {
            if (!valueCached) {
                if (onStack) {
                    throw new IllegalStateException("Derived key " + key + " attempted to read itself");
                }
                try {
                    onStack = true;
                    value = lazyValue.get(source);
                } finally {
                    onStack = false;
                }
                valueCached = true;
            }
        }

        @Override
        public Value<T> copy() {
            return new DerivedValue<>(key, lazyValue, valueCached, value);
        }

        @Override
        public void clearCache() {
            valueCached = false;
        }

        @Override
        public void close() {
            IoUtils.closeIfCloseable(value, null);
            shutdownIfExecutorService(value);
        }

        @Override
        public String toString() {
            if (valueCached) {
                return "Value(" + value + ")";
            }
            return "Value(<<lazy>>)";
        }
    }

    public static <T> T getAndRecordDependencies(DependencyGraph dependencyGraph, Value<T> value, LazyValueSource delegateGet) {
        return value.get(new LazyValueSource() {
            @Override
            public <U> U get(Key<U> key) {
                dependencyGraph.addDependency(value, key);
                return delegateGet.get(key);
            }
        });
    }

    private static void shutdownIfExecutorService(Object object) {
        if (object instanceof ExecutorService) {
            ExecutorService executor = (ExecutorService) object;
            executor.shutdown();
        }
    }
}
