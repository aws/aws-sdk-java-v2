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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
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
 */
@Generated("software.amazon.awssdk:codegen")
public final class UpdateDevEnvironmentRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<UpdateDevEnvironmentRequest.Builder, UpdateDevEnvironmentRequest> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(UpdateDevEnvironmentRequest::spaceName)).setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(UpdateDevEnvironmentRequest::projectName))
            .setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("projectName").build()).build();

    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(UpdateDevEnvironmentRequest::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("id").build()).build();

    private static final SdkField<String> ALIAS_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("alias")
            .getter(getter(UpdateDevEnvironmentRequest::alias)).setter(setter(Builder::alias))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("alias").build()).build();

    private static final SdkField<List<IdeConfiguration>> IDES_FIELD = SdkField
            .<List<IdeConfiguration>> builder(MarshallingType.LIST)
            .memberName("ides")
            .getter(getter(UpdateDevEnvironmentRequest::ides))
            .setter(setter(Builder::ides))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ides").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<IdeConfiguration> builder(MarshallingType.SDK_POJO)
                                            .constructor(IdeConfiguration::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> INSTANCE_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("instanceType").getter(getter(UpdateDevEnvironmentRequest::instanceTypeAsString))
            .setter(setter(Builder::instanceType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("instanceType").build()).build();

    private static final SdkField<Integer> INACTIVITY_TIMEOUT_MINUTES_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("inactivityTimeoutMinutes").getter(getter(UpdateDevEnvironmentRequest::inactivityTimeoutMinutes))
            .setter(setter(Builder::inactivityTimeoutMinutes))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("inactivityTimeoutMinutes").build())
            .build();

    private static final SdkField<String> CLIENT_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("clientToken").getter(getter(UpdateDevEnvironmentRequest::clientToken))
            .setter(setter(Builder::clientToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("clientToken").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            PROJECT_NAME_FIELD, ID_FIELD, ALIAS_FIELD, IDES_FIELD, INSTANCE_TYPE_FIELD, INACTIVITY_TIMEOUT_MINUTES_FIELD,
            CLIENT_TOKEN_FIELD));

    private final String spaceName;

    private final String projectName;

    private final String id;

    private final String alias;

    private final List<IdeConfiguration> ides;

    private final String instanceType;

    private final Integer inactivityTimeoutMinutes;

    private final String clientToken;

    private UpdateDevEnvironmentRequest(BuilderImpl builder) {
        super(builder);
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.id = builder.id;
        this.alias = builder.alias;
        this.ides = builder.ides;
        this.instanceType = builder.instanceType;
        this.inactivityTimeoutMinutes = builder.inactivityTimeoutMinutes;
        this.clientToken = builder.clientToken;
    }

    /**
     * <p>
     * The name of the space.
     * </p>
     * 
     * @return The name of the space.
     */
    public final String spaceName() {
        return spaceName;
    }

    /**
     * <p>
     * The name of the project in the space.
     * </p>
     * 
     * @return The name of the project in the space.
     */
    public final String projectName() {
        return projectName;
    }

    /**
     * <p>
     * The system-generated unique ID of the Dev Environment.
     * </p>
     * 
     * @return The system-generated unique ID of the Dev Environment.
     */
    public final String id() {
        return id;
    }

    /**
     * <p>
     * The user-specified alias for the Dev Environment. Changing this value will not cause a restart.
     * </p>
     * 
     * @return The user-specified alias for the Dev Environment. Changing this value will not cause a restart.
     */
    public final String alias() {
        return alias;
    }

    /**
     * For responses, this returns true if the service returned a value for the Ides property. This DOES NOT check that
     * the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful
     * because the SDK will never return a null collection or map, but you may need to differentiate between the service
     * returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true
     * if a value for the property was specified in the request builder, and false if a value was not specified.
     */
    public final boolean hasIdes() {
        return ides != null && !(ides instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Information about the integrated development environment (IDE) configured for a Dev Environment.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasIdes} method.
     * </p>
     * 
     * @return Information about the integrated development environment (IDE) configured for a Dev Environment.
     */
    public final List<IdeConfiguration> ides() {
        return ides;
    }

    /**
     * <p>
     * The Amazon EC2 instace type to use for the Dev Environment.
     * </p>
     * <note>
     * <p>
     * Changing this value will cause a restart of the Dev Environment if it is running.
     * </p>
     * </note>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #instanceType} will
     * return {@link InstanceType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #instanceTypeAsString}.
     * </p>
     * 
     * @return The Amazon EC2 instace type to use for the Dev Environment. </p> <note>
     *         <p>
     *         Changing this value will cause a restart of the Dev Environment if it is running.
     *         </p>
     * @see InstanceType
     */
    public final InstanceType instanceType() {
        return InstanceType.fromValue(instanceType);
    }

    /**
     * <p>
     * The Amazon EC2 instace type to use for the Dev Environment.
     * </p>
     * <note>
     * <p>
     * Changing this value will cause a restart of the Dev Environment if it is running.
     * </p>
     * </note>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #instanceType} will
     * return {@link InstanceType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #instanceTypeAsString}.
     * </p>
     * 
     * @return The Amazon EC2 instace type to use for the Dev Environment. </p> <note>
     *         <p>
     *         Changing this value will cause a restart of the Dev Environment if it is running.
     *         </p>
     * @see InstanceType
     */
    public final String instanceTypeAsString() {
        return instanceType;
    }

    /**
     * <p>
     * The amount of time the Dev Environment will run without any activity detected before stopping, in minutes. Only
     * whole integers are allowed. Dev Environments consume compute minutes when running.
     * </p>
     * <note>
     * <p>
     * Changing this value will cause a restart of the Dev Environment if it is running.
     * </p>
     * </note>
     * 
     * @return The amount of time the Dev Environment will run without any activity detected before stopping, in
     *         minutes. Only whole integers are allowed. Dev Environments consume compute minutes when running.</p>
     *         <note>
     *         <p>
     *         Changing this value will cause a restart of the Dev Environment if it is running.
     *         </p>
     */
    public final Integer inactivityTimeoutMinutes() {
        return inactivityTimeoutMinutes;
    }

    /**
     * <p>
     * A user-specified idempotency token. Idempotency ensures that an API request completes only once. With an
     * idempotent request, if the original request completes successfully, the subsequent retries return the result from
     * the original successful request and have no additional effect.
     * </p>
     * 
     * @return A user-specified idempotency token. Idempotency ensures that an API request completes only once. With an
     *         idempotent request, if the original request completes successfully, the subsequent retries return the
     *         result from the original successful request and have no additional effect.
     */
    public final String clientToken() {
        return clientToken;
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
        hashCode = 31 * hashCode + Objects.hashCode(spaceName());
        hashCode = 31 * hashCode + Objects.hashCode(projectName());
        hashCode = 31 * hashCode + Objects.hashCode(id());
        hashCode = 31 * hashCode + Objects.hashCode(alias());
        hashCode = 31 * hashCode + Objects.hashCode(hasIdes() ? ides() : null);
        hashCode = 31 * hashCode + Objects.hashCode(instanceTypeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(inactivityTimeoutMinutes());
        hashCode = 31 * hashCode + Objects.hashCode(clientToken());
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
        if (!(obj instanceof UpdateDevEnvironmentRequest)) {
            return false;
        }
        UpdateDevEnvironmentRequest other = (UpdateDevEnvironmentRequest) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && Objects.equals(id(), other.id()) && Objects.equals(alias(), other.alias()) && hasIdes() == other.hasIdes()
                && Objects.equals(ides(), other.ides()) && Objects.equals(instanceTypeAsString(), other.instanceTypeAsString())
                && Objects.equals(inactivityTimeoutMinutes(), other.inactivityTimeoutMinutes())
                && Objects.equals(clientToken(), other.clientToken());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("UpdateDevEnvironmentRequest").add("SpaceName", spaceName()).add("ProjectName", projectName())
                .add("Id", id()).add("Alias", alias()).add("Ides", hasIdes() ? ides() : null)
                .add("InstanceType", instanceTypeAsString()).add("InactivityTimeoutMinutes", inactivityTimeoutMinutes())
                .add("ClientToken", clientToken()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
        case "id":
            return Optional.ofNullable(clazz.cast(id()));
        case "alias":
            return Optional.ofNullable(clazz.cast(alias()));
        case "ides":
            return Optional.ofNullable(clazz.cast(ides()));
        case "instanceType":
            return Optional.ofNullable(clazz.cast(instanceTypeAsString()));
        case "inactivityTimeoutMinutes":
            return Optional.ofNullable(clazz.cast(inactivityTimeoutMinutes()));
        case "clientToken":
            return Optional.ofNullable(clazz.cast(clientToken()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<UpdateDevEnvironmentRequest, T> g) {
        return obj -> g.apply((UpdateDevEnvironmentRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo, CopyableBuilder<Builder, UpdateDevEnvironmentRequest> {
        /**
         * <p>
         * The name of the space.
         * </p>
         * 
         * @param spaceName
         *        The name of the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder spaceName(String spaceName);

        /**
         * <p>
         * The name of the project in the space.
         * </p>
         * 
         * @param projectName
         *        The name of the project in the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder projectName(String projectName);

        /**
         * <p>
         * The system-generated unique ID of the Dev Environment.
         * </p>
         * 
         * @param id
         *        The system-generated unique ID of the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder id(String id);

        /**
         * <p>
         * The user-specified alias for the Dev Environment. Changing this value will not cause a restart.
         * </p>
         * 
         * @param alias
         *        The user-specified alias for the Dev Environment. Changing this value will not cause a restart.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder alias(String alias);

        /**
         * <p>
         * Information about the integrated development environment (IDE) configured for a Dev Environment.
         * </p>
         * 
         * @param ides
         *        Information about the integrated development environment (IDE) configured for a Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder ides(Collection<IdeConfiguration> ides);

        /**
         * <p>
         * Information about the integrated development environment (IDE) configured for a Dev Environment.
         * </p>
         * 
         * @param ides
         *        Information about the integrated development environment (IDE) configured for a Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder ides(IdeConfiguration... ides);

        /**
         * <p>
         * Information about the integrated development environment (IDE) configured for a Dev Environment.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.codecatalyst.model.IdeConfiguration.Builder} avoiding the need to
         * create one manually via {@link software.amazon.awssdk.services.codecatalyst.model.IdeConfiguration#builder()}
         * .
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.codecatalyst.model.IdeConfiguration.Builder#build()} is called
         * immediately and its result is passed to {@link #ides(List<IdeConfiguration>)}.
         * 
         * @param ides
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.codecatalyst.model.IdeConfiguration.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #ides(java.util.Collection<IdeConfiguration>)
         */
        Builder ides(Consumer<IdeConfiguration.Builder>... ides);

        /**
         * <p>
         * The Amazon EC2 instace type to use for the Dev Environment.
         * </p>
         * <note>
         * <p>
         * Changing this value will cause a restart of the Dev Environment if it is running.
         * </p>
         * </note>
         * 
         * @param instanceType
         *        The Amazon EC2 instace type to use for the Dev Environment. </p> <note>
         *        <p>
         *        Changing this value will cause a restart of the Dev Environment if it is running.
         *        </p>
         * @see InstanceType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see InstanceType
         */
        Builder instanceType(String instanceType);

        /**
         * <p>
         * The Amazon EC2 instace type to use for the Dev Environment.
         * </p>
         * <note>
         * <p>
         * Changing this value will cause a restart of the Dev Environment if it is running.
         * </p>
         * </note>
         * 
         * @param instanceType
         *        The Amazon EC2 instace type to use for the Dev Environment. </p> <note>
         *        <p>
         *        Changing this value will cause a restart of the Dev Environment if it is running.
         *        </p>
         * @see InstanceType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see InstanceType
         */
        Builder instanceType(InstanceType instanceType);

        /**
         * <p>
         * The amount of time the Dev Environment will run without any activity detected before stopping, in minutes.
         * Only whole integers are allowed. Dev Environments consume compute minutes when running.
         * </p>
         * <note>
         * <p>
         * Changing this value will cause a restart of the Dev Environment if it is running.
         * </p>
         * </note>
         * 
         * @param inactivityTimeoutMinutes
         *        The amount of time the Dev Environment will run without any activity detected before stopping, in
         *        minutes. Only whole integers are allowed. Dev Environments consume compute minutes when running.</p>
         *        <note>
         *        <p>
         *        Changing this value will cause a restart of the Dev Environment if it is running.
         *        </p>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder inactivityTimeoutMinutes(Integer inactivityTimeoutMinutes);

        /**
         * <p>
         * A user-specified idempotency token. Idempotency ensures that an API request completes only once. With an
         * idempotent request, if the original request completes successfully, the subsequent retries return the result
         * from the original successful request and have no additional effect.
         * </p>
         * 
         * @param clientToken
         *        A user-specified idempotency token. Idempotency ensures that an API request completes only once. With
         *        an idempotent request, if the original request completes successfully, the subsequent retries return
         *        the result from the original successful request and have no additional effect.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder clientToken(String clientToken);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private String spaceName;

        private String projectName;

        private String id;

        private String alias;

        private List<IdeConfiguration> ides = DefaultSdkAutoConstructList.getInstance();

        private String instanceType;

        private Integer inactivityTimeoutMinutes;

        private String clientToken;

        private BuilderImpl() {
        }

        private BuilderImpl(UpdateDevEnvironmentRequest model) {
            super(model);
            spaceName(model.spaceName);
            projectName(model.projectName);
            id(model.id);
            alias(model.alias);
            ides(model.ides);
            instanceType(model.instanceType);
            inactivityTimeoutMinutes(model.inactivityTimeoutMinutes);
            clientToken(model.clientToken);
        }

        public final String getSpaceName() {
            return spaceName;
        }

        public final void setSpaceName(String spaceName) {
            this.spaceName = spaceName;
        }

        @Override
        public final Builder spaceName(String spaceName) {
            this.spaceName = spaceName;
            return this;
        }

        public final String getProjectName() {
            return projectName;
        }

        public final void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        @Override
        public final Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public final String getId() {
            return id;
        }

        public final void setId(String id) {
            this.id = id;
        }

        @Override
        public final Builder id(String id) {
            this.id = id;
            return this;
        }

        public final String getAlias() {
            return alias;
        }

        public final void setAlias(String alias) {
            this.alias = alias;
        }

        @Override
        public final Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public final List<IdeConfiguration.Builder> getIdes() {
            List<IdeConfiguration.Builder> result = IdeConfigurationListCopier.copyToBuilder(this.ides);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setIdes(Collection<IdeConfiguration.BuilderImpl> ides) {
            this.ides = IdeConfigurationListCopier.copyFromBuilder(ides);
        }

        @Override
        public final Builder ides(Collection<IdeConfiguration> ides) {
            this.ides = IdeConfigurationListCopier.copy(ides);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder ides(IdeConfiguration... ides) {
            ides(Arrays.asList(ides));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder ides(Consumer<IdeConfiguration.Builder>... ides) {
            ides(Stream.of(ides).map(c -> IdeConfiguration.builder().applyMutation(c).build()).collect(Collectors.toList()));
            return this;
        }

        public final String getInstanceType() {
            return instanceType;
        }

        public final void setInstanceType(String instanceType) {
            this.instanceType = instanceType;
        }

        @Override
        public final Builder instanceType(String instanceType) {
            this.instanceType = instanceType;
            return this;
        }

        @Override
        public final Builder instanceType(InstanceType instanceType) {
            this.instanceType(instanceType == null ? null : instanceType.toString());
            return this;
        }

        public final Integer getInactivityTimeoutMinutes() {
            return inactivityTimeoutMinutes;
        }

        public final void setInactivityTimeoutMinutes(Integer inactivityTimeoutMinutes) {
            this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
        }

        @Override
        public final Builder inactivityTimeoutMinutes(Integer inactivityTimeoutMinutes) {
            this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
            return this;
        }

        public final String getClientToken() {
            return clientToken;
        }

        public final void setClientToken(String clientToken) {
            this.clientToken = clientToken;
        }

        @Override
        public final Builder clientToken(String clientToken) {
            this.clientToken = clientToken;
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
        public UpdateDevEnvironmentRequest build() {
            return new UpdateDevEnvironmentRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
