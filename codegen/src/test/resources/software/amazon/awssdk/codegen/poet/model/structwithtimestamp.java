package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.time.Instant;
import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.StructWithTimestampMarshaller;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class StructWithTimestamp implements StructuredPojo, ToCopyableBuilder<StructWithTimestamp.Builder, StructWithTimestamp> {
    private final Instant nestedTimestamp;

    private StructWithTimestamp(BuilderImpl builder) {
        this.nestedTimestamp = builder.nestedTimestamp;
    }

    /**
     * Returns the value of the NestedTimestamp property for this object.
     *
     * @return The value of the NestedTimestamp property for this object.
     */
    public Instant nestedTimestamp() {
        return nestedTimestamp;
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
        hashCode = 31 * hashCode + ((nestedTimestamp() == null) ? 0 : nestedTimestamp().hashCode());
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
        if (!(obj instanceof StructWithTimestamp)) {
            return false;
        }
        StructWithTimestamp other = (StructWithTimestamp) obj;
        if (other.nestedTimestamp() == null ^ this.nestedTimestamp() == null) {
            return false;
        }
        if (other.nestedTimestamp() != null && !other.nestedTimestamp().equals(this.nestedTimestamp())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (nestedTimestamp() != null) {
            sb.append("NestedTimestamp: ").append(nestedTimestamp()).append(",");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        StructWithTimestampMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, StructWithTimestamp> {
        /**
         * Sets the value of the NestedTimestamp property for this object.
         *
         * @param nestedTimestamp
         *        The new value for the NestedTimestamp property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedTimestamp(Instant nestedTimestamp);
    }

    private static final class BuilderImpl implements Builder {
        private Instant nestedTimestamp;

        private BuilderImpl() {
        }

        private BuilderImpl(StructWithTimestamp model) {
            setNestedTimestamp(model.nestedTimestamp);
        }

        public final Instant getNestedTimestamp() {
            return nestedTimestamp;
        }

        @Override
        public final Builder nestedTimestamp(Instant nestedTimestamp) {
            this.nestedTimestamp = nestedTimestamp;
            return this;
        }

        public final void setNestedTimestamp(Instant nestedTimestamp) {
            this.nestedTimestamp = nestedTimestamp;
        }

        @Override
        public StructWithTimestamp build() {
            return new StructWithTimestamp(this);
        }
    }
}

