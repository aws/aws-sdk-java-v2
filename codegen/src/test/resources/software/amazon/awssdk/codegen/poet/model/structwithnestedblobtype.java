package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.runtime.StandardMemberCopier;
import software.amazon.awssdk.services.jsonprotocoltests.transform.StructWithNestedBlobTypeMarshaller;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class StructWithNestedBlobType implements StructuredPojo,
        ToCopyableBuilder<StructWithNestedBlobType.Builder, StructWithNestedBlobType> {
    private final ByteBuffer nestedBlob;

    private StructWithNestedBlobType(BuilderImpl builder) {
        this.nestedBlob = builder.nestedBlob;
    }

    /**
     *
     * <p>
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}.
     * </p>
     * 
     * @return
     */
    public ByteBuffer nestedBlob() {
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
        hashCode = 31 * hashCode + ((nestedBlob() == null) ? 0 : nestedBlob().hashCode());
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
        if (other.nestedBlob() == null ^ this.nestedBlob() == null) {
            return false;
        }
        if (other.nestedBlob() != null && !other.nestedBlob().equals(this.nestedBlob())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (nestedBlob() != null) {
            sb.append("NestedBlob: ").append(nestedBlob()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        StructWithNestedBlobTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, StructWithNestedBlobType> {
        /**
         *
         * @param nestedBlob
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedBlob(ByteBuffer nestedBlob);
    }

    private static final class BuilderImpl implements Builder {
        private ByteBuffer nestedBlob;

        private BuilderImpl() {
        }

        private BuilderImpl(StructWithNestedBlobType model) {
            setNestedBlob(model.nestedBlob);
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
