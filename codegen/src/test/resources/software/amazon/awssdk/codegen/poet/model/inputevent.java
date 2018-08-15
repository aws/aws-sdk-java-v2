package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.adapter.StandardMemberCopier;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.InputEventMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class InputEvent implements StructuredPojo, ToCopyableBuilder<InputEvent.Builder, InputEvent>, InputEventStream {
    private final SdkBytes explicitPayloadMember;

    private InputEvent(BuilderImpl builder) {
        this.explicitPayloadMember = builder.explicitPayloadMember;
    }

    /**
     * Returns the value of the ExplicitPayloadMember property for this object.
     *
     * @return The value of the ExplicitPayloadMember property for this object.
     */
    public SdkBytes explicitPayloadMember() {
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
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(explicitPayloadMember());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
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

    @Override
    public String toString() {
        return ToString.builder("InputEvent").add("ExplicitPayloadMember", explicitPayloadMember()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "ExplicitPayloadMember":
                return Optional.ofNullable(clazz.cast(explicitPayloadMember()));
            default:
                return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        InputEventMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, InputEvent> {
        /**
         * Sets the value of the ExplicitPayloadMember property for this object.
         *
         * @param explicitPayloadMember
         *        The new value for the ExplicitPayloadMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder explicitPayloadMember(SdkBytes explicitPayloadMember);
    }

    static final class BuilderImpl implements Builder {
        private SdkBytes explicitPayloadMember;

        private BuilderImpl() {
        }

        private BuilderImpl(InputEvent model) {
            explicitPayloadMember(model.explicitPayloadMember);
        }

        public final ByteBuffer getExplicitPayloadMember() {
            return explicitPayloadMember == null ? null : explicitPayloadMember.asByteBuffer();
        }

        @Override
        public final Builder explicitPayloadMember(SdkBytes explicitPayloadMember) {
            this.explicitPayloadMember = StandardMemberCopier.copy(explicitPayloadMember);
            return this;
        }

        public final void setExplicitPayloadMember(ByteBuffer explicitPayloadMember) {
            explicitPayloadMember(explicitPayloadMember == null ? null : SdkBytes.fromByteBuffer(explicitPayloadMember));
        }

        @Override
        public InputEvent build() {
            return new InputEvent(this);
        }
    }
}
