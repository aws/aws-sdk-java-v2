package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Date;
import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.runtime.StandardMemberCopier;
import software.amazon.awssdk.services.jsonprotocoltests.transform.StructWithTimestampMarshaller;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class StructWithTimestamp implements StructuredPojo, ToCopyableBuilder<StructWithTimestamp.Builder, StructWithTimestamp> {
    private final Date nestedTimestamp;

    private StructWithTimestamp(BuilderImpl builder) {
        this.nestedTimestamp = builder.nestedTimestamp;
    }

    /**
     *
     * @return
     */
    public Date nestedTimestamp() {
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
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (nestedTimestamp() != null) {
            sb.append("NestedTimestamp: ").append(nestedTimestamp()).append(",");
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
         *
         * @param nestedTimestamp
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedTimestamp(Date nestedTimestamp);
    }

    private static final class BuilderImpl implements Builder {
        private Date nestedTimestamp;

        private BuilderImpl() {
        }

        private BuilderImpl(StructWithTimestamp model) {
            setNestedTimestamp(model.nestedTimestamp);
        }

        public final Date getNestedTimestamp() {
            return nestedTimestamp;
        }

        @Override
        public final Builder nestedTimestamp(Date nestedTimestamp) {
            this.nestedTimestamp = StandardMemberCopier.copy(nestedTimestamp);
            return this;
        }

        public final void setNestedTimestamp(Date nestedTimestamp) {
            this.nestedTimestamp = StandardMemberCopier.copy(nestedTimestamp);
        }

        @Override
        public StructWithTimestamp build() {
            return new StructWithTimestamp(this);
        }
    }
}
