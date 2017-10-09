package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
public class StreamingInputOperationResponse extends
        JsonProtocolTestsResponse<StreamingInputOperationResponse.Builder, StreamingInputOperationResponse> {
    private StreamingInputOperationResponse(BuilderImpl builder) {
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
        if (!(obj instanceof StreamingInputOperationResponse)) {
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

    public interface Builder extends JsonProtocolTestsResponse.Builder<Builder, StreamingInputOperationResponse> {
    }

    private static final class BuilderImpl extends
            JsonProtocolTestsResponse.BuilderImpl<Builder, StreamingInputOperationResponse> implements Builder {
        private BuilderImpl() {
            super(Builder.class);
        }

        private BuilderImpl(StreamingInputOperationResponse model) {
            super(Builder.class, model);
        }

        @Override
        public StreamingInputOperationResponse build() {
            return new StreamingInputOperationResponse(this);
        }
    }
}

