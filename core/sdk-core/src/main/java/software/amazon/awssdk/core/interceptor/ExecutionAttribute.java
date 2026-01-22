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

package software.amazon.awssdk.core.interceptor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An attribute attached to a particular execution, stored in {@link ExecutionAttributes}.
 *
 * This is typically used as a static final field in an {@link ExecutionInterceptor}:
 * <pre>
 * {@code
 *  class MyExecutionInterceptor implements ExecutionInterceptor {
 *      private static final ExecutionAttribute<String> DATA = new ExecutionAttribute<>();
 *
 *      public void beforeExecution(Context.BeforeExecution execution, ExecutionAttributes executionAttributes) {
 *          executionAttributes.put(DATA, "Request: " + execution.request());
 *      }
 *
 *      public void afterExecution(Context.AfterExecution execution, ExecutionAttributes executionAttributes) {
 *          String data = executionAttributes.get(DATA); // Retrieve the value saved in beforeExecution.
 *      }
 *  }
 * }
 </pre>
 *
 * @param <T> The type of data associated with this attribute.
 */
@SdkPublicApi
public final class ExecutionAttribute<T> {
    private static final ConcurrentMap<String, ExecutionAttribute<?>> NAME_HISTORY = new ConcurrentHashMap<>();
    
    private final String name;
    private final ValueStorage<T> storage;

    /**
     * Creates a new {@link ExecutionAttribute} bound to the provided type param.
     *
     * @param name Descriptive name for the attribute, used primarily for debugging purposes.
     */
    public ExecutionAttribute(String name) {
        this(name, null);
    }

    private ExecutionAttribute(String name, ValueStorage<T> storage) {
        this.name = name;
        this.storage = storage == null ?
                       new DefaultValueStorage() :
                       storage;
        ensureUnique();
    }

    /**
     * Create an execution attribute whose value is derived from another attribute.
     *
     * <p>Whenever this value is read, its value is read from a different "real" attribute, and whenever this value is written its
     * value is written to the "real" attribute, instead.
     *
     * <p>This is useful when new attributes are created to replace old attributes, but for backwards-compatibility those old
     * attributes still need to be made available.
     *
     * @param name The name of the attribute to create
     * @param attributeType The type of the attribute being created
     * @param realAttribute The "real" attribute from which this attribute is derived
     */
    public static <T, U> DerivedAttributeBuilder<T, U> derivedBuilder(String name,
                                                                      @SuppressWarnings("unused") Class<T> attributeType,
                                                                      ExecutionAttribute<U> realAttribute) {
        return new DerivedAttributeBuilder<>(name, () -> realAttribute);
    }

    /**
     * This is the same as {@link #derivedBuilder(String, Class, ExecutionAttribute)}, but the real attribute is loaded
     * lazily at runtime. This is useful when the real attribute is in the same class hierarchy, to avoid initialization
     * order problems.
     */
    public static <T, U> DerivedAttributeBuilder<T, U> derivedBuilder(String name,
                                                                      @SuppressWarnings("unused") Class<T> attributeType,
                                                                      Supplier<ExecutionAttribute<U>> realAttribute) {
        return new DerivedAttributeBuilder<>(name, realAttribute);
    }

    /**
     * Create an execution attribute whose value is backed by another attribute, and gets mapped to another execution attribute.
     *
     * <p>Whenever this value is read, its value is read from the backing attribute, but whenever this value is written its
     * value is written to the given attribute AND the mapped attribute.
     *
     * <p>This is useful when you have a complex attribute relationship, where certain attributes may depend on other attributes.
     *
     * @param name The name of the attribute to create
     * @param backingAttributeSupplier The supplier for the backing attribute, which this attribute is backed by
     * @param attributeSupplier The supplier for the attribute which is mapped from the backing attribute
     */
    static <T, U> MappedAttributeBuilder<T, U> mappedBuilder(String name,
                                                             Supplier<ExecutionAttribute<T>> backingAttributeSupplier,
                                                             Supplier<ExecutionAttribute<U>> attributeSupplier) {
        return new MappedAttributeBuilder<>(name, backingAttributeSupplier, attributeSupplier);
    }

