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
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class InputEvent implements SdkPojo, Serializable, ToCopyableBuilder<InputEvent.Builder, InputEvent>, InputEventStream {
    private static final SdkField<SdkBytes> EXPLICIT_PAYLOAD_MEMBER_FIELD = SdkField
        .<SdkBytes> builder(MarshallingType.SDK_BYTES)
        .memberName("ExplicitPayloadMember")
        .getter(getter(InputEvent::explicitPayloadMember))
        .setter(setter(Builder::explicitPayloadMember))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ExplicitPayloadMember").build(),
                PayloadTrait.create()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections
        .unmodifiableList(Arrays.asList(EXPLICIT_PAYLOAD_MEMBER_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private static final long serialVersionUID = 1L;

    private final SdkBytes explicitPayloadMember;

    protected InputEvent(BuilderImpl builder) {
        this.explicitPayloadMember = builder.explicitPayloadMember;
    }

    /**
     * Returns the value of the ExplicitPayloadMember property for this object.
     *
     * @return The value of the ExplicitPayloadMember property for this object.
     */
    public final SdkBytes explicitPayloadMember() {
        return explicitPayloadMember;
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
        hashCode = 31 * hashCode + Objects.hashCode(explicitPayloadMember());
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
        if (!(obj instanceof InputEvent)) {
            return false;
        }
        InputEvent other = (InputEvent) obj;
        return Objects.equals(explicitPayloadMember(), other.explicitPayloadMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("InputEvent").add("ExplicitPayloadMember", explicitPayloadMember()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "ExplicitPayloadMember":
                return Optional.ofNullable(clazz.cast(explicitPayloadMember()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final InputEvent copy(Consumer<? super Builder> modifier) {
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
        map.put("ExplicitPayloadMember", EXPLICIT_PAYLOAD_MEMBER_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<InputEvent, T> g) {
        return obj -> g.apply((InputEvent) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Override
    public SdkEventType sdkEventType() {
        throw new UnsupportedOperationException("Unknown Event");
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, InputEvent> {
        /**
         * Sets the value of the ExplicitPayloadMember property for this object.
         *
         * @param explicitPayloadMember
         *        The new value for the ExplicitPayloadMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder explicitPayloadMember(SdkBytes explicitPayloadMember);
    }

    protected static class BuilderImpl implements Builder {
        private SdkBytes explicitPayloadMember;

        protected BuilderImpl() {
        }

        protected BuilderImpl(InputEvent model) {
            explicitPayloadMember(model.explicitPayloadMember);
        }

        public final ByteBuffer getExplicitPayloadMember() {
            return explicitPayloadMember == null ? null : explicitPayloadMember.asByteBuffer();
        }

        public final void setExplicitPayloadMember(ByteBuffer explicitPayloadMember) {
            explicitPayloadMember(explicitPayloadMember == null ? null : SdkBytes.fromByteBuffer(explicitPayloadMember));
        }

        @Override
        public final Builder explicitPayloadMember(SdkBytes explicitPayloadMember) {
            this.explicitPayloadMember = explicitPayloadMember;
            return this;
        }

        @Override
        public InputEvent build() {
            return new InputEvent(this);
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
