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
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
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
public final class OperationWithReservedKeywordMemberRequest extends JsonProtocolTestsRequest implements
                                                                                              ToCopyableBuilder<OperationWithReservedKeywordMemberRequest.Builder, OperationWithReservedKeywordMemberRequest> {
    private static final SdkField<ContainsReservedKeyword> RESERVED_KEYWORD_MEMBER_FIELD = SdkField
        .<ContainsReservedKeyword> builder(MarshallingType.SDK_POJO).memberName("ReservedKeywordMember")
        .getter(getter(OperationWithReservedKeywordMemberRequest::reservedKeywordMember))
        .setter(setter(Builder::reservedKeywordMember)).constructor(ContainsReservedKeyword::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ReservedKeywordMember").build())
        .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections
        .unmodifiableList(Arrays.asList(RESERVED_KEYWORD_MEMBER_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private final ContainsReservedKeyword reservedKeywordMember;

    private OperationWithReservedKeywordMemberRequest(BuilderImpl builder) {
        super(builder);
        this.reservedKeywordMember = builder.reservedKeywordMember;
    }

    /**
     * Returns the value of the ReservedKeywordMember property for this object.
     *
     * @return The value of the ReservedKeywordMember property for this object.
     */
    public final ContainsReservedKeyword reservedKeywordMember() {
        return reservedKeywordMember;
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
        hashCode = 31 * hashCode + Objects.hashCode(reservedKeywordMember());
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
        if (!(obj instanceof OperationWithReservedKeywordMemberRequest)) {
            return false;
        }
        OperationWithReservedKeywordMemberRequest other = (OperationWithReservedKeywordMemberRequest) obj;
        return Objects.equals(reservedKeywordMember(), other.reservedKeywordMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("OperationWithReservedKeywordMemberRequest")
                       .add("ReservedKeywordMember", reservedKeywordMember()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "ReservedKeywordMember":
                return Optional.ofNullable(clazz.cast(reservedKeywordMember()));
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

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("ReservedKeywordMember", RESERVED_KEYWORD_MEMBER_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<OperationWithReservedKeywordMemberRequest, T> g) {
        return obj -> g.apply((OperationWithReservedKeywordMemberRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends JsonProtocolTestsRequest.Builder, SdkPojo,
                                     CopyableBuilder<Builder, OperationWithReservedKeywordMemberRequest> {
        /**
         * Sets the value of the ReservedKeywordMember property for this object.
         *
         * @param reservedKeywordMember
         *        The new value for the ReservedKeywordMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder reservedKeywordMember(ContainsReservedKeyword reservedKeywordMember);

        /**
         * Sets the value of the ReservedKeywordMember property for this object.
         *
         * This is a convenience method that creates an instance of the {@link ContainsReservedKeyword.Builder} avoiding
         * the need to create one manually via {@link ContainsReservedKeyword#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link ContainsReservedKeyword.Builder#build()} is called immediately
         * and its result is passed to {@link #reservedKeywordMember(ContainsReservedKeyword)}.
         *
         * @param reservedKeywordMember
         *        a consumer that will call methods on {@link ContainsReservedKeyword.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #reservedKeywordMember(ContainsReservedKeyword)
         */
        default Builder reservedKeywordMember(Consumer<ContainsReservedKeyword.Builder> reservedKeywordMember) {
            return reservedKeywordMember(ContainsReservedKeyword.builder().applyMutation(reservedKeywordMember).build());
        }

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private ContainsReservedKeyword reservedKeywordMember;

        private BuilderImpl() {
        }

        private BuilderImpl(OperationWithReservedKeywordMemberRequest model) {
            super(model);
            reservedKeywordMember(model.reservedKeywordMember);
        }

        public final ContainsReservedKeyword.Builder getReservedKeywordMember() {
            return reservedKeywordMember != null ? reservedKeywordMember.toBuilder() : null;
        }

        public final void setReservedKeywordMember(ContainsReservedKeyword.BuilderImpl reservedKeywordMember) {
            this.reservedKeywordMember = reservedKeywordMember != null ? reservedKeywordMember.build() : null;
        }

        @Override
        public final Builder reservedKeywordMember(ContainsReservedKeyword reservedKeywordMember) {
            this.reservedKeywordMember = reservedKeywordMember;
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
        public OperationWithReservedKeywordMemberRequest build() {
            return new OperationWithReservedKeywordMemberRequest(this);
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
