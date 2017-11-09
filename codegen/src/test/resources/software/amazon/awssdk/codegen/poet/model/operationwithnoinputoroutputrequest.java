package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Optional;
import javax.annotation.Generated;
import software.amazon.awssdk.core.AmazonWebServiceRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public class OperationWithNoInputOrOutputRequest extends AmazonWebServiceRequest implements
        ToCopyableBuilder<OperationWithNoInputOrOutputRequest.Builder, OperationWithNoInputOrOutputRequest> {
    private OperationWithNoInputOrOutputRequest(BuilderImpl builder) {
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
        if (!(obj instanceof OperationWithNoInputOrOutputRequest)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    public interface Builder extends CopyableBuilder<Builder, OperationWithNoInputOrOutputRequest> {
    }

    static final class BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(OperationWithNoInputOrOutputRequest model) {
        }

        @Override
        public OperationWithNoInputOrOutputRequest build() {
            return new OperationWithNoInputOrOutputRequest(this);
        }
    }
}
