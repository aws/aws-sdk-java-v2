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
import java.time.Instant;
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
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <p>
 * Information about a Dev Environment.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class DevEnvironmentSummary implements SdkPojo, Serializable,
        ToCopyableBuilder<DevEnvironmentSummary.Builder, DevEnvironmentSummary> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(DevEnvironmentSummary::spaceName)).setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(DevEnvironmentSummary::projectName)).setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("projectName").build()).build();

    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(DevEnvironmentSummary::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("id").build()).build();

    private static final SdkField<Instant> LAST_UPDATED_TIME_FIELD = SdkField
            .<Instant> builder(MarshallingType.INSTANT)
            .memberName("lastUpdatedTime")
            .getter(getter(DevEnvironmentSummary::lastUpdatedTime))
            .setter(setter(Builder::lastUpdatedTime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("lastUpdatedTime").build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601)).build();

    private static final SdkField<String> CREATOR_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("creatorId").getter(getter(DevEnvironmentSummary::creatorId)).setter(setter(Builder::creatorId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("creatorId").build()).build();

    private static final SdkField<String> STATUS_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("status")
            .getter(getter(DevEnvironmentSummary::statusAsString)).setter(setter(Builder::status))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("status").build()).build();

    private static final SdkField<String> STATUS_REASON_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("statusReason").getter(getter(DevEnvironmentSummary::statusReason)).setter(setter(Builder::statusReason))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("statusReason").build()).build();

    private static final SdkField<List<DevEnvironmentRepositorySummary>> REPOSITORIES_FIELD = SdkField
            .<List<DevEnvironmentRepositorySummary>> builder(MarshallingType.LIST)
            .memberName("repositories")
            .getter(getter(DevEnvironmentSummary::repositories))
            .setter(setter(Builder::repositories))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("repositories").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<DevEnvironmentRepositorySummary> builder(MarshallingType.SDK_POJO)
                                            .constructor(DevEnvironmentRepositorySummary::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> ALIAS_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("alias")
            .getter(getter(DevEnvironmentSummary::alias)).setter(setter(Builder::alias))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("alias").build()).build();

    private static final SdkField<List<Ide>> IDES_FIELD = SdkField
            .<List<Ide>> builder(MarshallingType.LIST)
            .memberName("ides")
            .getter(getter(DevEnvironmentSummary::ides))
            .setter(setter(Builder::ides))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ides").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<Ide> builder(MarshallingType.SDK_POJO)
                                            .constructor(Ide::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> INSTANCE_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("instanceType").getter(getter(DevEnvironmentSummary::instanceTypeAsString))
            .setter(setter(Builder::instanceType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("instanceType").build()).build();

    private static final SdkField<Integer> INACTIVITY_TIMEOUT_MINUTES_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("inactivityTimeoutMinutes").getter(getter(DevEnvironmentSummary::inactivityTimeoutMinutes))
            .setter(setter(Builder::inactivityTimeoutMinutes))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("inactivityTimeoutMinutes").build())
            .build();

    private static final SdkField<PersistentStorage> PERSISTENT_STORAGE_FIELD = SdkField
            .<PersistentStorage> builder(MarshallingType.SDK_POJO).memberName("persistentStorage")
            .getter(getter(DevEnvironmentSummary::persistentStorage)).setter(setter(Builder::persistentStorage))
            .constructor(PersistentStorage::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("persistentStorage").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            PROJECT_NAME_FIELD, ID_FIELD, LAST_UPDATED_TIME_FIELD, CREATOR_ID_FIELD, STATUS_FIELD, STATUS_REASON_FIELD,
            REPOSITORIES_FIELD, ALIAS_FIELD, IDES_FIELD, INSTANCE_TYPE_FIELD, INACTIVITY_TIMEOUT_MINUTES_FIELD,
            PERSISTENT_STORAGE_FIELD));

    private static final long serialVersionUID = 1L;

    private final String spaceName;

    private final String projectName;

    private final String id;

    private final Instant lastUpdatedTime;

    private final String creatorId;

    private final String status;

    private final String statusReason;

    private final List<DevEnvironmentRepositorySummary> repositories;

    private final String alias;

    private final List<Ide> ides;

    private final String instanceType;

    private final Integer inactivityTimeoutMinutes;

    private final PersistentStorage persistentStorage;

    private DevEnvironmentSummary(BuilderImpl builder) {
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.id = builder.id;
        this.lastUpdatedTime = builder.lastUpdatedTime;
        this.creatorId = builder.creatorId;
        this.status = builder.status;
        this.statusReason = builder.statusReason;
        this.repositories = builder.repositories;
        this.alias = builder.alias;
        this.ides = builder.ides;
        this.instanceType = builder.instanceType;
        this.inactivityTimeoutMinutes = builder.inactivityTimeoutMinutes;
        this.persistentStorage = builder.persistentStorage;
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
     * The system-generated unique ID for the Dev Environment.
     * </p>
     * 
     * @return The system-generated unique ID for the Dev Environment.
     */
    public final String id() {
        return id;
    }

    /**
     * <p>
     * The time when the Dev Environment was last updated, in coordinated universal time (UTC) timestamp format as
     * specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     * </p>
     * 
     * @return The time when the Dev Environment was last updated, in coordinated universal time (UTC) timestamp format
     *         as specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     */
    public final Instant lastUpdatedTime() {
        return lastUpdatedTime;
    }

    /**
     * <p>
     * The system-generated unique ID of the user who created the Dev Environment.
     * </p>
     * 
     * @return The system-generated unique ID of the user who created the Dev Environment.
     */
    public final String creatorId() {
        return creatorId;
    }

    /**
     * <p>
     * The status of the Dev Environment.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #status} will
     * return {@link DevEnvironmentStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #statusAsString}.
     * </p>
     * 
     * @return The status of the Dev Environment.
     * @see DevEnvironmentStatus
     */
    public final DevEnvironmentStatus status() {
        return DevEnvironmentStatus.fromValue(status);
    }

    /**
     * <p>
     * The status of the Dev Environment.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #status} will
     * return {@link DevEnvironmentStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #statusAsString}.
     * </p>
     * 
     * @return The status of the Dev Environment.
     * @see DevEnvironmentStatus
     */
    public final String statusAsString() {
        return status;
    }

    /**
     * <p>
     * The reason for the status.
     * </p>
     * 
     * @return The reason for the status.
     */
    public final String statusReason() {
        return statusReason;
    }

    /**
     * For responses, this returns true if the service returned a value for the Repositories property. This DOES NOT
     * check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasRepositories() {
        return repositories != null && !(repositories instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Information about the repositories that will be cloned into the Dev Environment. If no rvalue is specified, no
     * repository is cloned.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasRepositories} method.
     * </p>
     * 
     * @return Information about the repositories that will be cloned into the Dev Environment. If no rvalue is
     *         specified, no repository is cloned.
     */
    public final List<DevEnvironmentRepositorySummary> repositories() {
        return repositories;
    }

    /**
     * <p>
     * The user-specified alias for the Dev Environment.
     * </p>
     * 
     * @return The user-specified alias for the Dev Environment.
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
    public final List<Ide> ides() {
        return ides;
    }

    /**
     * <p>
     * The Amazon EC2 instace type used for the Dev Environment.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #instanceType} will
     * return {@link InstanceType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #instanceTypeAsString}.
     * </p>
     * 
     * @return The Amazon EC2 instace type used for the Dev Environment.
     * @see InstanceType
     */
    public final InstanceType instanceType() {
        return InstanceType.fromValue(instanceType);
    }

    /**
     * <p>
     * The Amazon EC2 instace type used for the Dev Environment.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #instanceType} will
     * return {@link InstanceType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #instanceTypeAsString}.
     * </p>
     * 
     * @return The Amazon EC2 instace type used for the Dev Environment.
     * @see InstanceType
     */
    public final String instanceTypeAsString() {
        return instanceType;
    }

    /**
     * <p>
     * The amount of time the Dev Environment will run without any activity detected before stopping, in minutes. Dev
     * Environments consume compute minutes when running.
     * </p>
     * 
     * @return The amount of time the Dev Environment will run without any activity detected before stopping, in
     *         minutes. Dev Environments consume compute minutes when running.
     */
    public final Integer inactivityTimeoutMinutes() {
        return inactivityTimeoutMinutes;
    }

    /**
     * <p>
     * Information about the configuration of persistent storage for the Dev Environment.
     * </p>
     * 
     * @return Information about the configuration of persistent storage for the Dev Environment.
     */
    public final PersistentStorage persistentStorage() {
        return persistentStorage;
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
        hashCode = 31 * hashCode + Objects.hashCode(spaceName());
        hashCode = 31 * hashCode + Objects.hashCode(projectName());
        hashCode = 31 * hashCode + Objects.hashCode(id());
        hashCode = 31 * hashCode + Objects.hashCode(lastUpdatedTime());
        hashCode = 31 * hashCode + Objects.hashCode(creatorId());
        hashCode = 31 * hashCode + Objects.hashCode(statusAsString());
        hashCode = 31 * hashCode + Objects.hashCode(statusReason());
        hashCode = 31 * hashCode + Objects.hashCode(hasRepositories() ? repositories() : null);
        hashCode = 31 * hashCode + Objects.hashCode(alias());
        hashCode = 31 * hashCode + Objects.hashCode(hasIdes() ? ides() : null);
        hashCode = 31 * hashCode + Objects.hashCode(instanceTypeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(inactivityTimeoutMinutes());
        hashCode = 31 * hashCode + Objects.hashCode(persistentStorage());
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
        if (!(obj instanceof DevEnvironmentSummary)) {
            return false;
        }
        DevEnvironmentSummary other = (DevEnvironmentSummary) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && Objects.equals(id(), other.id()) && Objects.equals(lastUpdatedTime(), other.lastUpdatedTime())
                && Objects.equals(creatorId(), other.creatorId()) && Objects.equals(statusAsString(), other.statusAsString())
                && Objects.equals(statusReason(), other.statusReason()) && hasRepositories() == other.hasRepositories()
                && Objects.equals(repositories(), other.repositories()) && Objects.equals(alias(), other.alias())
                && hasIdes() == other.hasIdes() && Objects.equals(ides(), other.ides())
                && Objects.equals(instanceTypeAsString(), other.instanceTypeAsString())
                && Objects.equals(inactivityTimeoutMinutes(), other.inactivityTimeoutMinutes())
                && Objects.equals(persistentStorage(), other.persistentStorage());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DevEnvironmentSummary").add("SpaceName", spaceName()).add("ProjectName", projectName())
                .add("Id", id()).add("LastUpdatedTime", lastUpdatedTime()).add("CreatorId", creatorId())
                .add("Status", statusAsString()).add("StatusReason", statusReason())
                .add("Repositories", hasRepositories() ? repositories() : null).add("Alias", alias())
                .add("Ides", hasIdes() ? ides() : null).add("InstanceType", instanceTypeAsString())
                .add("InactivityTimeoutMinutes", inactivityTimeoutMinutes()).add("PersistentStorage", persistentStorage())
                .build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
        case "id":
            return Optional.ofNullable(clazz.cast(id()));
        case "lastUpdatedTime":
            return Optional.ofNullable(clazz.cast(lastUpdatedTime()));
        case "creatorId":
            return Optional.ofNullable(clazz.cast(creatorId()));
        case "status":
            return Optional.ofNullable(clazz.cast(statusAsString()));
        case "statusReason":
            return Optional.ofNullable(clazz.cast(statusReason()));
        case "repositories":
            return Optional.ofNullable(clazz.cast(repositories()));
        case "alias":
            return Optional.ofNullable(clazz.cast(alias()));
        case "ides":
            return Optional.ofNullable(clazz.cast(ides()));
        case "instanceType":
            return Optional.ofNullable(clazz.cast(instanceTypeAsString()));
        case "inactivityTimeoutMinutes":
            return Optional.ofNullable(clazz.cast(inactivityTimeoutMinutes()));
        case "persistentStorage":
            return Optional.ofNullable(clazz.cast(persistentStorage()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DevEnvironmentSummary, T> g) {
        return obj -> g.apply((DevEnvironmentSummary) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, DevEnvironmentSummary> {
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
         * The system-generated unique ID for the Dev Environment.
         * </p>
         * 
         * @param id
         *        The system-generated unique ID for the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder id(String id);

        /**
         * <p>
         * The time when the Dev Environment was last updated, in coordinated universal time (UTC) timestamp format as
         * specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * </p>
         * 
         * @param lastUpdatedTime
         *        The time when the Dev Environment was last updated, in coordinated universal time (UTC) timestamp
         *        format as specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder lastUpdatedTime(Instant lastUpdatedTime);

        /**
         * <p>
         * The system-generated unique ID of the user who created the Dev Environment.
         * </p>
         * 
         * @param creatorId
         *        The system-generated unique ID of the user who created the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder creatorId(String creatorId);

        /**
         * <p>
         * The status of the Dev Environment.
         * </p>
         * 
         * @param status
         *        The status of the Dev Environment.
         * @see DevEnvironmentStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see DevEnvironmentStatus
         */
        Builder status(String status);

        /**
         * <p>
         * The status of the Dev Environment.
         * </p>
         * 
         * @param status
         *        The status of the Dev Environment.
         * @see DevEnvironmentStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see DevEnvironmentStatus
         */
        Builder status(DevEnvironmentStatus status);

        /**
         * <p>
         * The reason for the status.
         * </p>
         * 
         * @param statusReason
         *        The reason for the status.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder statusReason(String statusReason);

        /**
         * <p>
         * Information about the repositories that will be cloned into the Dev Environment. If no rvalue is specified,
         * no repository is cloned.
         * </p>
         * 
         * @param repositories
         *        Information about the repositories that will be cloned into the Dev Environment. If no rvalue is
         *        specified, no repository is cloned.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder repositories(Collection<DevEnvironmentRepositorySummary> repositories);

        /**
         * <p>
         * Information about the repositories that will be cloned into the Dev Environment. If no rvalue is specified,
         * no repository is cloned.
         * </p>
         * 
         * @param repositories
         *        Information about the repositories that will be cloned into the Dev Environment. If no rvalue is
         *        specified, no repository is cloned.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder repositories(DevEnvironmentRepositorySummary... repositories);

        /**
         * <p>
         * Information about the repositories that will be cloned into the Dev Environment. If no rvalue is specified,
         * no repository is cloned.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.codecatalyst.model.DevEnvironmentRepositorySummary.Builder} avoiding
         * the need to create one manually via
         * {@link software.amazon.awssdk.services.codecatalyst.model.DevEnvironmentRepositorySummary#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.codecatalyst.model.DevEnvironmentRepositorySummary.Builder#build()} is
         * called immediately and its result is passed to {@link #repositories(List<DevEnvironmentRepositorySummary>)}.
         * 
         * @param repositories
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.codecatalyst.model.DevEnvironmentRepositorySummary.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #repositories(java.util.Collection<DevEnvironmentRepositorySummary>)
         */
        Builder repositories(Consumer<DevEnvironmentRepositorySummary.Builder>... repositories);

        /**
         * <p>
         * The user-specified alias for the Dev Environment.
         * </p>
         * 
         * @param alias
         *        The user-specified alias for the Dev Environment.
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
        Builder ides(Collection<Ide> ides);

        /**
         * <p>
         * Information about the integrated development environment (IDE) configured for a Dev Environment.
         * </p>
         * 
         * @param ides
         *        Information about the integrated development environment (IDE) configured for a Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder ides(Ide... ides);

        /**
         * <p>
         * Information about the integrated development environment (IDE) configured for a Dev Environment.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.codecatalyst.model.Ide.Builder} avoiding the need to create one
         * manually via {@link software.amazon.awssdk.services.codecatalyst.model.Ide#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.codecatalyst.model.Ide.Builder#build()} is called immediately and its
         * result is passed to {@link #ides(List<Ide>)}.
         * 
         * @param ides
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.codecatalyst.model.Ide.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #ides(java.util.Collection<Ide>)
         */
        Builder ides(Consumer<Ide.Builder>... ides);

        /**
         * <p>
         * The Amazon EC2 instace type used for the Dev Environment.
         * </p>
         * 
         * @param instanceType
         *        The Amazon EC2 instace type used for the Dev Environment.
         * @see InstanceType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see InstanceType
         */
        Builder instanceType(String instanceType);

        /**
         * <p>
         * The Amazon EC2 instace type used for the Dev Environment.
         * </p>
         * 
         * @param instanceType
         *        The Amazon EC2 instace type used for the Dev Environment.
         * @see InstanceType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see InstanceType
         */
        Builder instanceType(InstanceType instanceType);

        /**
         * <p>
         * The amount of time the Dev Environment will run without any activity detected before stopping, in minutes.
         * Dev Environments consume compute minutes when running.
         * </p>
         * 
         * @param inactivityTimeoutMinutes
         *        The amount of time the Dev Environment will run without any activity detected before stopping, in
         *        minutes. Dev Environments consume compute minutes when running.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder inactivityTimeoutMinutes(Integer inactivityTimeoutMinutes);

        /**
         * <p>
         * Information about the configuration of persistent storage for the Dev Environment.
         * </p>
         * 
         * @param persistentStorage
         *        Information about the configuration of persistent storage for the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder persistentStorage(PersistentStorage persistentStorage);

        /**
         * <p>
         * Information about the configuration of persistent storage for the Dev Environment.
         * </p>
         * This is a convenience method that creates an instance of the {@link PersistentStorage.Builder} avoiding the
         * need to create one manually via {@link PersistentStorage#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link PersistentStorage.Builder#build()} is called immediately and its
         * result is passed to {@link #persistentStorage(PersistentStorage)}.
         * 
         * @param persistentStorage
         *        a consumer that will call methods on {@link PersistentStorage.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #persistentStorage(PersistentStorage)
         */
        default Builder persistentStorage(Consumer<PersistentStorage.Builder> persistentStorage) {
            return persistentStorage(PersistentStorage.builder().applyMutation(persistentStorage).build());
        }
    }

    static final class BuilderImpl implements Builder {
        private String spaceName;

        private String projectName;

        private String id;

        private Instant lastUpdatedTime;

        private String creatorId;

        private String status;

        private String statusReason;

        private List<DevEnvironmentRepositorySummary> repositories = DefaultSdkAutoConstructList.getInstance();

        private String alias;

        private List<Ide> ides = DefaultSdkAutoConstructList.getInstance();

        private String instanceType;

        private Integer inactivityTimeoutMinutes;

        private PersistentStorage persistentStorage;

        private BuilderImpl() {
        }

        private BuilderImpl(DevEnvironmentSummary model) {
            spaceName(model.spaceName);
            projectName(model.projectName);
            id(model.id);
            lastUpdatedTime(model.lastUpdatedTime);
            creatorId(model.creatorId);
            status(model.status);
            statusReason(model.statusReason);
            repositories(model.repositories);
            alias(model.alias);
            ides(model.ides);
            instanceType(model.instanceType);
            inactivityTimeoutMinutes(model.inactivityTimeoutMinutes);
            persistentStorage(model.persistentStorage);
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

        public final Instant getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public final void setLastUpdatedTime(Instant lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
        }

        @Override
        public final Builder lastUpdatedTime(Instant lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
            return this;
        }

        public final String getCreatorId() {
            return creatorId;
        }

        public final void setCreatorId(String creatorId) {
            this.creatorId = creatorId;
        }

        @Override
        public final Builder creatorId(String creatorId) {
            this.creatorId = creatorId;
            return this;
        }

        public final String getStatus() {
            return status;
        }

        public final void setStatus(String status) {
            this.status = status;
        }

        @Override
        public final Builder status(String status) {
            this.status = status;
            return this;
        }

        @Override
        public final Builder status(DevEnvironmentStatus status) {
            this.status(status == null ? null : status.toString());
            return this;
        }

        public final String getStatusReason() {
            return statusReason;
        }

        public final void setStatusReason(String statusReason) {
            this.statusReason = statusReason;
        }

        @Override
        public final Builder statusReason(String statusReason) {
            this.statusReason = statusReason;
            return this;
        }

        public final List<DevEnvironmentRepositorySummary.Builder> getRepositories() {
            List<DevEnvironmentRepositorySummary.Builder> result = DevEnvironmentRepositorySummariesCopier
                    .copyToBuilder(this.repositories);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setRepositories(Collection<DevEnvironmentRepositorySummary.BuilderImpl> repositories) {
            this.repositories = DevEnvironmentRepositorySummariesCopier.copyFromBuilder(repositories);
        }

        @Override
        public final Builder repositories(Collection<DevEnvironmentRepositorySummary> repositories) {
            this.repositories = DevEnvironmentRepositorySummariesCopier.copy(repositories);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder repositories(DevEnvironmentRepositorySummary... repositories) {
            repositories(Arrays.asList(repositories));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder repositories(Consumer<DevEnvironmentRepositorySummary.Builder>... repositories) {
            repositories(Stream.of(repositories).map(c -> DevEnvironmentRepositorySummary.builder().applyMutation(c).build())
                    .collect(Collectors.toList()));
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

        public final List<Ide.Builder> getIdes() {
            List<Ide.Builder> result = IdesCopier.copyToBuilder(this.ides);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setIdes(Collection<Ide.BuilderImpl> ides) {
            this.ides = IdesCopier.copyFromBuilder(ides);
        }

        @Override
        public final Builder ides(Collection<Ide> ides) {
            this.ides = IdesCopier.copy(ides);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder ides(Ide... ides) {
            ides(Arrays.asList(ides));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder ides(Consumer<Ide.Builder>... ides) {
            ides(Stream.of(ides).map(c -> Ide.builder().applyMutation(c).build()).collect(Collectors.toList()));
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

        public final PersistentStorage.Builder getPersistentStorage() {
            return persistentStorage != null ? persistentStorage.toBuilder() : null;
        }

        public final void setPersistentStorage(PersistentStorage.BuilderImpl persistentStorage) {
            this.persistentStorage = persistentStorage != null ? persistentStorage.build() : null;
        }

        @Override
        public final Builder persistentStorage(PersistentStorage persistentStorage) {
            this.persistentStorage = persistentStorage;
            return this;
        }

        @Override
        public DevEnvironmentSummary build() {
            return new DevEnvironmentSummary(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
