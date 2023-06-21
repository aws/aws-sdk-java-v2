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
 * Object containing expiration events options associated with an Amazon Web Services account.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class ExpiryEventsConfiguration implements SdkPojo, Serializable,
        ToCopyableBuilder<ExpiryEventsConfiguration.Builder, ExpiryEventsConfiguration> {
    private static final SdkField<Integer> DAYS_BEFORE_EXPIRY_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("DaysBeforeExpiry").getter(getter(ExpiryEventsConfiguration::daysBeforeExpiry))
            .setter(setter(Builder::daysBeforeExpiry))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DaysBeforeExpiry").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(DAYS_BEFORE_EXPIRY_FIELD));

    private static final long serialVersionUID = 1L;

    private final Integer daysBeforeExpiry;

    private ExpiryEventsConfiguration(BuilderImpl builder) {
        this.daysBeforeExpiry = builder.daysBeforeExpiry;
    }

    /**
     * <p>
     * Specifies the number of days prior to certificate expiration when ACM starts generating <code>EventBridge</code>
     * events. ACM sends one event per day per certificate until the certificate expires. By default, accounts receive
     * events starting 45 days before certificate expiration.
     * </p>
     * 
     * @return Specifies the number of days prior to certificate expiration when ACM starts generating
     *         <code>EventBridge</code> events. ACM sends one event per day per certificate until the certificate
     *         expires. By default, accounts receive events starting 45 days before certificate expiration.
     */
    public final Integer daysBeforeExpiry() {
        return daysBeforeExpiry;
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
        hashCode = 31 * hashCode + Objects.hashCode(daysBeforeExpiry());
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
        if (!(obj instanceof ExpiryEventsConfiguration)) {
            return false;
        }
        ExpiryEventsConfiguration other = (ExpiryEventsConfiguration) obj;
        return Objects.equals(daysBeforeExpiry(), other.daysBeforeExpiry());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ExpiryEventsConfiguration").add("DaysBeforeExpiry", daysBeforeExpiry()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "DaysBeforeExpiry":
            return Optional.ofNullable(clazz.cast(daysBeforeExpiry()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ExpiryEventsConfiguration, T> g) {
        return obj -> g.apply((ExpiryEventsConfiguration) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ExpiryEventsConfiguration> {
        /**
         * <p>
         * Specifies the number of days prior to certificate expiration when ACM starts generating
         * <code>EventBridge</code> events. ACM sends one event per day per certificate until the certificate expires.
         * By default, accounts receive events starting 45 days before certificate expiration.
         * </p>
         * 
         * @param daysBeforeExpiry
         *        Specifies the number of days prior to certificate expiration when ACM starts generating
         *        <code>EventBridge</code> events. ACM sends one event per day per certificate until the certificate
         *        expires. By default, accounts receive events starting 45 days before certificate expiration.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder daysBeforeExpiry(Integer daysBeforeExpiry);
    }

    static final class BuilderImpl implements Builder {
        private Integer daysBeforeExpiry;

        private BuilderImpl() {
        }

        private BuilderImpl(ExpiryEventsConfiguration model) {
            daysBeforeExpiry(model.daysBeforeExpiry);
        }

        public final Integer getDaysBeforeExpiry() {
            return daysBeforeExpiry;
        }

        public final void setDaysBeforeExpiry(Integer daysBeforeExpiry) {
            this.daysBeforeExpiry = daysBeforeExpiry;
        }

        @Override
        public final Builder daysBeforeExpiry(Integer daysBeforeExpiry) {
            this.daysBeforeExpiry = daysBeforeExpiry;
            return this;
        }

        @Override
        public ExpiryEventsConfiguration build() {
            return new ExpiryEventsConfiguration(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
