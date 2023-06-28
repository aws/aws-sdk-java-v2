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

package software.amazon.awssdk.services.codecatalyst.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
 * Information about the configuration of a Dev Environment session.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class DevEnvironmentSessionConfiguration implements SdkPojo, Serializable,
        ToCopyableBuilder<DevEnvironmentSessionConfiguration.Builder, DevEnvironmentSessionConfiguration> {
    private static final SdkField<String> SESSION_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("sessionType").getter(getter(DevEnvironmentSessionConfiguration::sessionTypeAsString))
            .setter(setter(Builder::sessionType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("sessionType").build()).build();

    private static final SdkField<ExecuteCommandSessionConfiguration> EXECUTE_COMMAND_SESSION_CONFIGURATION_FIELD = SdkField
            .<ExecuteCommandSessionConfiguration> builder(MarshallingType.SDK_POJO)
            .memberName("executeCommandSessionConfiguration")
            .getter(getter(DevEnvironmentSessionConfiguration::executeCommandSessionConfiguration))
            .setter(setter(Builder::executeCommandSessionConfiguration))
            .constructor(ExecuteCommandSessionConfiguration::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("executeCommandSessionConfiguration")
                    .build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SESSION_TYPE_FIELD,
            EXECUTE_COMMAND_SESSION_CONFIGURATION_FIELD));

    private static final long serialVersionUID = 1L;

    private final String sessionType;

    private final ExecuteCommandSessionConfiguration executeCommandSessionConfiguration;

    private DevEnvironmentSessionConfiguration(BuilderImpl builder) {
        this.sessionType = builder.sessionType;
        this.executeCommandSessionConfiguration = builder.executeCommandSessionConfiguration;
    }

    /**
     * <p>
     * The type of the session.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sessionType} will
     * return {@link DevEnvironmentSessionType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is
     * available from {@link #sessionTypeAsString}.
     * </p>
     * 
     * @return The type of the session.
     * @see DevEnvironmentSessionType
     */
    public final DevEnvironmentSessionType sessionType() {
        return DevEnvironmentSessionType.fromValue(sessionType);
    }

    /**
     * <p>
     * The type of the session.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sessionType} will
     * return {@link DevEnvironmentSessionType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is
     * available from {@link #sessionTypeAsString}.
     * </p>
     * 
     * @return The type of the session.
     * @see DevEnvironmentSessionType
     */
    public final String sessionTypeAsString() {
        return sessionType;
    }

    /**
     * <p>
     * Information about optional commands that will be run on the Dev Environment when the SSH session begins.
     * </p>
     * 
     * @return Information about optional commands that will be run on the Dev Environment when the SSH session begins.
     */
    public final ExecuteCommandSessionConfiguration executeCommandSessionConfiguration() {
        return executeCommandSessionConfiguration;
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
        hashCode = 31 * hashCode + Objects.hashCode(sessionTypeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(executeCommandSessionConfiguration());
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
        if (!(obj instanceof DevEnvironmentSessionConfiguration)) {
            return false;
        }
        DevEnvironmentSessionConfiguration other = (DevEnvironmentSessionConfiguration) obj;
        return Objects.equals(sessionTypeAsString(), other.sessionTypeAsString())
                && Objects.equals(executeCommandSessionConfiguration(), other.executeCommandSessionConfiguration());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DevEnvironmentSessionConfiguration").add("SessionType", sessionTypeAsString())
                .add("ExecuteCommandSessionConfiguration", executeCommandSessionConfiguration()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "sessionType":
            return Optional.ofNullable(clazz.cast(sessionTypeAsString()));
        case "executeCommandSessionConfiguration":
            return Optional.ofNullable(clazz.cast(executeCommandSessionConfiguration()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DevEnvironmentSessionConfiguration, T> g) {
        return obj -> g.apply((DevEnvironmentSessionConfiguration) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, DevEnvironmentSessionConfiguration> {
        /**
         * <p>
         * The type of the session.
         * </p>
         * 
         * @param sessionType
         *        The type of the session.
         * @see DevEnvironmentSessionType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see DevEnvironmentSessionType
         */
        Builder sessionType(String sessionType);

        /**
         * <p>
         * The type of the session.
         * </p>
         * 
         * @param sessionType
         *        The type of the session.
         * @see DevEnvironmentSessionType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see DevEnvironmentSessionType
         */
        Builder sessionType(DevEnvironmentSessionType sessionType);

        /**
         * <p>
         * Information about optional commands that will be run on the Dev Environment when the SSH session begins.
         * </p>
         * 
         * @param executeCommandSessionConfiguration
         *        Information about optional commands that will be run on the Dev Environment when the SSH session
         *        begins.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder executeCommandSessionConfiguration(ExecuteCommandSessionConfiguration executeCommandSessionConfiguration);

        /**
         * <p>
         * Information about optional commands that will be run on the Dev Environment when the SSH session begins.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link ExecuteCommandSessionConfiguration.Builder} avoiding the need to create one manually via
         * {@link ExecuteCommandSessionConfiguration#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link ExecuteCommandSessionConfiguration.Builder#build()} is called
         * immediately and its result is passed to
         * {@link #executeCommandSessionConfiguration(ExecuteCommandSessionConfiguration)}.
         * 
         * @param executeCommandSessionConfiguration
         *        a consumer that will call methods on {@link ExecuteCommandSessionConfiguration.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #executeCommandSessionConfiguration(ExecuteCommandSessionConfiguration)
         */
        default Builder executeCommandSessionConfiguration(
                Consumer<ExecuteCommandSessionConfiguration.Builder> executeCommandSessionConfiguration) {
            return executeCommandSessionConfiguration(ExecuteCommandSessionConfiguration.builder()
                    .applyMutation(executeCommandSessionConfiguration).build());
        }
    }

    static final class BuilderImpl implements Builder {
        private String sessionType;

        private ExecuteCommandSessionConfiguration executeCommandSessionConfiguration;

        private BuilderImpl() {
        }

        private BuilderImpl(DevEnvironmentSessionConfiguration model) {
            sessionType(model.sessionType);
            executeCommandSessionConfiguration(model.executeCommandSessionConfiguration);
        }

        public final String getSessionType() {
            return sessionType;
        }

        public final void setSessionType(String sessionType) {
            this.sessionType = sessionType;
        }

        @Override
        public final Builder sessionType(String sessionType) {
            this.sessionType = sessionType;
            return this;
        }

        @Override
        public final Builder sessionType(DevEnvironmentSessionType sessionType) {
            this.sessionType(sessionType == null ? null : sessionType.toString());
            return this;
        }

        public final ExecuteCommandSessionConfiguration.Builder getExecuteCommandSessionConfiguration() {
            return executeCommandSessionConfiguration != null ? executeCommandSessionConfiguration.toBuilder() : null;
        }

        public final void setExecuteCommandSessionConfiguration(
                ExecuteCommandSessionConfiguration.BuilderImpl executeCommandSessionConfiguration) {
            this.executeCommandSessionConfiguration = executeCommandSessionConfiguration != null ? executeCommandSessionConfiguration
                    .build() : null;
        }

        @Override
        public final Builder executeCommandSessionConfiguration(
                ExecuteCommandSessionConfiguration executeCommandSessionConfiguration) {
            this.executeCommandSessionConfiguration = executeCommandSessionConfiguration;
            return this;
        }

        @Override
        public DevEnvironmentSessionConfiguration build() {
            return new DevEnvironmentSessionConfiguration(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
