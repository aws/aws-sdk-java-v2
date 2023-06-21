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
public final class StartDevEnvironmentRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<StartDevEnvironmentRequest.Builder, StartDevEnvironmentRequest> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(StartDevEnvironmentRequest::spaceName)).setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(StartDevEnvironmentRequest::projectName))
            .setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("projectName").build()).build();

    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(StartDevEnvironmentRequest::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("id").build()).build();

    private static final SdkField<List<IdeConfiguration>> IDES_FIELD = SdkField
            .<List<IdeConfiguration>> builder(MarshallingType.LIST)
            .memberName("ides")
            .getter(getter(StartDevEnvironmentRequest::ides))
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
            .memberName("instanceType").getter(getter(StartDevEnvironmentRequest::instanceTypeAsString))
            .setter(setter(Builder::instanceType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("instanceType").build()).build();

    private static final SdkField<Integer> INACTIVITY_TIMEOUT_MINUTES_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("inactivityTimeoutMinutes").getter(getter(StartDevEnvironmentRequest::inactivityTimeoutMinutes))
            .setter(setter(Builder::inactivityTimeoutMinutes))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("inactivityTimeoutMinutes").build())
            .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            PROJECT_NAME_FIELD, ID_FIELD, IDES_FIELD, INSTANCE_TYPE_FIELD, INACTIVITY_TIMEOUT_MINUTES_FIELD));

    private final String spaceName;

    private final String projectName;

    private final String id;

    private final List<IdeConfiguration> ides;

    private final String instanceType;

    private final Integer inactivityTimeoutMinutes;

    private StartDevEnvironmentRequest(BuilderImpl builder) {
        super(builder);
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.id = builder.id;
        this.ides = builder.ides;
        this.instanceType = builder.instanceType;
        this.inactivityTimeoutMinutes = builder.inactivityTimeoutMinutes;
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
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #instanceType} will
     * return {@link InstanceType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #instanceTypeAsString}.
     * </p>
     * 
     * @return The Amazon EC2 instace type to use for the Dev Environment.
     * @see InstanceType
     */
    public final InstanceType instanceType() {
        return InstanceType.fromValue(instanceType);
    }

    /**
     * <p>
     * The Amazon EC2 instace type to use for the Dev Environment.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #instanceType} will
     * return {@link InstanceType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #instanceTypeAsString}.
     * </p>
     * 
     * @return The Amazon EC2 instace type to use for the Dev Environment.
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
     * 
     * @return The amount of time the Dev Environment will run without any activity detected before stopping, in
     *         minutes. Only whole integers are allowed. Dev Environments consume compute minutes when running.
     */
    public final Integer inactivityTimeoutMinutes() {
        return inactivityTimeoutMinutes;
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
        hashCode = 31 * hashCode + Objects.hashCode(hasIdes() ? ides() : null);
        hashCode = 31 * hashCode + Objects.hashCode(instanceTypeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(inactivityTimeoutMinutes());
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
        if (!(obj instanceof StartDevEnvironmentRequest)) {
            return false;
        }
        StartDevEnvironmentRequest other = (StartDevEnvironmentRequest) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && Objects.equals(id(), other.id()) && hasIdes() == other.hasIdes() && Objects.equals(ides(), other.ides())
                && Objects.equals(instanceTypeAsString(), other.instanceTypeAsString())
                && Objects.equals(inactivityTimeoutMinutes(), other.inactivityTimeoutMinutes());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("StartDevEnvironmentRequest").add("SpaceName", spaceName()).add("ProjectName", projectName())
                .add("Id", id()).add("Ides", hasIdes() ? ides() : null).add("InstanceType", instanceTypeAsString())
                .add("InactivityTimeoutMinutes", inactivityTimeoutMinutes()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
        case "id":
            return Optional.ofNullable(clazz.cast(id()));
        case "ides":
            return Optional.ofNullable(clazz.cast(ides()));
        case "instanceType":
            return Optional.ofNullable(clazz.cast(instanceTypeAsString()));
        case "inactivityTimeoutMinutes":
            return Optional.ofNullable(clazz.cast(inactivityTimeoutMinutes()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<StartDevEnvironmentRequest, T> g) {
        return obj -> g.apply((StartDevEnvironmentRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo, CopyableBuilder<Builder, StartDevEnvironmentRequest> {
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
         * 
         * @param instanceType
         *        The Amazon EC2 instace type to use for the Dev Environment.
         * @see InstanceType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see InstanceType
         */
        Builder instanceType(String instanceType);

        /**
         * <p>
         * The Amazon EC2 instace type to use for the Dev Environment.
         * </p>
         * 
         * @param instanceType
         *        The Amazon EC2 instace type to use for the Dev Environment.
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
         * 
         * @param inactivityTimeoutMinutes
         *        The amount of time the Dev Environment will run without any activity detected before stopping, in
         *        minutes. Only whole integers are allowed. Dev Environments consume compute minutes when running.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder inactivityTimeoutMinutes(Integer inactivityTimeoutMinutes);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private String spaceName;

        private String projectName;

        private String id;

        private List<IdeConfiguration> ides = DefaultSdkAutoConstructList.getInstance();

        private String instanceType;

        private Integer inactivityTimeoutMinutes;

        private BuilderImpl() {
        }

        private BuilderImpl(StartDevEnvironmentRequest model) {
            super(model);
            spaceName(model.spaceName);
            projectName(model.projectName);
            id(model.id);
            ides(model.ides);
            instanceType(model.instanceType);
            inactivityTimeoutMinutes(model.inactivityTimeoutMinutes);
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
        public StartDevEnvironmentRequest build() {
            return new StartDevEnvironmentRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
