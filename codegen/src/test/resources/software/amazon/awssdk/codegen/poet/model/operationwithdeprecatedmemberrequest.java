package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.core.traits.DataTypeConversionFailureHandlingTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class OperationWithDeprecatedMemberRequest extends JsonProtocolTestsRequest implements
        ToCopyableBuilder<OperationWithDeprecatedMemberRequest.Builder, OperationWithDeprecatedMemberRequest> {
    private static final SdkField<String> MEMBER_MODELED_AS_DEPRECATED_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("MemberModeledAsDeprecated")
            .getter(getter(OperationWithDeprecatedMemberRequest::memberModeledAsDeprecated))
            .setter(setter(Builder::memberModeledAsDeprecated))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MemberModeledAsDeprecated").build())
            .build();

    private static final SdkField<String> MEMBER_MODIFIED_AS_DEPRECATED_FIELD = SdkField
            .<String> builder(MarshallingType.STRING)
            .memberName("MemberModifiedAsDeprecated")
            .getter(getter(OperationWithDeprecatedMemberRequest::memberModifiedAsDeprecated))
            .setter(setter(Builder::memberModifiedAsDeprecated))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MemberModifiedAsDeprecated").build())
            .build();

    private static final SdkField<String> UNDEPRECATED_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("UndeprecatedMember").getter(getter(OperationWithDeprecatedMemberRequest::undeprecatedMember))
            .setter(setter(Builder::undeprecatedMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("UndeprecatedMember").build())
            .build();

    private static final SdkField<String> MEMBER_IGNORE_DATA_TYPE_FAILURE_HANDLING_FIELD = SdkField
            .<String> builder(MarshallingType.STRING)
            .memberName("MemberIgnoreDataTypeFailureHandling")
            .getter(getter(OperationWithDeprecatedMemberRequest::memberIgnoreDataTypeFailureHandling))
            .setter(setter(Builder::memberIgnoreDataTypeFailureHandling))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                    .locationName("MemberIgnoreDataTypeFailureHandling").build(), new DataTypeConversionFailureHandlingTrait())
            .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(
            MEMBER_MODELED_AS_DEPRECATED_FIELD, MEMBER_MODIFIED_AS_DEPRECATED_FIELD, UNDEPRECATED_MEMBER_FIELD,
            MEMBER_IGNORE_DATA_TYPE_FAILURE_HANDLING_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = Collections
            .unmodifiableMap(new HashMap<String, SdkField<?>>() {
                {
                    put("MemberModeledAsDeprecated", MEMBER_MODELED_AS_DEPRECATED_FIELD);
                    put("MemberModifiedAsDeprecated", MEMBER_MODIFIED_AS_DEPRECATED_FIELD);
                    put("UndeprecatedMember", UNDEPRECATED_MEMBER_FIELD);
                    put("MemberIgnoreDataTypeFailureHandling", MEMBER_IGNORE_DATA_TYPE_FAILURE_HANDLING_FIELD);
                }
            });

    private final String memberModeledAsDeprecated;

    private final String memberModifiedAsDeprecated;

    private final String undeprecatedMember;

    private final String memberIgnoreDataTypeFailureHandling;

    private OperationWithDeprecatedMemberRequest(BuilderImpl builder) {
        super(builder);
        this.memberModeledAsDeprecated = builder.memberModeledAsDeprecated;
        this.memberModifiedAsDeprecated = builder.memberModifiedAsDeprecated;
        this.undeprecatedMember = builder.undeprecatedMember;
        this.memberIgnoreDataTypeFailureHandling = builder.memberIgnoreDataTypeFailureHandling;
    }

    /**
     * Returns the value of the MemberModeledAsDeprecated property for this object.
     * 
     * @return The value of the MemberModeledAsDeprecated property for this object.
     * @deprecated This field is modeled as deprecated.
     */
    @Deprecated
    public final String memberModeledAsDeprecated() {
        return memberModeledAsDeprecated;
    }

    /**
     * Returns the value of the MemberModifiedAsDeprecated property for this object.
     * 
     * @return The value of the MemberModifiedAsDeprecated property for this object.
     * @deprecated This field is modified as deprecated.
     */
    @Deprecated
    public final String memberModifiedAsDeprecated() {
        return memberModifiedAsDeprecated;
    }

    /**
     * Returns the value of the UndeprecatedMember property for this object.
     * 
     * @return The value of the UndeprecatedMember property for this object.
     */
    public final String undeprecatedMember() {
        return undeprecatedMember;
    }

    /**
     * Returns the value of the MemberIgnoreDataTypeFailureHandling property for this object.
     * 
     * @return The value of the MemberIgnoreDataTypeFailureHandling property for this object.
     */
    public final String memberIgnoreDataTypeFailureHandling() {
        return memberIgnoreDataTypeFailureHandling;
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
        hashCode = 31 * hashCode + Objects.hashCode(memberModeledAsDeprecated());
        hashCode = 31 * hashCode + Objects.hashCode(memberModifiedAsDeprecated());
        hashCode = 31 * hashCode + Objects.hashCode(undeprecatedMember());
        hashCode = 31 * hashCode + Objects.hashCode(memberIgnoreDataTypeFailureHandling());
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
        if (!(obj instanceof OperationWithDeprecatedMemberRequest)) {
            return false;
        }
        OperationWithDeprecatedMemberRequest other = (OperationWithDeprecatedMemberRequest) obj;
        return Objects.equals(memberModeledAsDeprecated(), other.memberModeledAsDeprecated())
                && Objects.equals(memberModifiedAsDeprecated(), other.memberModifiedAsDeprecated())
                && Objects.equals(undeprecatedMember(), other.undeprecatedMember())
                && Objects.equals(memberIgnoreDataTypeFailureHandling(), other.memberIgnoreDataTypeFailureHandling());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("OperationWithDeprecatedMemberRequest")
                .add("MemberModeledAsDeprecated", memberModeledAsDeprecated())
                .add("MemberModifiedAsDeprecated", memberModifiedAsDeprecated()).add("UndeprecatedMember", undeprecatedMember())
                .add("MemberIgnoreDataTypeFailureHandling", memberIgnoreDataTypeFailureHandling()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "MemberModeledAsDeprecated":
            return Optional.ofNullable(clazz.cast(memberModeledAsDeprecated()));
        case "MemberModifiedAsDeprecated":
            return Optional.ofNullable(clazz.cast(memberModifiedAsDeprecated()));
        case "UndeprecatedMember":
            return Optional.ofNullable(clazz.cast(undeprecatedMember()));
        case "MemberIgnoreDataTypeFailureHandling":
            return Optional.ofNullable(clazz.cast(memberIgnoreDataTypeFailureHandling()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public final Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
    }

    private static <T> Function<Object, T> getter(Function<OperationWithDeprecatedMemberRequest, T> g) {
        return obj -> g.apply((OperationWithDeprecatedMemberRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends JsonProtocolTestsRequest.Builder, SdkPojo,
            CopyableBuilder<Builder, OperationWithDeprecatedMemberRequest> {
        /**
         * Sets the value of the MemberModeledAsDeprecated property for this object.
         *
         * @param memberModeledAsDeprecated
         *        The new value for the MemberModeledAsDeprecated property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         * @deprecated This field is modeled as deprecated.
         */
        @Deprecated
        Builder memberModeledAsDeprecated(String memberModeledAsDeprecated);

        /**
         * Sets the value of the MemberModifiedAsDeprecated property for this object.
         *
         * @param memberModifiedAsDeprecated
         *        The new value for the MemberModifiedAsDeprecated property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         * @deprecated This field is modified as deprecated.
         */
        @Deprecated
        Builder memberModifiedAsDeprecated(String memberModifiedAsDeprecated);

        /**
         * Sets the value of the UndeprecatedMember property for this object.
         *
         * @param undeprecatedMember
         *        The new value for the UndeprecatedMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder undeprecatedMember(String undeprecatedMember);

        /**
         * Sets the value of the MemberIgnoreDataTypeFailureHandling property for this object.
         *
         * @param memberIgnoreDataTypeFailureHandling
         *        The new value for the MemberIgnoreDataTypeFailureHandling property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder memberIgnoreDataTypeFailureHandling(String memberIgnoreDataTypeFailureHandling);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private String memberModeledAsDeprecated;

        private String memberModifiedAsDeprecated;

        private String undeprecatedMember;

        private String memberIgnoreDataTypeFailureHandling;

        private BuilderImpl() {
        }

        private BuilderImpl(OperationWithDeprecatedMemberRequest model) {
            super(model);
            memberModeledAsDeprecated(model.memberModeledAsDeprecated);
            memberModifiedAsDeprecated(model.memberModifiedAsDeprecated);
            undeprecatedMember(model.undeprecatedMember);
            memberIgnoreDataTypeFailureHandling(model.memberIgnoreDataTypeFailureHandling);
        }

        @Deprecated
        public final String getMemberModeledAsDeprecated() {
            return memberModeledAsDeprecated;
        }

        @Deprecated
        public final void setMemberModeledAsDeprecated(String memberModeledAsDeprecated) {
            this.memberModeledAsDeprecated = memberModeledAsDeprecated;
        }

        @Override
        @Deprecated
        public final Builder memberModeledAsDeprecated(String memberModeledAsDeprecated) {
            this.memberModeledAsDeprecated = memberModeledAsDeprecated;
            return this;
        }

        @Deprecated
        public final String getMemberModifiedAsDeprecated() {
            return memberModifiedAsDeprecated;
        }

        @Deprecated
        public final void setMemberModifiedAsDeprecated(String memberModifiedAsDeprecated) {
            this.memberModifiedAsDeprecated = memberModifiedAsDeprecated;
        }

        @Override
        @Deprecated
        public final Builder memberModifiedAsDeprecated(String memberModifiedAsDeprecated) {
            this.memberModifiedAsDeprecated = memberModifiedAsDeprecated;
            return this;
        }

        public final String getUndeprecatedMember() {
            return undeprecatedMember;
        }

        public final void setUndeprecatedMember(String undeprecatedMember) {
            this.undeprecatedMember = undeprecatedMember;
        }

        @Override
        public final Builder undeprecatedMember(String undeprecatedMember) {
            this.undeprecatedMember = undeprecatedMember;
            return this;
        }

        public final String getMemberIgnoreDataTypeFailureHandling() {
            return memberIgnoreDataTypeFailureHandling;
        }

        public final void setMemberIgnoreDataTypeFailureHandling(String memberIgnoreDataTypeFailureHandling) {
            this.memberIgnoreDataTypeFailureHandling = memberIgnoreDataTypeFailureHandling;
        }

        @Override
        public final Builder memberIgnoreDataTypeFailureHandling(String memberIgnoreDataTypeFailureHandling) {
            this.memberIgnoreDataTypeFailureHandling = memberIgnoreDataTypeFailureHandling;
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
        public OperationWithDeprecatedMemberRequest build() {
            return new OperationWithDeprecatedMemberRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return SDK_NAME_TO_FIELD;
        }
    }
}
