/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <p>
 * This structure can be used in the <a>ListCertificates</a> action to filter the output of the certificate list.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class Filters implements SdkPojo, Serializable, ToCopyableBuilder<Filters.Builder, Filters> {
    private static final SdkField<List<String>> EXTENDED_KEY_USAGE_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("extendedKeyUsage")
            .getter(getter(Filters::extendedKeyUsageAsStrings))
            .setter(setter(Builder::extendedKeyUsageWithStrings))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("extendedKeyUsage").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<String>> KEY_USAGE_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("keyUsage")
            .getter(getter(Filters::keyUsageAsStrings))
            .setter(setter(Builder::keyUsageWithStrings))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("keyUsage").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<String>> KEY_TYPES_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("keyTypes")
            .getter(getter(Filters::keyTypesAsStrings))
            .setter(setter(Builder::keyTypesWithStrings))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("keyTypes").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(EXTENDED_KEY_USAGE_FIELD,
            KEY_USAGE_FIELD, KEY_TYPES_FIELD));

    private static final long serialVersionUID = 1L;

    private final List<String> extendedKeyUsage;

    private final List<String> keyUsage;

    private final List<String> keyTypes;

    private Filters(BuilderImpl builder) {
        this.extendedKeyUsage = builder.extendedKeyUsage;
        this.keyUsage = builder.keyUsage;
        this.keyTypes = builder.keyTypes;
    }

    /**
     * <p>
     * Specify one or more <a>ExtendedKeyUsage</a> extension values.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasExtendedKeyUsage} method.
     * </p>
     * 
     * @return Specify one or more <a>ExtendedKeyUsage</a> extension values.
     */
    public final List<ExtendedKeyUsageName> extendedKeyUsage() {
        return ExtendedKeyUsageFilterListCopier.copyStringToEnum(extendedKeyUsage);
    }

    /**
     * For responses, this returns true if the service returned a value for the ExtendedKeyUsage property. This DOES NOT
     * check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasExtendedKeyUsage() {
        return extendedKeyUsage != null && !(extendedKeyUsage instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Specify one or more <a>ExtendedKeyUsage</a> extension values.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasExtendedKeyUsage} method.
     * </p>
     * 
     * @return Specify one or more <a>ExtendedKeyUsage</a> extension values.
     */
    public final List<String> extendedKeyUsageAsStrings() {
        return extendedKeyUsage;
    }

    /**
     * <p>
     * Specify one or more <a>KeyUsage</a> extension values.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasKeyUsage} method.
     * </p>
     * 
     * @return Specify one or more <a>KeyUsage</a> extension values.
     */
    public final List<KeyUsageName> keyUsage() {
        return KeyUsageFilterListCopier.copyStringToEnum(keyUsage);
    }

    /**
     * For responses, this returns true if the service returned a value for the KeyUsage property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasKeyUsage() {
        return keyUsage != null && !(keyUsage instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Specify one or more <a>KeyUsage</a> extension values.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasKeyUsage} method.
     * </p>
     * 
     * @return Specify one or more <a>KeyUsage</a> extension values.
     */
    public final List<String> keyUsageAsStrings() {
        return keyUsage;
    }

    /**
     * <p>
     * Specify one or more algorithms that can be used to generate key pairs.
     * </p>
     * <p>
     * Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at least
     * one domain. To return other certificate types, provide the desired type signatures in a comma-separated list. For
     * example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both <code>RSA_2048</code> and
     * <code>RSA_4096</code> certificates.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasKeyTypes} method.
     * </p>
     * 
     * @return Specify one or more algorithms that can be used to generate key pairs.</p>
     *         <p>
     *         Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at
     *         least one domain. To return other certificate types, provide the desired type signatures in a
     *         comma-separated list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both
     *         <code>RSA_2048</code> and <code>RSA_4096</code> certificates.
     */
    public final List<KeyAlgorithm> keyTypes() {
        return KeyAlgorithmListCopier.copyStringToEnum(keyTypes);
    }

    /**
     * For responses, this returns true if the service returned a value for the KeyTypes property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasKeyTypes() {
        return keyTypes != null && !(keyTypes instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Specify one or more algorithms that can be used to generate key pairs.
     * </p>
     * <p>
     * Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at least
     * one domain. To return other certificate types, provide the desired type signatures in a comma-separated list. For
     * example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both <code>RSA_2048</code> and
     * <code>RSA_4096</code> certificates.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasKeyTypes} method.
     * </p>
     * 
     * @return Specify one or more algorithms that can be used to generate key pairs.</p>
     *         <p>
     *         Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at
     *         least one domain. To return other certificate types, provide the desired type signatures in a
     *         comma-separated list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both
     *         <code>RSA_2048</code> and <code>RSA_4096</code> certificates.
     */
    public final List<String> keyTypesAsStrings() {
        return keyTypes;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(hasExtendedKeyUsage() ? extendedKeyUsageAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasKeyUsage() ? keyUsageAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasKeyTypes() ? keyTypesAsStrings() : null);
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Filters)) {
            return false;
        }
        Filters other = (Filters) obj;
        return hasExtendedKeyUsage() == other.hasExtendedKeyUsage()
                && Objects.equals(extendedKeyUsageAsStrings(), other.extendedKeyUsageAsStrings())
                && hasKeyUsage() == other.hasKeyUsage() && Objects.equals(keyUsageAsStrings(), other.keyUsageAsStrings())
                && hasKeyTypes() == other.hasKeyTypes() && Objects.equals(keyTypesAsStrings(), other.keyTypesAsStrings());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("Filters").add("ExtendedKeyUsage", hasExtendedKeyUsage() ? extendedKeyUsageAsStrings() : null)
                .add("KeyUsage", hasKeyUsage() ? keyUsageAsStrings() : null)
                .add("KeyTypes", hasKeyTypes() ? keyTypesAsStrings() : null).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "extendedKeyUsage":
            return Optional.ofNullable(clazz.cast(extendedKeyUsageAsStrings()));
        case "keyUsage":
            return Optional.ofNullable(clazz.cast(keyUsageAsStrings()));
        case "keyTypes":
            return Optional.ofNullable(clazz.cast(keyTypesAsStrings()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<Filters, T> g) {
        return obj -> g.apply((Filters) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, Filters> {
        /**
         * <p>
         * Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * </p>
         * 
         * @param extendedKeyUsage
         *        Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder extendedKeyUsageWithStrings(Collection<String> extendedKeyUsage);

        /**
         * <p>
         * Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * </p>
         * 
         * @param extendedKeyUsage
         *        Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder extendedKeyUsageWithStrings(String... extendedKeyUsage);

        /**
         * <p>
         * Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * </p>
         * 
         * @param extendedKeyUsage
         *        Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder extendedKeyUsage(Collection<ExtendedKeyUsageName> extendedKeyUsage);

        /**
         * <p>
         * Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * </p>
         * 
         * @param extendedKeyUsage
         *        Specify one or more <a>ExtendedKeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder extendedKeyUsage(ExtendedKeyUsageName... extendedKeyUsage);

        /**
         * <p>
         * Specify one or more <a>KeyUsage</a> extension values.
         * </p>
         * 
         * @param keyUsage
         *        Specify one or more <a>KeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyUsageWithStrings(Collection<String> keyUsage);

        /**
         * <p>
         * Specify one or more <a>KeyUsage</a> extension values.
         * </p>
         * 
         * @param keyUsage
         *        Specify one or more <a>KeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyUsageWithStrings(String... keyUsage);

        /**
         * <p>
         * Specify one or more <a>KeyUsage</a> extension values.
         * </p>
         * 
         * @param keyUsage
         *        Specify one or more <a>KeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyUsage(Collection<KeyUsageName> keyUsage);

        /**
         * <p>
         * Specify one or more <a>KeyUsage</a> extension values.
         * </p>
         * 
         * @param keyUsage
         *        Specify one or more <a>KeyUsage</a> extension values.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyUsage(KeyUsageName... keyUsage);

        /**
         * <p>
         * Specify one or more algorithms that can be used to generate key pairs.
         * </p>
         * <p>
         * Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at
         * least one domain. To return other certificate types, provide the desired type signatures in a comma-separated
         * list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both <code>RSA_2048</code> and
         * <code>RSA_4096</code> certificates.
         * </p>
         * 
         * @param keyTypes
         *        Specify one or more algorithms that can be used to generate key pairs.</p>
         *        <p>
         *        Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have
         *        at least one domain. To return other certificate types, provide the desired type signatures in a
         *        comma-separated list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both
         *        <code>RSA_2048</code> and <code>RSA_4096</code> certificates.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyTypesWithStrings(Collection<String> keyTypes);

        /**
         * <p>
         * Specify one or more algorithms that can be used to generate key pairs.
         * </p>
         * <p>
         * Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at
         * least one domain. To return other certificate types, provide the desired type signatures in a comma-separated
         * list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both <code>RSA_2048</code> and
         * <code>RSA_4096</code> certificates.
         * </p>
         * 
         * @param keyTypes
         *        Specify one or more algorithms that can be used to generate key pairs.</p>
         *        <p>
         *        Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have
         *        at least one domain. To return other certificate types, provide the desired type signatures in a
         *        comma-separated list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both
         *        <code>RSA_2048</code> and <code>RSA_4096</code> certificates.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyTypesWithStrings(String... keyTypes);

        /**
         * <p>
         * Specify one or more algorithms that can be used to generate key pairs.
         * </p>
         * <p>
         * Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at
         * least one domain. To return other certificate types, provide the desired type signatures in a comma-separated
         * list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both <code>RSA_2048</code> and
         * <code>RSA_4096</code> certificates.
         * </p>
         * 
         * @param keyTypes
         *        Specify one or more algorithms that can be used to generate key pairs.</p>
         *        <p>
         *        Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have
         *        at least one domain. To return other certificate types, provide the desired type signatures in a
         *        comma-separated list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both
         *        <code>RSA_2048</code> and <code>RSA_4096</code> certificates.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyTypes(Collection<KeyAlgorithm> keyTypes);

        /**
         * <p>
         * Specify one or more algorithms that can be used to generate key pairs.
         * </p>
         * <p>
         * Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have at
         * least one domain. To return other certificate types, provide the desired type signatures in a comma-separated
         * list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both <code>RSA_2048</code> and
         * <code>RSA_4096</code> certificates.
         * </p>
         * 
         * @param keyTypes
         *        Specify one or more algorithms that can be used to generate key pairs.</p>
         *        <p>
         *        Default filtering returns only <code>RSA_1024</code> and <code>RSA_2048</code> certificates that have
         *        at least one domain. To return other certificate types, provide the desired type signatures in a
         *        comma-separated list. For example, <code>"keyTypes": ["RSA_2048","RSA_4096"]</code> returns both
         *        <code>RSA_2048</code> and <code>RSA_4096</code> certificates.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyTypes(KeyAlgorithm... keyTypes);
    }

    static final class BuilderImpl implements Builder {
        private List<String> extendedKeyUsage = DefaultSdkAutoConstructList.getInstance();

        private List<String> keyUsage = DefaultSdkAutoConstructList.getInstance();

        private List<String> keyTypes = DefaultSdkAutoConstructList.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(Filters model) {
            extendedKeyUsageWithStrings(model.extendedKeyUsage);
            keyUsageWithStrings(model.keyUsage);
            keyTypesWithStrings(model.keyTypes);
        }

        public final Collection<String> getExtendedKeyUsage() {
            if (extendedKeyUsage instanceof SdkAutoConstructList) {
                return null;
            }
            return extendedKeyUsage;
        }

        public final void setExtendedKeyUsage(Collection<String> extendedKeyUsage) {
            this.extendedKeyUsage = ExtendedKeyUsageFilterListCopier.copy(extendedKeyUsage);
        }

        @Override
        public final Builder extendedKeyUsageWithStrings(Collection<String> extendedKeyUsage) {
            this.extendedKeyUsage = ExtendedKeyUsageFilterListCopier.copy(extendedKeyUsage);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder extendedKeyUsageWithStrings(String... extendedKeyUsage) {
            extendedKeyUsageWithStrings(Arrays.asList(extendedKeyUsage));
            return this;
        }

        @Override
        public final Builder extendedKeyUsage(Collection<ExtendedKeyUsageName> extendedKeyUsage) {
            this.extendedKeyUsage = ExtendedKeyUsageFilterListCopier.copyEnumToString(extendedKeyUsage);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder extendedKeyUsage(ExtendedKeyUsageName... extendedKeyUsage) {
            extendedKeyUsage(Arrays.asList(extendedKeyUsage));
            return this;
        }

        public final Collection<String> getKeyUsage() {
            if (keyUsage instanceof SdkAutoConstructList) {
                return null;
            }
            return keyUsage;
        }

        public final void setKeyUsage(Collection<String> keyUsage) {
            this.keyUsage = KeyUsageFilterListCopier.copy(keyUsage);
        }

        @Override
        public final Builder keyUsageWithStrings(Collection<String> keyUsage) {
            this.keyUsage = KeyUsageFilterListCopier.copy(keyUsage);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyUsageWithStrings(String... keyUsage) {
            keyUsageWithStrings(Arrays.asList(keyUsage));
            return this;
        }

        @Override
        public final Builder keyUsage(Collection<KeyUsageName> keyUsage) {
            this.keyUsage = KeyUsageFilterListCopier.copyEnumToString(keyUsage);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyUsage(KeyUsageName... keyUsage) {
            keyUsage(Arrays.asList(keyUsage));
            return this;
        }

        public final Collection<String> getKeyTypes() {
            if (keyTypes instanceof SdkAutoConstructList) {
                return null;
            }
            return keyTypes;
        }

        public final void setKeyTypes(Collection<String> keyTypes) {
            this.keyTypes = KeyAlgorithmListCopier.copy(keyTypes);
        }

        @Override
        public final Builder keyTypesWithStrings(Collection<String> keyTypes) {
            this.keyTypes = KeyAlgorithmListCopier.copy(keyTypes);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyTypesWithStrings(String... keyTypes) {
            keyTypesWithStrings(Arrays.asList(keyTypes));
            return this;
        }

        @Override
        public final Builder keyTypes(Collection<KeyAlgorithm> keyTypes) {
            this.keyTypes = KeyAlgorithmListCopier.copyEnumToString(keyTypes);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyTypes(KeyAlgorithm... keyTypes) {
            keyTypes(Arrays.asList(keyTypes));
            return this;
        }

        @Override
        public Filters build() {
            return new Filters(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
