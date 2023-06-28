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
 * Contains information about the domain names that you want ACM to use to send you emails that enable you to validate
 * domain ownership.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class DomainValidationOption implements SdkPojo, Serializable,
        ToCopyableBuilder<DomainValidationOption.Builder, DomainValidationOption> {
    private static final SdkField<String> DOMAIN_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("DomainName").getter(getter(DomainValidationOption::domainName)).setter(setter(Builder::domainName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DomainName").build()).build();

    private static final SdkField<String> VALIDATION_DOMAIN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("ValidationDomain").getter(getter(DomainValidationOption::validationDomain))
            .setter(setter(Builder::validationDomain))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ValidationDomain").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(DOMAIN_NAME_FIELD,
            VALIDATION_DOMAIN_FIELD));

    private static final long serialVersionUID = 1L;

    private final String domainName;

    private final String validationDomain;

    private DomainValidationOption(BuilderImpl builder) {
        this.domainName = builder.domainName;
        this.validationDomain = builder.validationDomain;
    }

    /**
     * <p>
     * A fully qualified domain name (FQDN) in the certificate request.
     * </p>
     * 
     * @return A fully qualified domain name (FQDN) in the certificate request.
     */
    public final String domainName() {
        return domainName;
    }

    /**
     * <p>
     * The domain name that you want ACM to use to send you validation emails. This domain name is the suffix of the
     * email addresses that you want ACM to use. This must be the same as the <code>DomainName</code> value or a
     * superdomain of the <code>DomainName</code> value. For example, if you request a certificate for
     * <code>testing.example.com</code>, you can specify <code>example.com</code> for this value. In that case, ACM
     * sends domain validation emails to the following five addresses:
     * </p>
     * <ul>
     * <li>
     * <p>
     * admin@example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * administrator@example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * hostmaster@example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * postmaster@example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * webmaster@example.com
     * </p>
     * </li>
     * </ul>
     * 
     * @return The domain name that you want ACM to use to send you validation emails. This domain name is the suffix of
     *         the email addresses that you want ACM to use. This must be the same as the <code>DomainName</code> value
     *         or a superdomain of the <code>DomainName</code> value. For example, if you request a certificate for
     *         <code>testing.example.com</code>, you can specify <code>example.com</code> for this value. In that case,
     *         ACM sends domain validation emails to the following five addresses:</p>
     *         <ul>
     *         <li>
     *         <p>
     *         admin@example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         administrator@example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         hostmaster@example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         postmaster@example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         webmaster@example.com
     *         </p>
     *         </li>
     */
    public final String validationDomain() {
        return validationDomain;
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
        hashCode = 31 * hashCode + Objects.hashCode(domainName());
        hashCode = 31 * hashCode + Objects.hashCode(validationDomain());
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
        if (!(obj instanceof DomainValidationOption)) {
            return false;
        }
        DomainValidationOption other = (DomainValidationOption) obj;
        return Objects.equals(domainName(), other.domainName()) && Objects.equals(validationDomain(), other.validationDomain());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DomainValidationOption").add("DomainName", domainName())
                .add("ValidationDomain", validationDomain()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "DomainName":
            return Optional.ofNullable(clazz.cast(domainName()));
        case "ValidationDomain":
            return Optional.ofNullable(clazz.cast(validationDomain()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DomainValidationOption, T> g) {
        return obj -> g.apply((DomainValidationOption) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, DomainValidationOption> {
        /**
         * <p>
         * A fully qualified domain name (FQDN) in the certificate request.
         * </p>
         * 
         * @param domainName
         *        A fully qualified domain name (FQDN) in the certificate request.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainName(String domainName);

        /**
         * <p>
         * The domain name that you want ACM to use to send you validation emails. This domain name is the suffix of the
         * email addresses that you want ACM to use. This must be the same as the <code>DomainName</code> value or a
         * superdomain of the <code>DomainName</code> value. For example, if you request a certificate for
         * <code>testing.example.com</code>, you can specify <code>example.com</code> for this value. In that case, ACM
         * sends domain validation emails to the following five addresses:
         * </p>
         * <ul>
         * <li>
         * <p>
         * admin@example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * administrator@example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * hostmaster@example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * postmaster@example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * webmaster@example.com
         * </p>
         * </li>
         * </ul>
         * 
         * @param validationDomain
         *        The domain name that you want ACM to use to send you validation emails. This domain name is the suffix
         *        of the email addresses that you want ACM to use. This must be the same as the <code>DomainName</code>
         *        value or a superdomain of the <code>DomainName</code> value. For example, if you request a certificate
         *        for <code>testing.example.com</code>, you can specify <code>example.com</code> for this value. In that
         *        case, ACM sends domain validation emails to the following five addresses:</p>
         *        <ul>
         *        <li>
         *        <p>
         *        admin@example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        administrator@example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        hostmaster@example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        postmaster@example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        webmaster@example.com
         *        </p>
         *        </li>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder validationDomain(String validationDomain);
    }

    static final class BuilderImpl implements Builder {
        private String domainName;

        private String validationDomain;

        private BuilderImpl() {
        }

        private BuilderImpl(DomainValidationOption model) {
            domainName(model.domainName);
            validationDomain(model.validationDomain);
        }

        public final String getDomainName() {
            return domainName;
        }

        public final void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        @Override
        public final Builder domainName(String domainName) {
            this.domainName = domainName;
            return this;
        }

        public final String getValidationDomain() {
            return validationDomain;
        }

        public final void setValidationDomain(String validationDomain) {
            this.validationDomain = validationDomain;
        }

        @Override
        public final Builder validationDomain(String validationDomain) {
            this.validationDomain = validationDomain;
            return this;
        }

        @Override
        public DomainValidationOption build() {
            return new DomainValidationOption(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