    private void ensureUnique() {
        ExecutionAttribute<?> prev = NAME_HISTORY.putIfAbsent(name, this);
        if (prev != null) {
            throw new IllegalArgumentException(String.format("No duplicate ExecutionAttribute names allowed but both "
                                                             + "ExecutionAttributes %s and %s have the same name: %s. " 
                                                             + "ExecutionAttributes should be referenced from a shared static " 
                                                             + "constant to protect against erroneous or unexpected collisions.",
                                                             Integer.toHexString(System.identityHashCode(prev)),
                                                             Integer.toHexString(System.identityHashCode(this)),
                                                             name));
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * This override considers execution attributes with the same name
     * to be the same object for the purpose of attribute merge.
     * @return boolean indicating whether the objects are equal or not.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExecutionAttribute that = (ExecutionAttribute) o;
        return that.name.equals(this.name);
    }

    /**
     * This override considers execution attributes with the same name
     * to be the same object for the purpose of attribute merge.
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    /**
     * Visible for {@link ExecutionAttributes} to invoke when writing or reading values for this attribute.
     */
    ValueStorage<T> storage() {
        return storage;
    }

    public static final class DerivedAttributeBuilder<T, U> {
        private final String name;
        private final Supplier<ExecutionAttribute<U>> realAttribute;
        private Function<U, T> readMapping;
        private BiFunction<U, T, U> writeMapping;

        private DerivedAttributeBuilder(String name, Supplier<ExecutionAttribute<U>> realAttribute) {
            this.name = name;
            this.realAttribute = realAttribute;
        }

        /**
         * Set the "read" mapping for this derived attribute. The provided function accepts the current value of the
         * "real" attribute and returns the value of the derived attribute.
         */
        public DerivedAttributeBuilder<T, U> readMapping(Function<U, T> readMapping) {
            this.readMapping = readMapping;
            return this;
        }

        /**
         * Set the "write" mapping for this derived attribute. The provided function accepts the current value of the "real"
         * attribute, the value that we're trying to set to the derived attribute, and returns the value to set to the "real"
         * attribute.
         */
        public DerivedAttributeBuilder<T, U> writeMapping(BiFunction<U, T, U> writeMapping) {
            this.writeMapping = writeMapping;
            return this;
        }

        public ExecutionAttribute<T> build() {
            return new ExecutionAttribute<>(name, new DerivationValueStorage<>(this));
        }
    }

    /**
     * The value storage allows reading or writing values to this attribute. Used by {@link ExecutionAttributes} for storing
     * attribute values, whether they are "real" or derived.
     */
    interface ValueStorage<T> {
        /**
         * Retrieve an attribute's value from the provided attribute map.
         */
        T get(Map<ExecutionAttribute<?>, Object> attributes);

        /**
         * Set an attribute's value to the provided attribute map.
         */
        void set(Map<ExecutionAttribute<?>, Object> attributes, T value);

        /**
         * Set an attribute's value to the provided attribute map, if the value is not already in the map.
         */
        void setIfAbsent(Map<ExecutionAttribute<?>, Object> attributes, T value);
    }

    /**
     * An implementation of {@link ValueStorage} that stores the current execution attribute in the provided attributes map.
     */
    private final class DefaultValueStorage implements ValueStorage<T> {
        @SuppressWarnings("unchecked") // Safe because of the implementation of set()
        @Override
        public T get(Map<ExecutionAttribute<?>, Object> attributes) {
            return (T) attributes.get(ExecutionAttribute.this);
        }

        @Override
        public void set(Map<ExecutionAttribute<?>, Object> attributes, T value) {
            attributes.put(ExecutionAttribute.this, value);
        }

        @Override
        public void setIfAbsent(Map<ExecutionAttribute<?>, Object> attributes, T value) {
            attributes.putIfAbsent(ExecutionAttribute.this, value);
        }
    }

    /**
     * An implementation of {@link ValueStorage} that derives its value from a different execution attribute in the provided
     * attributes map.
     */
    private static final class DerivationValueStorage<T, U> implements ValueStorage<T> {
        private final Supplier<ExecutionAttribute<U>> realAttribute;
        private final Function<U, T> readMapping;
        private final BiFunction<U, T, U> writeMapping;

        private DerivationValueStorage(DerivedAttributeBuilder<T, U> builder) {
            this.realAttribute = Validate.paramNotNull(builder.realAttribute, "realAttribute");
            this.readMapping = Validate.paramNotNull(builder.readMapping, "readMapping");
            this.writeMapping = Validate.paramNotNull(builder.writeMapping, "writeMapping");
        }

        @SuppressWarnings("unchecked") // Safe because of the implementation of set
        @Override
        public T get(Map<ExecutionAttribute<?>, Object> attributes) {
            return readMapping.apply((U) attributes.get(realAttribute.get()));
        }

        @SuppressWarnings("unchecked") // Safe because of the implementation of set
        @Override
        public void set(Map<ExecutionAttribute<?>, Object> attributes, T value) {
            attributes.compute(realAttribute.get(), (k, real) -> writeMapping.apply((U) real, value));
        }

        @Override
        public void setIfAbsent(Map<ExecutionAttribute<?>, Object> attributes, T value) {
            T currentValue = get(attributes);
            if (currentValue == null) {
                set(attributes, value);
            }
        }
    }

    /**
     * An implementation of {@link ValueStorage} that is backed by a different execution attribute in the provided
     * attributes map (mirrors its value), and maps (updates) to another attribute.
     */
    private static final class MappedValueStorage<T, U> implements ValueStorage<T> {
        private final Supplier<ExecutionAttribute<T>> backingAttributeSupplier;
        private final Supplier<ExecutionAttribute<U>> attributeSupplier;
        private final BiFunction<T, U, T> readMapping;
        private final BiFunction<U, T, U> writeMapping;

        private MappedValueStorage(MappedAttributeBuilder<T, U> builder) {
            this.backingAttributeSupplier = Validate.paramNotNull(builder.backingAttributeSupplier, "backingAttributeSupplier");
            this.attributeSupplier = Validate.paramNotNull(builder.attributeSupplier, "attributeSupplier");
            this.readMapping = Validate.paramNotNull(builder.readMapping, "readMapping");
            this.writeMapping = Validate.paramNotNull(builder.writeMapping, "writeMapping");
        }

        @SuppressWarnings("unchecked") // Safe because of the implementation of set
        @Override
        public T get(Map<ExecutionAttribute<?>, Object> attributes) {
            return readMapping.apply(
                (T) attributes.get(backingAttributeSupplier.get()),
                (U) attributes.get(attributeSupplier.get())
            );
        }

        @SuppressWarnings("unchecked") // Safe because of the implementation of set
        @Override
        public void set(Map<ExecutionAttribute<?>, Object> attributes, T value) {
            attributes.put(backingAttributeSupplier.get(), value);
            attributes.compute(attributeSupplier.get(), (k, attr) -> writeMapping.apply((U) attr, value));
        }

        @Override
        public void setIfAbsent(Map<ExecutionAttribute<?>, Object> attributes, T value) {
            T currentValue = get(attributes);
            if (currentValue == null) {
                set(attributes, value);
            }
        }
    }

    protected static final class MappedAttributeBuilder<T, U> {
        private final String name;
        private final Supplier<ExecutionAttribute<T>> backingAttributeSupplier;
        private final Supplier<ExecutionAttribute<U>> attributeSupplier;
        private BiFunction<T, U, T> readMapping;
        private BiFunction<U, T, U> writeMapping;

        private MappedAttributeBuilder(String name, Supplier<ExecutionAttribute<T>> backingAttributeSupplier,
                                       Supplier<ExecutionAttribute<U>> attributeSupplier) {
            this.name = name;
            this.backingAttributeSupplier = backingAttributeSupplier;
            this.attributeSupplier = attributeSupplier;
        }

        /**
         * Set the "read" mapping for this mapped attribute. The provided function accepts the current value of the
         * backing attribute,
         */
        public MappedAttributeBuilder<T, U> readMapping(BiFunction<T, U, T> readMapping) {
            this.readMapping = readMapping;
            return this;
        }

        /**
         * Set the "write" mapping for this derived attribute. The provided function accepts the current value of the mapped
         * attribute, the value that we are mapping from (the "backing" attribute), and returns the value to set to the mapped
         * attribute.
         */
        public MappedAttributeBuilder<T, U> writeMapping(BiFunction<U, T, U> writeMapping) {
            this.writeMapping = writeMapping;
            return this;
        }

        public ExecutionAttribute<T> build() {
            return new ExecutionAttribute<>(name, new MappedValueStorage<>(this));
        }
    }
}
