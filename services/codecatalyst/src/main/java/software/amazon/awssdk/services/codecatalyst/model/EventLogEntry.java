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
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <p>
 * Information about an entry in an event log of Amazon CodeCatalyst activity.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class EventLogEntry implements SdkPojo, Serializable, ToCopyableBuilder<EventLogEntry.Builder, EventLogEntry> {
    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(EventLogEntry::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("id").build()).build();

    private static final SdkField<String> EVENT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("eventName").getter(getter(EventLogEntry::eventName)).setter(setter(Builder::eventName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("eventName").build()).build();

    private static final SdkField<String> EVENT_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("eventType").getter(getter(EventLogEntry::eventType)).setter(setter(Builder::eventType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("eventType").build()).build();

    private static final SdkField<String> EVENT_CATEGORY_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("eventCategory").getter(getter(EventLogEntry::eventCategory)).setter(setter(Builder::eventCategory))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("eventCategory").build()).build();

    private static final SdkField<String> EVENT_SOURCE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("eventSource").getter(getter(EventLogEntry::eventSource)).setter(setter(Builder::eventSource))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("eventSource").build()).build();

    private static final SdkField<Instant> EVENT_TIME_FIELD = SdkField
            .<Instant> builder(MarshallingType.INSTANT)
            .memberName("eventTime")
            .getter(getter(EventLogEntry::eventTime))
            .setter(setter(Builder::eventTime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("eventTime").build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601)).build();

    private static final SdkField<String> OPERATION_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("operationType").getter(getter(EventLogEntry::operationTypeAsString))
            .setter(setter(Builder::operationType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("operationType").build()).build();

    private static final SdkField<UserIdentity> USER_IDENTITY_FIELD = SdkField.<UserIdentity> builder(MarshallingType.SDK_POJO)
            .memberName("userIdentity").getter(getter(EventLogEntry::userIdentity)).setter(setter(Builder::userIdentity))
            .constructor(UserIdentity::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userIdentity").build()).build();

    private static final SdkField<ProjectInformation> PROJECT_INFORMATION_FIELD = SdkField
            .<ProjectInformation> builder(MarshallingType.SDK_POJO).memberName("projectInformation")
            .getter(getter(EventLogEntry::projectInformation)).setter(setter(Builder::projectInformation))
            .constructor(ProjectInformation::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("projectInformation").build())
            .build();

    private static final SdkField<String> REQUEST_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("requestId").getter(getter(EventLogEntry::requestId)).setter(setter(Builder::requestId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("requestId").build()).build();

    private static final SdkField<EventPayload> REQUEST_PAYLOAD_FIELD = SdkField.<EventPayload> builder(MarshallingType.SDK_POJO)
            .memberName("requestPayload").getter(getter(EventLogEntry::requestPayload)).setter(setter(Builder::requestPayload))
            .constructor(EventPayload::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("requestPayload").build()).build();

    private static final SdkField<EventPayload> RESPONSE_PAYLOAD_FIELD = SdkField
            .<EventPayload> builder(MarshallingType.SDK_POJO).memberName("responsePayload")
            .getter(getter(EventLogEntry::responsePayload)).setter(setter(Builder::responsePayload))
            .constructor(EventPayload::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("responsePayload").build()).build();

    private static final SdkField<String> ERROR_CODE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("errorCode").getter(getter(EventLogEntry::errorCode)).setter(setter(Builder::errorCode))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("errorCode").build()).build();

    private static final SdkField<String> SOURCE_IP_ADDRESS_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("sourceIpAddress").getter(getter(EventLogEntry::sourceIpAddress))
            .setter(setter(Builder::sourceIpAddress))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("sourceIpAddress").build()).build();

    private static final SdkField<String> USER_AGENT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("userAgent").getter(getter(EventLogEntry::userAgent)).setter(setter(Builder::userAgent))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userAgent").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(ID_FIELD, EVENT_NAME_FIELD,
            EVENT_TYPE_FIELD, EVENT_CATEGORY_FIELD, EVENT_SOURCE_FIELD, EVENT_TIME_FIELD, OPERATION_TYPE_FIELD,
            USER_IDENTITY_FIELD, PROJECT_INFORMATION_FIELD, REQUEST_ID_FIELD, REQUEST_PAYLOAD_FIELD, RESPONSE_PAYLOAD_FIELD,
            ERROR_CODE_FIELD, SOURCE_IP_ADDRESS_FIELD, USER_AGENT_FIELD));

    private static final long serialVersionUID = 1L;

    private final String id;

    private final String eventName;

    private final String eventType;

    private final String eventCategory;

    private final String eventSource;

    private final Instant eventTime;

    private final String operationType;

    private final UserIdentity userIdentity;

    private final ProjectInformation projectInformation;

    private final String requestIdValue;

    private final EventPayload requestPayload;

    private final EventPayload responsePayload;

    private final String errorCode;

    private final String sourceIpAddress;

    private final String userAgent;

    private EventLogEntry(BuilderImpl builder) {
        this.id = builder.id;
        this.eventName = builder.eventName;
        this.eventType = builder.eventType;
        this.eventCategory = builder.eventCategory;
        this.eventSource = builder.eventSource;
        this.eventTime = builder.eventTime;
        this.operationType = builder.operationType;
        this.userIdentity = builder.userIdentity;
        this.projectInformation = builder.projectInformation;
        this.requestIdValue = builder.requestIdValue;
        this.requestPayload = builder.requestPayload;
        this.responsePayload = builder.responsePayload;
        this.errorCode = builder.errorCode;
        this.sourceIpAddress = builder.sourceIpAddress;
        this.userAgent = builder.userAgent;
    }

    /**
     * <p>
     * The system-generated unique ID of the event.
     * </p>
     * 
     * @return The system-generated unique ID of the event.
     */
    public final String id() {
        return id;
    }

    /**
     * <p>
     * The name of the event.
     * </p>
     * 
     * @return The name of the event.
     */
    public final String eventName() {
        return eventName;
    }

    /**
     * <p>
     * The type of the event.
     * </p>
     * 
     * @return The type of the event.
     */
    public final String eventType() {
        return eventType;
    }

    /**
     * <p>
     * The category for the event.
     * </p>
     * 
     * @return The category for the event.
     */
    public final String eventCategory() {
        return eventCategory;
    }

    /**
     * <p>
     * The source of the event.
     * </p>
     * 
     * @return The source of the event.
     */
    public final String eventSource() {
        return eventSource;
    }

    /**
     * <p>
     * The time the event took place, in coordinated universal time (UTC) timestamp format as specified in <a
     * href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     * </p>
     * 
     * @return The time the event took place, in coordinated universal time (UTC) timestamp format as specified in <a
     *         href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     */
    public final Instant eventTime() {
        return eventTime;
    }

    /**
     * <p>
     * The type of the event.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #operationType}
     * will return {@link OperationType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #operationTypeAsString}.
     * </p>
     * 
     * @return The type of the event.
     * @see OperationType
     */
    public final OperationType operationType() {
        return OperationType.fromValue(operationType);
    }

    /**
     * <p>
     * The type of the event.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #operationType}
     * will return {@link OperationType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #operationTypeAsString}.
     * </p>
     * 
     * @return The type of the event.
     * @see OperationType
     */
    public final String operationTypeAsString() {
        return operationType;
    }

    /**
     * <p>
     * The system-generated unique ID of the user whose actions are recorded in the event.
     * </p>
     * 
     * @return The system-generated unique ID of the user whose actions are recorded in the event.
     */
    public final UserIdentity userIdentity() {
        return userIdentity;
    }

    /**
     * <p>
     * Information about the project where the event occurred.
     * </p>
     * 
     * @return Information about the project where the event occurred.
     */
    public final ProjectInformation projectInformation() {
        return projectInformation;
    }

    /**
     * <p>
     * The system-generated unique ID of the request.
     * </p>
     * 
     * @return The system-generated unique ID of the request.
     */
    public final String requestId() {
        return requestIdValue;
    }

    /**
     * <p>
     * Information about the payload of the request.
     * </p>
     * 
     * @return Information about the payload of the request.
     */
    public final EventPayload requestPayload() {
        return requestPayload;
    }

    /**
     * <p>
     * Information about the payload of the response, if any.
     * </p>
     * 
     * @return Information about the payload of the response, if any.
     */
    public final EventPayload responsePayload() {
        return responsePayload;
    }

    /**
     * <p>
     * The code of the error, if any.
     * </p>
     * 
     * @return The code of the error, if any.
     */
    public final String errorCode() {
        return errorCode;
    }

    /**
     * <p>
     * The IP address of the user whose actions are recorded in the event.
     * </p>
     * 
     * @return The IP address of the user whose actions are recorded in the event.
     */
    public final String sourceIpAddress() {
        return sourceIpAddress;
    }

    /**
     * <p/>
     * 
     * @return
     */
    public final String userAgent() {
        return userAgent;
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
        hashCode = 31 * hashCode + Objects.hashCode(id());
        hashCode = 31 * hashCode + Objects.hashCode(eventName());
        hashCode = 31 * hashCode + Objects.hashCode(eventType());
        hashCode = 31 * hashCode + Objects.hashCode(eventCategory());
        hashCode = 31 * hashCode + Objects.hashCode(eventSource());
        hashCode = 31 * hashCode + Objects.hashCode(eventTime());
        hashCode = 31 * hashCode + Objects.hashCode(operationTypeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(userIdentity());
        hashCode = 31 * hashCode + Objects.hashCode(projectInformation());
        hashCode = 31 * hashCode + Objects.hashCode(requestId());
        hashCode = 31 * hashCode + Objects.hashCode(requestPayload());
        hashCode = 31 * hashCode + Objects.hashCode(responsePayload());
        hashCode = 31 * hashCode + Objects.hashCode(errorCode());
        hashCode = 31 * hashCode + Objects.hashCode(sourceIpAddress());
        hashCode = 31 * hashCode + Objects.hashCode(userAgent());
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
        if (!(obj instanceof EventLogEntry)) {
            return false;
        }
        EventLogEntry other = (EventLogEntry) obj;
        return Objects.equals(id(), other.id()) && Objects.equals(eventName(), other.eventName())
                && Objects.equals(eventType(), other.eventType()) && Objects.equals(eventCategory(), other.eventCategory())
                && Objects.equals(eventSource(), other.eventSource()) && Objects.equals(eventTime(), other.eventTime())
                && Objects.equals(operationTypeAsString(), other.operationTypeAsString())
                && Objects.equals(userIdentity(), other.userIdentity())
                && Objects.equals(projectInformation(), other.projectInformation())
                && Objects.equals(requestId(), other.requestId()) && Objects.equals(requestPayload(), other.requestPayload())
                && Objects.equals(responsePayload(), other.responsePayload()) && Objects.equals(errorCode(), other.errorCode())
                && Objects.equals(sourceIpAddress(), other.sourceIpAddress()) && Objects.equals(userAgent(), other.userAgent());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("EventLogEntry").add("Id", id()).add("EventName", eventName()).add("EventType", eventType())
                .add("EventCategory", eventCategory()).add("EventSource", eventSource()).add("EventTime", eventTime())
                .add("OperationType", operationTypeAsString()).add("UserIdentity", userIdentity())
                .add("ProjectInformation", projectInformation()).add("RequestId", requestId())
                .add("RequestPayload", requestPayload()).add("ResponsePayload", responsePayload()).add("ErrorCode", errorCode())
                .add("SourceIpAddress", sourceIpAddress()).add("UserAgent", userAgent()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "id":
            return Optional.ofNullable(clazz.cast(id()));
        case "eventName":
            return Optional.ofNullable(clazz.cast(eventName()));
        case "eventType":
            return Optional.ofNullable(clazz.cast(eventType()));
        case "eventCategory":
            return Optional.ofNullable(clazz.cast(eventCategory()));
        case "eventSource":
            return Optional.ofNullable(clazz.cast(eventSource()));
        case "eventTime":
            return Optional.ofNullable(clazz.cast(eventTime()));
        case "operationType":
            return Optional.ofNullable(clazz.cast(operationTypeAsString()));
        case "userIdentity":
            return Optional.ofNullable(clazz.cast(userIdentity()));
        case "projectInformation":
            return Optional.ofNullable(clazz.cast(projectInformation()));
        case "requestId":
            return Optional.ofNullable(clazz.cast(requestId()));
        case "requestPayload":
            return Optional.ofNullable(clazz.cast(requestPayload()));
        case "responsePayload":
            return Optional.ofNullable(clazz.cast(responsePayload()));
        case "errorCode":
            return Optional.ofNullable(clazz.cast(errorCode()));
        case "sourceIpAddress":
            return Optional.ofNullable(clazz.cast(sourceIpAddress()));
        case "userAgent":
            return Optional.ofNullable(clazz.cast(userAgent()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<EventLogEntry, T> g) {
        return obj -> g.apply((EventLogEntry) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, EventLogEntry> {
        /**
         * <p>
         * The system-generated unique ID of the event.
         * </p>
         * 
         * @param id
         *        The system-generated unique ID of the event.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder id(String id);

        /**
         * <p>
         * The name of the event.
         * </p>
         * 
         * @param eventName
         *        The name of the event.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder eventName(String eventName);

        /**
         * <p>
         * The type of the event.
         * </p>
         * 
         * @param eventType
         *        The type of the event.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder eventType(String eventType);

        /**
         * <p>
         * The category for the event.
         * </p>
         * 
         * @param eventCategory
         *        The category for the event.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder eventCategory(String eventCategory);

        /**
         * <p>
         * The source of the event.
         * </p>
         * 
         * @param eventSource
         *        The source of the event.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder eventSource(String eventSource);

        /**
         * <p>
         * The time the event took place, in coordinated universal time (UTC) timestamp format as specified in <a
         * href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * </p>
         * 
         * @param eventTime
         *        The time the event took place, in coordinated universal time (UTC) timestamp format as specified in <a
         *        href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder eventTime(Instant eventTime);

        /**
         * <p>
         * The type of the event.
         * </p>
         * 
         * @param operationType
         *        The type of the event.
         * @see OperationType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see OperationType
         */
        Builder operationType(String operationType);

        /**
         * <p>
         * The type of the event.
         * </p>
         * 
         * @param operationType
         *        The type of the event.
         * @see OperationType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see OperationType
         */
        Builder operationType(OperationType operationType);

        /**
         * <p>
         * The system-generated unique ID of the user whose actions are recorded in the event.
         * </p>
         * 
         * @param userIdentity
         *        The system-generated unique ID of the user whose actions are recorded in the event.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder userIdentity(UserIdentity userIdentity);

        /**
         * <p>
         * The system-generated unique ID of the user whose actions are recorded in the event.
         * </p>
         * This is a convenience method that creates an instance of the {@link UserIdentity.Builder} avoiding the need
         * to create one manually via {@link UserIdentity#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link UserIdentity.Builder#build()} is called immediately and its
         * result is passed to {@link #userIdentity(UserIdentity)}.
         * 
         * @param userIdentity
         *        a consumer that will call methods on {@link UserIdentity.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #userIdentity(UserIdentity)
         */
        default Builder userIdentity(Consumer<UserIdentity.Builder> userIdentity) {
            return userIdentity(UserIdentity.builder().applyMutation(userIdentity).build());
        }

        /**
         * <p>
         * Information about the project where the event occurred.
         * </p>
         * 
         * @param projectInformation
         *        Information about the project where the event occurred.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder projectInformation(ProjectInformation projectInformation);

        /**
         * <p>
         * Information about the project where the event occurred.
         * </p>
         * This is a convenience method that creates an instance of the {@link ProjectInformation.Builder} avoiding the
         * need to create one manually via {@link ProjectInformation#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link ProjectInformation.Builder#build()} is called immediately and its
         * result is passed to {@link #projectInformation(ProjectInformation)}.
         * 
         * @param projectInformation
         *        a consumer that will call methods on {@link ProjectInformation.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #projectInformation(ProjectInformation)
         */
        default Builder projectInformation(Consumer<ProjectInformation.Builder> projectInformation) {
            return projectInformation(ProjectInformation.builder().applyMutation(projectInformation).build());
        }

        /**
         * <p>
         * The system-generated unique ID of the request.
         * </p>
         * 
         * @param requestIdValue
         *        The system-generated unique ID of the request.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder requestId(String requestIdValue);

        /**
         * <p>
         * Information about the payload of the request.
         * </p>
         * 
         * @param requestPayload
         *        Information about the payload of the request.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder requestPayload(EventPayload requestPayload);

        /**
         * <p>
         * Information about the payload of the request.
         * </p>
         * This is a convenience method that creates an instance of the {@link EventPayload.Builder} avoiding the need
         * to create one manually via {@link EventPayload#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link EventPayload.Builder#build()} is called immediately and its
         * result is passed to {@link #requestPayload(EventPayload)}.
         * 
         * @param requestPayload
         *        a consumer that will call methods on {@link EventPayload.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #requestPayload(EventPayload)
         */
        default Builder requestPayload(Consumer<EventPayload.Builder> requestPayload) {
            return requestPayload(EventPayload.builder().applyMutation(requestPayload).build());
        }

        /**
         * <p>
         * Information about the payload of the response, if any.
         * </p>
         * 
         * @param responsePayload
         *        Information about the payload of the response, if any.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder responsePayload(EventPayload responsePayload);

        /**
         * <p>
         * Information about the payload of the response, if any.
         * </p>
         * This is a convenience method that creates an instance of the {@link EventPayload.Builder} avoiding the need
         * to create one manually via {@link EventPayload#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link EventPayload.Builder#build()} is called immediately and its
         * result is passed to {@link #responsePayload(EventPayload)}.
         * 
         * @param responsePayload
         *        a consumer that will call methods on {@link EventPayload.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #responsePayload(EventPayload)
         */
        default Builder responsePayload(Consumer<EventPayload.Builder> responsePayload) {
            return responsePayload(EventPayload.builder().applyMutation(responsePayload).build());
        }

        /**
         * <p>
         * The code of the error, if any.
         * </p>
         * 
         * @param errorCode
         *        The code of the error, if any.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder errorCode(String errorCode);

        /**
         * <p>
         * The IP address of the user whose actions are recorded in the event.
         * </p>
         * 
         * @param sourceIpAddress
         *        The IP address of the user whose actions are recorded in the event.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder sourceIpAddress(String sourceIpAddress);

        /**
         * <p/>
         * 
         * @param userAgent
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder userAgent(String userAgent);
    }

    static final class BuilderImpl implements Builder {
        private String id;

        private String eventName;

        private String eventType;

        private String eventCategory;

        private String eventSource;

        private Instant eventTime;

        private String operationType;

        private UserIdentity userIdentity;

        private ProjectInformation projectInformation;

        private String requestIdValue;

        private EventPayload requestPayload;

        private EventPayload responsePayload;

        private String errorCode;

        private String sourceIpAddress;

        private String userAgent;

        private BuilderImpl() {
        }

        private BuilderImpl(EventLogEntry model) {
            id(model.id);
            eventName(model.eventName);
            eventType(model.eventType);
            eventCategory(model.eventCategory);
            eventSource(model.eventSource);
            eventTime(model.eventTime);
            operationType(model.operationType);
            userIdentity(model.userIdentity);
            projectInformation(model.projectInformation);
            requestId(model.requestIdValue);
            requestPayload(model.requestPayload);
            responsePayload(model.responsePayload);
            errorCode(model.errorCode);
            sourceIpAddress(model.sourceIpAddress);
            userAgent(model.userAgent);
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

        public final String getEventName() {
            return eventName;
        }

        public final void setEventName(String eventName) {
            this.eventName = eventName;
        }

        @Override
        public final Builder eventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public final String getEventType() {
            return eventType;
        }

        public final void setEventType(String eventType) {
            this.eventType = eventType;
        }

        @Override
        public final Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public final String getEventCategory() {
            return eventCategory;
        }

        public final void setEventCategory(String eventCategory) {
            this.eventCategory = eventCategory;
        }

        @Override
        public final Builder eventCategory(String eventCategory) {
            this.eventCategory = eventCategory;
            return this;
        }

        public final String getEventSource() {
            return eventSource;
        }

        public final void setEventSource(String eventSource) {
            this.eventSource = eventSource;
        }

        @Override
        public final Builder eventSource(String eventSource) {
            this.eventSource = eventSource;
            return this;
        }

        public final Instant getEventTime() {
            return eventTime;
        }

        public final void setEventTime(Instant eventTime) {
            this.eventTime = eventTime;
        }

        @Override
        public final Builder eventTime(Instant eventTime) {
            this.eventTime = eventTime;
            return this;
        }

        public final String getOperationType() {
            return operationType;
        }

        public final void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        @Override
        public final Builder operationType(String operationType) {
            this.operationType = operationType;
            return this;
        }

        @Override
        public final Builder operationType(OperationType operationType) {
            this.operationType(operationType == null ? null : operationType.toString());
            return this;
        }

        public final UserIdentity.Builder getUserIdentity() {
            return userIdentity != null ? userIdentity.toBuilder() : null;
        }

        public final void setUserIdentity(UserIdentity.BuilderImpl userIdentity) {
            this.userIdentity = userIdentity != null ? userIdentity.build() : null;
        }

        @Override
        public final Builder userIdentity(UserIdentity userIdentity) {
            this.userIdentity = userIdentity;
            return this;
        }

        public final ProjectInformation.Builder getProjectInformation() {
            return projectInformation != null ? projectInformation.toBuilder() : null;
        }

        public final void setProjectInformation(ProjectInformation.BuilderImpl projectInformation) {
            this.projectInformation = projectInformation != null ? projectInformation.build() : null;
        }

        @Override
        public final Builder projectInformation(ProjectInformation projectInformation) {
            this.projectInformation = projectInformation;
            return this;
        }

        public final String getRequestId() {
            return requestIdValue;
        }

        public final void setRequestId(String requestIdValue) {
            this.requestIdValue = requestIdValue;
        }

        @Override
        public final Builder requestId(String requestIdValue) {
            this.requestIdValue = requestIdValue;
            return this;
        }

        public final EventPayload.Builder getRequestPayload() {
            return requestPayload != null ? requestPayload.toBuilder() : null;
        }

        public final void setRequestPayload(EventPayload.BuilderImpl requestPayload) {
            this.requestPayload = requestPayload != null ? requestPayload.build() : null;
        }

        @Override
        public final Builder requestPayload(EventPayload requestPayload) {
            this.requestPayload = requestPayload;
            return this;
        }

        public final EventPayload.Builder getResponsePayload() {
            return responsePayload != null ? responsePayload.toBuilder() : null;
        }

        public final void setResponsePayload(EventPayload.BuilderImpl responsePayload) {
            this.responsePayload = responsePayload != null ? responsePayload.build() : null;
        }

        @Override
        public final Builder responsePayload(EventPayload responsePayload) {
            this.responsePayload = responsePayload;
            return this;
        }

        public final String getErrorCode() {
            return errorCode;
        }

        public final void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        @Override
        public final Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public final String getSourceIpAddress() {
            return sourceIpAddress;
        }

        public final void setSourceIpAddress(String sourceIpAddress) {
            this.sourceIpAddress = sourceIpAddress;
        }

        @Override
        public final Builder sourceIpAddress(String sourceIpAddress) {
            this.sourceIpAddress = sourceIpAddress;
            return this;
        }

        public final String getUserAgent() {
            return userAgent;
        }

        public final void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public final Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        @Override
        public EventLogEntry build() {
            return new EventLogEntry(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
