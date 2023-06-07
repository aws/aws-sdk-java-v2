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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class PutAccountConfigurationRequest extends AcmRequest implements
        ToCopyableBuilder<PutAccountConfigurationRequest.Builder, PutAccountConfigurationRequest> {
    private static final SdkField<ExpiryEventsConfiguration> EXPIRY_EVENTS_FIELD = SdkField
            .<ExpiryEventsConfiguration> builder(MarshallingType.SDK_POJO).memberName("ExpiryEvents")
            .getter(getter(PutAccountConfigurationRequest::expiryEvents)).setter(setter(Builder::expiryEvents))
            .constructor(ExpiryEventsConfiguration::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ExpiryEvents").build()).build();

    private static final SdkField<String> IDEMPOTENCY_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("IdempotencyToken").getter(getter(PutAccountConfigurationRequest::idempotencyToken))
            .setter(setter(Builder::idempotencyToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("IdempotencyToken").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(EXPIRY_EVENTS_FIELD,
            IDEMPOTENCY_TOKEN_FIELD));

    private final ExpiryEventsConfiguration expiryEvents;

    private final String idempotencyToken;

    private PutAccountConfigurationRequest(BuilderImpl builder) {
        super(builder);
        this.expiryEvents = builder.expiryEvents;
        this.idempotencyToken = builder.idempotencyToken;
    }

    /**
     * <p>
     * Specifies expiration events associated with an account.
     * </p>
     * 
     * @return Specifies expiration events associated with an account.
     */
    public final ExpiryEventsConfiguration expiryEvents() {
        return expiryEvents;
    }

    /**
     * <p>
     * Customer-chosen string used to distinguish between calls to <code>PutAccountConfiguration</code>. Idempotency
     * tokens time out after one hour. If you call <code>PutAccountConfiguration</code> multiple times with the same
     * unexpired idempotency token, ACM treats it as the same request and returns the original result. If you change the
     * idempotency token for each call, ACM treats each call as a new request.
     * </p>
     * 
     * @return Customer-chosen string used to distinguish between calls to <code>PutAccountConfiguration</code>.
     *         Idempotency tokens time out after one hour. If you call <code>PutAccountConfiguration</code> multiple
     *         times with the same unexpired idempotency token, ACM treats it as the same request and returns the
     *         original result. If you change the idempotency token for each call, ACM treats each call as a new
     *         request.
     */
    public final String idempotencyToken() {
        return idempotencyToken;
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
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(expiryEvents());
        hashCode = 31 * hashCode + Objects.hashCode(idempotencyToken());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PutAccountConfigurationRequest)) {
            return false;
        }
        PutAccountConfigurationRequest other = (PutAccountConfigurationRequest) obj;
        return Objects.equals(expiryEvents(), other.expiryEvents())
                && Objects.equals(idempotencyToken(), other.idempotencyToken());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("PutAccountConfigurationRequest").add("ExpiryEvents", expiryEvents())
                .add("IdempotencyToken", idempotencyToken()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "ExpiryEvents":
            return Optional.ofNullable(clazz.cast(expiryEvents()));
        case "IdempotencyToken":
            return Optional.ofNullable(clazz.cast(idempotencyToken()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<PutAccountConfigurationRequest, T> g) {
        return obj -> g.apply((PutAccountConfigurationRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmRequest.Builder, SdkPojo, CopyableBuilder<Builder, PutAccountConfigurationRequest> {
        /**
         * <p>
         * Specifies expiration events associated with an account.
         * </p>
         * 
         * @param expiryEvents
         *        Specifies expiration events associated with an account.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder expiryEvents(ExpiryEventsConfiguration expiryEvents);

        /**
         * <p>
         * Specifies expiration events associated with an account.
         * </p>
         * This is a convenience method that creates an instance of the {@link ExpiryEventsConfiguration.Builder}
         * avoiding the need to create one manually via {@link ExpiryEventsConfiguration#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link ExpiryEventsConfiguration.Builder#build()} is called immediately
         * and its result is passed to {@link #expiryEvents(ExpiryEventsConfiguration)}.
         * 
         * @param expiryEvents
         *        a consumer that will call methods on {@link ExpiryEventsConfiguration.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #expiryEvents(ExpiryEventsConfiguration)
         */
        default Builder expiryEvents(Consumer<ExpiryEventsConfiguration.Builder> expiryEvents) {
            return expiryEvents(ExpiryEventsConfiguration.builder().applyMutation(expiryEvents).build());
        }

        /**
         * <p>
         * Customer-chosen string used to distinguish between calls to <code>PutAccountConfiguration</code>. Idempotency
         * tokens time out after one hour. If you call <code>PutAccountConfiguration</code> multiple times with the same
         * unexpired idempotency token, ACM treats it as the same request and returns the original result. If you change
         * the idempotency token for each call, ACM treats each call as a new request.
         * </p>
         * 
         * @param idempotencyToken
         *        Customer-chosen string used to distinguish between calls to <code>PutAccountConfiguration</code>.
         *        Idempotency tokens time out after one hour. If you call <code>PutAccountConfiguration</code> multiple
         *        times with the same unexpired idempotency token, ACM treats it as the same request and returns the
         *        original result. If you change the idempotency token for each call, ACM treats each call as a new
         *        request.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder idempotencyToken(String idempotencyToken);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends AcmRequest.BuilderImpl implements Builder {
        private ExpiryEventsConfiguration expiryEvents;

        private String idempotencyToken;

        private BuilderImpl() {
        }

        private BuilderImpl(PutAccountConfigurationRequest model) {
            super(model);
            expiryEvents(model.expiryEvents);
            idempotencyToken(model.idempotencyToken);
        }

        public final ExpiryEventsConfiguration.Builder getExpiryEvents() {
            return expiryEvents != null ? expiryEvents.toBuilder() : null;
        }

        public final void setExpiryEvents(ExpiryEventsConfiguration.BuilderImpl expiryEvents) {
            this.expiryEvents = expiryEvents != null ? expiryEvents.build() : null;
        }

        @Override
        public final Builder expiryEvents(ExpiryEventsConfiguration expiryEvents) {
            this.expiryEvents = expiryEvents;
            return this;
        }

        public final String getIdempotencyToken() {
            return idempotencyToken;
        }

        public final void setIdempotencyToken(String idempotencyToken) {
            this.idempotencyToken = idempotencyToken;
        }

        @Override
        public final Builder idempotencyToken(String idempotencyToken) {
            this.idempotencyToken = idempotencyToken;
            return this;
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public PutAccountConfigurationRequest build() {
            return new PutAccountConfigurationRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
