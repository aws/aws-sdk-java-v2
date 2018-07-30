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
import software.amazon.awssdk.services.jsonprotocoltests.transform.StructWithNestedBlobTypeMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class StructWithNestedBlobType implements StructuredPojo,
        ToCopyableBuilder<StructWithNestedBlobType.Builder, StructWithNestedBlobType> {
    private final SdkBytes nestedBlob;

    private StructWithNestedBlobType(BuilderImpl builder) {
        this.nestedBlob = builder.nestedBlob;
    }

    /**
     * Returns the value of the NestedBlob property for this object.
     *
     * @return The value of the NestedBlob property for this object.
     */
    public SdkBytes nestedBlob() {
        return nestedBlob;
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
        hashCode = 31 * hashCode + Objects.hashCode(nestedBlob());
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
        if (!(obj instanceof StructWithNestedBlobType)) {
            return false;
        }
        StructWithNestedBlobType other = (StructWithNestedBlobType) obj;
        return Objects.equals(nestedBlob(), other.nestedBlob());
    }

    @Override
    public String toString() {
        return ToString.builder("StructWithNestedBlobType").add("NestedBlob", nestedBlob()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "NestedBlob":
            return Optional.ofNullable(clazz.cast(nestedBlob()));
        default:
            return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        StructWithNestedBlobTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, StructWithNestedBlobType> {
        /**
         * Sets the value of the NestedBlob property for this object.
         *
         * @param nestedBlob
         *        The new value for the NestedBlob property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedBlob(SdkBytes nestedBlob);
    }

    static final class BuilderImpl implements Builder {
        private SdkBytes nestedBlob;

        private BuilderImpl() {
        }

        private BuilderImpl(StructWithNestedBlobType model) {
            nestedBlob(model.nestedBlob);
        }

        public final ByteBuffer getNestedBlob() {
            return nestedBlob == null ? null : nestedBlob.asByteBuffer();
        }

        @Override
        public final Builder nestedBlob(SdkBytes nestedBlob) {
            this.nestedBlob = StandardMemberCopier.copy(nestedBlob);
            return this;
        }

        public final void setNestedBlob(ByteBuffer nestedBlob) {
            nestedBlob(nestedBlob == null ? null : SdkBytes.fromByteBuffer(nestedBlob));
        }

        @Override
        public StructWithNestedBlobType build() {
            return new StructWithNestedBlobType(this);
        }
    }
}
