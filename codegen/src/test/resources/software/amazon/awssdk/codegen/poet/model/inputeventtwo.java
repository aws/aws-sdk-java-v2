package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkEventType;
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
public class InputEventTwo implements SdkPojo, Serializable, ToCopyableBuilder<InputEventTwo.Builder, InputEventTwo>,
                                      InputEventStreamTwo {
    private static final SdkField<SdkBytes> IMPLICIT_PAYLOAD_MEMBER_ONE_FIELD = SdkField
        .<SdkBytes> builder(MarshallingType.SDK_BYTES).memberName("ImplicitPayloadMemberOne")
        .getter(getter(InputEventTwo::implicitPayloadMemberOne)).setter(setter(Builder::implicitPayloadMemberOne))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ImplicitPayloadMemberOne").build())
        .build();

    private static final SdkField<String> IMPLICIT_PAYLOAD_MEMBER_TWO_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                                      .memberName("ImplicitPayloadMemberTwo").getter(getter(InputEventTwo::implicitPayloadMemberTwo))
                                                                                      .setter(setter(Builder::implicitPayloadMemberTwo))
                                                                                      .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ImplicitPayloadMemberTwo").build())
                                                                                      .build();

    private static final SdkField<String> EVENT_HEADER_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                              .memberName("EventHeaderMember").getter(getter(InputEventTwo::eventHeaderMember))
                                                                              .setter(setter(Builder::eventHeaderMember))
                                                                              .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("EventHeaderMember").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(
        IMPLICIT_PAYLOAD_MEMBER_ONE_FIELD, IMPLICIT_PAYLOAD_MEMBER_TWO_FIELD, EVENT_HEADER_MEMBER_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private static final long serialVersionUID = 1L;

    private final SdkBytes implicitPayloadMemberOne;

    private final String implicitPayloadMemberTwo;

    private final String eventHeaderMember;

    protected InputEventTwo(BuilderImpl builder) {
        this.implicitPayloadMemberOne = builder.implicitPayloadMemberOne;
        this.implicitPayloadMemberTwo = builder.implicitPayloadMemberTwo;
        this.eventHeaderMember = builder.eventHeaderMember;
    }

    /**
     * Returns the value of the ImplicitPayloadMemberOne property for this object.
     *
     * @return The value of the ImplicitPayloadMemberOne property for this object.
     */
    public final SdkBytes implicitPayloadMemberOne() {
        return implicitPayloadMemberOne;
    }

    /**
     * Returns the value of the ImplicitPayloadMemberTwo property for this object.
     *
     * @return The value of the ImplicitPayloadMemberTwo property for this object.
     */
    public final String implicitPayloadMemberTwo() {
        return implicitPayloadMemberTwo;
    }

    /**
     * Returns the value of the EventHeaderMember property for this object.
     *
     * @return The value of the EventHeaderMember property for this object.
     */
    public final String eventHeaderMember() {
        return eventHeaderMember;
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
        hashCode = 31 * hashCode + Objects.hashCode(implicitPayloadMemberOne());
        hashCode = 31 * hashCode + Objects.hashCode(implicitPayloadMemberTwo());
        hashCode = 31 * hashCode + Objects.hashCode(eventHeaderMember());
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
        if (!(obj instanceof InputEventTwo)) {
            return false;
        }
        InputEventTwo other = (InputEventTwo) obj;
        return Objects.equals(implicitPayloadMemberOne(), other.implicitPayloadMemberOne())
               && Objects.equals(implicitPayloadMemberTwo(), other.implicitPayloadMemberTwo())
               && Objects.equals(eventHeaderMember(), other.eventHeaderMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("InputEventTwo").add("ImplicitPayloadMemberOne", implicitPayloadMemberOne())
                       .add("ImplicitPayloadMemberTwo", implicitPayloadMemberTwo()).add("EventHeaderMember", eventHeaderMember())
                       .build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "ImplicitPayloadMemberOne":
                return Optional.ofNullable(clazz.cast(implicitPayloadMemberOne()));
            case "ImplicitPayloadMemberTwo":
                return Optional.ofNullable(clazz.cast(implicitPayloadMemberTwo()));
            case "EventHeaderMember":
                return Optional.ofNullable(clazz.cast(eventHeaderMember()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final InputEventTwo copy(Consumer<? super Builder> modifier) {
        return ToCopyableBuilder.super.copy(modifier);
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
        map.put("ImplicitPayloadMemberOne", IMPLICIT_PAYLOAD_MEMBER_ONE_FIELD);
        map.put("ImplicitPayloadMemberTwo", IMPLICIT_PAYLOAD_MEMBER_TWO_FIELD);
        map.put("EventHeaderMember", EVENT_HEADER_MEMBER_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<InputEventTwo, T> g) {
        return obj -> g.apply((InputEventTwo) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Override
    public SdkEventType sdkEventType() {
        throw new UnsupportedOperationException("Unknown Event");
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, InputEventTwo> {
        /**
         * Sets the value of the ImplicitPayloadMemberOne property for this object.
         *
         * @param implicitPayloadMemberOne
         *        The new value for the ImplicitPayloadMemberOne property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder implicitPayloadMemberOne(SdkBytes implicitPayloadMemberOne);

        /**
         * Sets the value of the ImplicitPayloadMemberTwo property for this object.
         *
         * @param implicitPayloadMemberTwo
         *        The new value for the ImplicitPayloadMemberTwo property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder implicitPayloadMemberTwo(String implicitPayloadMemberTwo);

        /**
         * Sets the value of the EventHeaderMember property for this object.
         *
         * @param eventHeaderMember
         *        The new value for the EventHeaderMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder eventHeaderMember(String eventHeaderMember);
    }

    protected static class BuilderImpl implements Builder {
        private SdkBytes implicitPayloadMemberOne;

        private String implicitPayloadMemberTwo;

        private String eventHeaderMember;

        protected BuilderImpl() {
        }

        protected BuilderImpl(InputEventTwo model) {
            implicitPayloadMemberOne(model.implicitPayloadMemberOne);
            implicitPayloadMemberTwo(model.implicitPayloadMemberTwo);
            eventHeaderMember(model.eventHeaderMember);
        }

        public final ByteBuffer getImplicitPayloadMemberOne() {
            return implicitPayloadMemberOne == null ? null : implicitPayloadMemberOne.asByteBuffer();
        }

        public final void setImplicitPayloadMemberOne(ByteBuffer implicitPayloadMemberOne) {
            implicitPayloadMemberOne(implicitPayloadMemberOne == null ? null : SdkBytes.fromByteBuffer(implicitPayloadMemberOne));
        }

        @Override
        public final Builder implicitPayloadMemberOne(SdkBytes implicitPayloadMemberOne) {
            this.implicitPayloadMemberOne = implicitPayloadMemberOne;
            return this;
        }

        public final String getImplicitPayloadMemberTwo() {
            return implicitPayloadMemberTwo;
        }

        public final void setImplicitPayloadMemberTwo(String implicitPayloadMemberTwo) {
            this.implicitPayloadMemberTwo = implicitPayloadMemberTwo;
        }

        @Override
        public final Builder implicitPayloadMemberTwo(String implicitPayloadMemberTwo) {
            this.implicitPayloadMemberTwo = implicitPayloadMemberTwo;
            return this;
        }

        public final String getEventHeaderMember() {
            return eventHeaderMember;
        }

        public final void setEventHeaderMember(String eventHeaderMember) {
            this.eventHeaderMember = eventHeaderMember;
        }

        @Override
        public final Builder eventHeaderMember(String eventHeaderMember) {
            this.eventHeaderMember = eventHeaderMember;
            return this;
        }

        @Override
        public InputEventTwo build() {
            return new InputEventTwo(this);
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
