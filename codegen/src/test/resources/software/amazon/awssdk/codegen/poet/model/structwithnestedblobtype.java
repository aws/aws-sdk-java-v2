package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.core.runtime.StandardMemberCopier;
import software.amazon.awssdk.services.jsonprotocoltests.transform.StructWithNestedBlobTypeMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class StructWithNestedBlobType implements StructuredPojo,
                                                       ToCopyableBuilder<StructWithNestedBlobType.Builder,
                                                           StructWithNestedBlobType> {
    private final ByteBuffer nestedBlob;

    private StructWithNestedBlobType(BuilderImpl builder) {
        this.nestedBlob = builder.nestedBlob;
    }

    /**
     * Returns the value of the NestedBlob property for this object.
     * <p>
     * This method will return a new read-only {@code ByteBuffer} each time it is invoked.
     * </p>
     *
     * @return The value of the NestedBlob property for this object.
     */
    public ByteBuffer nestedBlob() {
        return nestedBlob == null ? null : nestedBlob.asReadOnlyBuffer();
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
                return Optional.of(clazz.cast(nestedBlob()));
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
         * <p>
         * To preserve immutability, the remaining bytes in the provided buffer will be copied into a new buffer when
         * set.
         * </p>
         *
         * @param nestedBlob
         *        The new value for the NestedBlob property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedBlob(ByteBuffer nestedBlob);
    }

    static final class BuilderImpl implements Builder {
        private ByteBuffer nestedBlob;

        private BuilderImpl() {
        }

        private BuilderImpl(StructWithNestedBlobType model) {
            nestedBlob(model.nestedBlob);
        }

        public final ByteBuffer getNestedBlob() {
            return nestedBlob;
        }

        @Override
        public final Builder nestedBlob(ByteBuffer nestedBlob) {
            this.nestedBlob = StandardMemberCopier.copy(nestedBlob);
            return this;
        }

        public final void setNestedBlob(ByteBuffer nestedBlob) {
            this.nestedBlob = StandardMemberCopier.copy(nestedBlob);
        }

        @Override
        public StructWithNestedBlobType build() {
            return new StructWithNestedBlobType(this);
        }
    }
}
