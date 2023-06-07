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
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <p>
 * The Extended Key Usage X.509 v3 extension defines one or more purposes for which the public key can be used. This is
 * in addition to or in place of the basic purposes specified by the Key Usage extension.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class ExtendedKeyUsage implements SdkPojo, Serializable,
        ToCopyableBuilder<ExtendedKeyUsage.Builder, ExtendedKeyUsage> {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Name")
            .getter(getter(ExtendedKeyUsage::nameAsString)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Name").build()).build();

    private static final SdkField<String> OID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("OID")
            .getter(getter(ExtendedKeyUsage::oid)).setter(setter(Builder::oid))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OID").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD, OID_FIELD));

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String oid;

    private ExtendedKeyUsage(BuilderImpl builder) {
        this.name = builder.name;
        this.oid = builder.oid;
    }

    /**
     * <p>
     * The name of an Extended Key Usage value.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #name} will return
     * {@link ExtendedKeyUsageName#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #nameAsString}.
     * </p>
     * 
     * @return The name of an Extended Key Usage value.
     * @see ExtendedKeyUsageName
     */
    public final ExtendedKeyUsageName name() {
        return ExtendedKeyUsageName.fromValue(name);
    }

    /**
     * <p>
     * The name of an Extended Key Usage value.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #name} will return
     * {@link ExtendedKeyUsageName#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #nameAsString}.
     * </p>
     * 
     * @return The name of an Extended Key Usage value.
     * @see ExtendedKeyUsageName
     */
    public final String nameAsString() {
        return name;
    }

    /**
     * <p>
     * An object identifier (OID) for the extension value. OIDs are strings of numbers separated by periods. The
     * following OIDs are defined in RFC 3280 and RFC 5280.
     * </p>
     * <ul>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.1 (TLS_WEB_SERVER_AUTHENTICATION)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.2 (TLS_WEB_CLIENT_AUTHENTICATION)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.3 (CODE_SIGNING)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.4 (EMAIL_PROTECTION)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.8 (TIME_STAMPING)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.9 (OCSP_SIGNING)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.5 (IPSEC_END_SYSTEM)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.6 (IPSEC_TUNNEL)</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>1.3.6.1.5.5.7.3.7 (IPSEC_USER)</code>
     * </p>
     * </li>
     * </ul>
     * 
     * @return An object identifier (OID) for the extension value. OIDs are strings of numbers separated by periods. The
     *         following OIDs are defined in RFC 3280 and RFC 5280. </p>
     *         <ul>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.1 (TLS_WEB_SERVER_AUTHENTICATION)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.2 (TLS_WEB_CLIENT_AUTHENTICATION)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.3 (CODE_SIGNING)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.4 (EMAIL_PROTECTION)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.8 (TIME_STAMPING)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.9 (OCSP_SIGNING)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.5 (IPSEC_END_SYSTEM)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.6 (IPSEC_TUNNEL)</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>1.3.6.1.5.5.7.3.7 (IPSEC_USER)</code>
     *         </p>
     *         </li>
     */
    public final String oid() {
        return oid;
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
        hashCode = 31 * hashCode + Objects.hashCode(nameAsString());
        hashCode = 31 * hashCode + Objects.hashCode(oid());
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
        if (!(obj instanceof ExtendedKeyUsage)) {
            return false;
        }
        ExtendedKeyUsage other = (ExtendedKeyUsage) obj;
        return Objects.equals(nameAsString(), other.nameAsString()) && Objects.equals(oid(), other.oid());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ExtendedKeyUsage").add("Name", nameAsString()).add("OID", oid()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Name":
            return Optional.ofNullable(clazz.cast(nameAsString()));
        case "OID":
            return Optional.ofNullable(clazz.cast(oid()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ExtendedKeyUsage, T> g) {
        return obj -> g.apply((ExtendedKeyUsage) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ExtendedKeyUsage> {
        /**
         * <p>
         * The name of an Extended Key Usage value.
         * </p>
         * 
         * @param name
         *        The name of an Extended Key Usage value.
         * @see ExtendedKeyUsageName
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see ExtendedKeyUsageName
         */
        Builder name(String name);

        /**
         * <p>
         * The name of an Extended Key Usage value.
         * </p>
         * 
         * @param name
         *        The name of an Extended Key Usage value.
         * @see ExtendedKeyUsageName
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see ExtendedKeyUsageName
         */
        Builder name(ExtendedKeyUsageName name);

        /**
         * <p>
         * An object identifier (OID) for the extension value. OIDs are strings of numbers separated by periods. The
         * following OIDs are defined in RFC 3280 and RFC 5280.
         * </p>
         * <ul>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.1 (TLS_WEB_SERVER_AUTHENTICATION)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.2 (TLS_WEB_CLIENT_AUTHENTICATION)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.3 (CODE_SIGNING)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.4 (EMAIL_PROTECTION)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.8 (TIME_STAMPING)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.9 (OCSP_SIGNING)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.5 (IPSEC_END_SYSTEM)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.6 (IPSEC_TUNNEL)</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>1.3.6.1.5.5.7.3.7 (IPSEC_USER)</code>
         * </p>
         * </li>
         * </ul>
         * 
         * @param oid
         *        An object identifier (OID) for the extension value. OIDs are strings of numbers separated by periods.
         *        The following OIDs are defined in RFC 3280 and RFC 5280. </p>
         *        <ul>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.1 (TLS_WEB_SERVER_AUTHENTICATION)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.2 (TLS_WEB_CLIENT_AUTHENTICATION)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.3 (CODE_SIGNING)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.4 (EMAIL_PROTECTION)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.8 (TIME_STAMPING)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.9 (OCSP_SIGNING)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.5 (IPSEC_END_SYSTEM)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.6 (IPSEC_TUNNEL)</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>1.3.6.1.5.5.7.3.7 (IPSEC_USER)</code>
         *        </p>
         *        </li>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder oid(String oid);
    }

    static final class BuilderImpl implements Builder {
        private String name;

        private String oid;

        private BuilderImpl() {
        }

        private BuilderImpl(ExtendedKeyUsage model) {
            name(model.name);
            oid(model.oid);
        }

        public final String getName() {
            return name;
        }

        public final void setName(String name) {
            this.name = name;
        }

        @Override
        public final Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public final Builder name(ExtendedKeyUsageName name) {
            this.name(name == null ? null : name.toString());
            return this;
        }

        public final String getOid() {
            return oid;
        }

        public final void setOid(String oid) {
            this.oid = oid;
        }

        @Override
        public final Builder oid(String oid) {
            this.oid = oid;
            return this;
        }

        @Override
        public ExtendedKeyUsage build() {
            return new ExtendedKeyUsage(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
