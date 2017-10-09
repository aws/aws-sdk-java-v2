package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
public class OperationWithNoInputOrOutputResponse extends
        JsonProtocolTestsResponse<OperationWithNoInputOrOutputResponse.Builder, OperationWithNoInputOrOutputResponse> {
    private OperationWithNoInputOrOutputResponse(BuilderImpl builder) {
        super(builder);
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
        if (!(obj instanceof OperationWithNoInputOrOutputResponse)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("}");
        return sb.toString();
    }

    public interface Builder extends JsonProtocolTestsResponse.Builder<Builder, OperationWithNoInputOrOutputResponse> {
    }

    private static final class BuilderImpl extends
            JsonProtocolTestsResponse.BuilderImpl<Builder, OperationWithNoInputOrOutputResponse> implements Builder {
        private BuilderImpl() {
            super(Builder.class);
        }

        private BuilderImpl(OperationWithNoInputOrOutputResponse model) {
            super(Builder.class, model);
        }

        @Override
        public OperationWithNoInputOrOutputResponse build() {
            return new OperationWithNoInputOrOutputResponse(this);
        }
    }
}

