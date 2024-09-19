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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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

        return key.convertValue(value.get(new ExpectCachedLazyValueSource()));
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
            resultBuilder.internalComputeIfAbsent(k, () -> {
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

    public AttributeMap copy() {
        return toBuilder().build();
    }

    @Override
    @ToBuilderIgnoreField("configuration")
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
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

        private Builder(Builder builder) {
            this.attributes = builder.attributes;
            this.dependencyGraph = builder.dependencyGraph;
            this.copyOnUpdate = true;
            checkCopyOnUpdate(); // Proactively copy the values out of the source builder.
        }

        /**
         * Get the value for the provided key.
         */
        public <T> T get(Key<T> key) {
            return key.convertValue(internalGet(null, key));
        }

        /**
         * Add a mapping between the provided key and value, if the current value for the key is null. Returns the value.
         */
        public <T> T computeIfAbsent(Key<T> key, Supplier<T> valueIfAbsent) {
            Validate.notNull(key, "Key to set must not be null.");
            Value<?> result = internalComputeIfAbsent(key, () -> {
                T value = valueIfAbsent.get();
                return new ConstantValue<>(value);
            });
            return key.convertValue(resolveValue(result));
        }

        /**
         * Add a mapping between the provided key and value.
         */
        public <T> Builder put(Key<T> key, T value) {
            Validate.notNull(key, "Key to set must not be null.");
            internalPut(key, new ConstantValue<>(value));
            return this;
        }

        /**
         * Add a mapping between the provided key and value provider.
         *
         * The lazy value will only be resolved when the value is needed. During resolution, the lazy value is provided with a
         * value reader. The value reader will fail if the reader attempts to read its own value (directly, or indirectly
         * through other lazy values).
         *
         * If a value is updated that a lazy value is depended on, the lazy value will be re-resolved the next time the lazy
         * value is accessed.
         */
        public <T> Builder putLazy(Key<T> key, LazyValue<T> lazyValue) {
            Validate.notNull(key, "Key to set must not be null.");
            internalPut(key, new DerivedValue<>(lazyValue));
            return this;
        }

        /**
         * Equivalent to {@link #putLazy(Key, LazyValue)}, but does not assign the value if there is
         * already a non-null value assigned for the provided key.
         */
        public <T> Builder putLazyIfAbsent(Key<T> key, LazyValue<T> lazyValue) {
            Validate.notNull(key, "Key to set must not be null.");
            internalComputeIfAbsent(key, () -> new DerivedValue<>(lazyValue));
            return this;
        }

        /**
         * Adds all the attributes from the map provided. This is not type safe, and will throw an exception during creation if
         * a value in the map is not of the correct type for its key.
         */
        public Builder putAll(Map<? extends Key<?>, ?> attributes) {
            attributes.forEach(this::unsafeInternalPutConstant);
            return this;
        }

        /**
         * Put all of the attributes from the provided map into this one. This will resolve lazy attributes and store their
         * value as a constant, so this should only be used when the source attributes are constants or it's okay that the
         * values will no longer be lazy.
         */
        public Builder putAll(AttributeMap attributes) {
            attributes.attributes.forEach((k, v) -> unsafeInternalPutConstant(k, attributes.get(k)));
            return this;
        }

        /**
         * Equivalent to {@link #internalPut(Key, Value)} with a constant value, but uses runtime check to verify the value.
         * This is useful when type safety of the value isn't possible.
         */
        private <T> void unsafeInternalPutConstant(Key<T> key, Object value) {
            try {
                T tValue = key.convertValue(value);
                internalPut(key, new ConstantValue<>(tValue));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Cannot write " + value.getClass() + " type to key " + key, e);
            }
        }

        /**
         * Update the value for the provided key.
         */
        private void internalPut(Key<?> key, Value<?> value) {
            Validate.notNull(value, "Value must not be null.");
            checkCopyOnUpdate();

            Value<?> oldValue = attributes.put(key, value);
            if (oldValue != null) {
                dependencyGraph.valueUpdated(oldValue, value);
            }
        }

        /**
         * If the current value for the provided key is null (or doesn't exist), set it using the provided value supplier.
         * Returns the new value that was set.
         */
        private Value<?> internalComputeIfAbsent(Key<?> key, Supplier<Value<?>> value) {
            checkCopyOnUpdate();
            Value<?> currentValue = attributes.get(key);
            if (currentValue == null || resolveValue(currentValue) == null) {
                Value<?> newValue = value.get();
                Validate.notNull(newValue, "Supplied value must not be null.");
                if (currentValue != null) {
                    dependencyGraph.valueUpdated(currentValue, newValue);
                }
                attributes.put(key, newValue);
                return newValue;
            }
            return currentValue;
        }

        private void checkCopyOnUpdate() {
            if (copyOnUpdate) {
                Map<Key<?>, Value<?>> attributesToCopy = attributes;
                attributes = new HashMap<>(attributesToCopy.size());
                Map<Value<?>, Value<?>> valueRemapping = new IdentityHashMap<>(attributesToCopy.size());
                attributesToCopy.forEach((k, v) -> {
                    Value<?> newValue = v.copy();
                    valueRemapping.put(v, newValue);
                    attributes.put(k, newValue);
                });
                dependencyGraph = dependencyGraph.copy(valueRemapping);
                copyOnUpdate = false;
            }
        }

        @Override
        public AttributeMap build() {
            // Resolve all of the attributes ahead of creating the attribute map, so that values can be read without any magic.
            Collection<Value<?>> valuesToPrime = new ArrayList<>(this.attributes.values());
            valuesToPrime.forEach(this::resolveValue);
            copyOnUpdate = true;
            return new AttributeMap(this);
        }

        @Override
        public Builder copy() {
            return new Builder(this);
        }

        /**
         * Retrieve the value for the provided key, with the provided requesting value (used for tracking consumers in the
         * dependency graph). Requester may be null if this isn't a call within a derived value.
         */
        private <T> T internalGet(Value<?> requester, Key<T> key) {
            Validate.notNull(key, "Key to retrieve must not be null.");
            Value<?> value;
            if (requester != null) {
                checkCopyOnUpdate();
                value = attributes.computeIfAbsent(key, k -> new ConstantValue<>(null));
                dependencyGraph.addDependency(requester, value);
            } else {
                value = attributes.get(key);
                if (value == null) {
                    return null;
                }
            }
            return key.convertValue(resolveValue(value));
        }

        /**
         * Resolve the provided value, making sure to record any of its dependencies in the dependency graph.
         */
        private <T> T resolveValue(Value<T> value) {
            Validate.notNull(value,
                             "Encountered a null value when resolving configuration attributes. This is commonly "
                             + "caused by concurrent modifications to non-thread-safe types. Ensure you're "
                             + "synchronizing access to all non-thread-safe types.");
            return value.get(new LazyValueSource() {
                @Override
                public <U> U get(Key<U> innerKey) {
                    return internalGet(value, innerKey);
                }
            });
        }
    }

    /**
     * A value that is evaluated lazily. See {@link Builder#putLazy(Key, LazyValue)}.
     */
    @FunctionalInterface
    public interface LazyValue<T> {
        T get(LazyValueSource source);
    }

    /**
     * A source for other values, provided to a {@link LazyValue} when the value is resolved.
     */
    @FunctionalInterface
    public interface LazyValueSource {
        <T> T get(Key<T> sourceKey);
    }

    /**
     * Tracks which values "depend on" other values, so that when we update one value, when can clear the cache of any other
     * values that were derived from the value that was updated.
     */
    private static final class DependencyGraph {
        /**
         * Inverted adjacency list of dependencies between derived values. Mapping from a value to what depends on that value.
         */
        private final Map<Value<?>, Set<Value<?>>> dependents;

        private DependencyGraph() {
            this.dependents = new IdentityHashMap<>();
        }

        private DependencyGraph(DependencyGraph source,
                                Map<Value<?>, Value<?>> valueRemapping) {
            this.dependents = new IdentityHashMap<>(source.dependents.size());

            source.dependents.forEach((key, values) -> {
                Set<Value<?>> newValues = new HashSet<>(values.size());
                Value<?> remappedKey = valueRemapping.get(key);
                Validate.notNull(remappedKey, "Remapped key must not be null.");
                this.dependents.put(remappedKey, newValues);
                values.forEach(v -> {
                    Value<?> remappedValue = valueRemapping.get(v);
                    Validate.notNull(remappedValue, "Remapped value must not be null.");
                    newValues.add(remappedValue);
                });
            });
        }

        private void addDependency(Value<?> consumer, Value<?> dependency) {
            Validate.notNull(consumer, "Consumer must not be null.");
            dependents.computeIfAbsent(dependency, k -> new HashSet<>()).add(consumer);
        }

        private void valueUpdated(Value<?> oldValue, Value<?> newValue) {
            if (oldValue == newValue) {
                // Optimization: if we didn't actually update the value, do nothing.
                return;
            }

            CachedValue<?> oldCachedValue = oldValue.cachedValue();
            CachedValue<?> newCachedValue = newValue.cachedValue();

            if (!CachedValue.haveSameCachedValues(oldCachedValue, newCachedValue)) {
                // Optimization: don't invalidate consumer caches if the value hasn't changed.
                invalidateConsumerCaches(oldValue);
            }

            Set<Value<?>> oldValueDependents = dependents.remove(oldValue);
            if (oldValueDependents != null) {
                dependents.put(newValue, oldValueDependents);
            }

            // TODO: Explore optimizations to not have to update every dependent value.
            dependents.values().forEach(v -> {
                if (v.remove(oldValue)) {
                    v.add(newValue);
                }
            });
        }

        private void invalidateConsumerCaches(Value<?> value) {
            Queue<Value<?>> unloadQueue = new ArrayDeque<>();
            unloadQueue.add(value);
            while (!unloadQueue.isEmpty()) {
                Value<?> toUnload = unloadQueue.poll();
                toUnload.clearCache();
                Set<Value<?>> toUnloadDependents = dependents.remove(toUnload);
                if (toUnloadDependents != null) {
                    unloadQueue.addAll(toUnloadDependents);
                }
            }
        }

        public DependencyGraph copy(Map<Value<?>, Value<?>> valueRemapping) {
            return new DependencyGraph(this, valueRemapping);
        }
    }

    /**
     * A value stored in this attribute map.
     */
    private interface Value<T> extends SdkAutoCloseable {
        /**
         * Resolve the stored value using the provided value source.
         */
        T get(LazyValueSource source);

        /**
         * Copy this value, so that modifications like {@link #clearCache()} on this object do not affect the copy.
         */
        Value<T> copy();

        /**
         * If this value is cached, clear that cache.
         */
        void clearCache();

        /**
         * Read the cached value. This will return null if there is no value currently cached.
         */
        CachedValue<T> cachedValue();
    }

    /**
     * A constant (unchanging) {@link Value}.
     */
    private static class ConstantValue<T> implements Value<T> {
        private final T value;

        private ConstantValue(T value) {
            this.value = value;
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
        public CachedValue<T> cachedValue() {
            return new CachedValue<>(value);
        }

        @Override
        public void close() {
            closeIfPossible(value);
        }

        @Override
        public String toString() {
            return "Value(" + value + ")";
        }
    }

    /**
     * A value that is derived from other {@link Value}s.
     */
    private static final class DerivedValue<T> implements Value<T> {
        private final LazyValue<T> lazyValue;
        private boolean valueCached = false;
        private T value;

        private boolean onStack = false;

        private DerivedValue(LazyValue<T> lazyValue) {
            this.lazyValue = lazyValue;
        }

        private DerivedValue(LazyValue<T> lazyValue, boolean valueCached, T value) {
            this.lazyValue = lazyValue;
            this.valueCached = valueCached;
            this.value = value;
        }

        @Override
        public T get(LazyValueSource source) {
            primeCache(source);
            return value;
        }

        private void primeCache(LazyValueSource source) {
            if (!valueCached) {
                if (onStack) {
                    throw new IllegalStateException("Derived key attempted to read itself");
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
            return new DerivedValue<>(lazyValue, valueCached, value);
        }

        @Override
        public void clearCache() {
            valueCached = false;
        }

        @Override
        public CachedValue<T> cachedValue() {
            if (!valueCached) {
                return null;
            }
            return new CachedValue<>(value);
        }

        @Override
        public void close() {
            closeIfPossible(value);
        }

        @Override
        public String toString() {
            if (valueCached) {
                return "Value(" + value + ")";
            }
            return "Value(<<lazy>>)";
        }
    }

    private static class CachedValue<T> {
        private final T value;

        private CachedValue(T value) {
            this.value = value;
        }

        private static boolean haveSameCachedValues(CachedValue<?> lhs, CachedValue<?> rhs) {
            // If one is null, we can't guarantee that they have the same cached value.
            if (lhs == null || rhs == null) {
                return false;
            }

            return lhs.value == rhs.value;
        }
    }

    /**
     * An implementation of {@link LazyValueSource} that expects all values to be cached.
     */
    static class ExpectCachedLazyValueSource implements LazyValueSource {
        @Override
        public <T> T get(Key<T> sourceKey) {
            throw new IllegalStateException("Value should be cached.");
        }
    }

    private static void closeIfPossible(Object object) {
        // We're explicitly checking for whether the provided object is an ExecutorService instance, because as of
        // Java 21, it extends AutoCloseable, which triggers an ExecutorService#close call, which in turn can
        // result in deadlocks. Instead, we safely shut it down, and close any other objects that are closeable.
        if (object instanceof ExecutorService) {
            ((ExecutorService) object).shutdown();
        } else {
            IoUtils.closeIfCloseable(object, null);
        }
    }
}
