package software.amazon.awssdk.services.jsonprotocoltests.model;

import javax.annotation.Generated;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public class StreamingInputOperationResponse extends AmazonWebServiceResult<ResponseMetadata> implements
                                                                                              ToCopyableBuilder<StreamingInputOperationResponse.Builder, StreamingInputOperationResponse> {
    private StreamingInputOperationResponse(BuilderImpl builder) {
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

    public interface Builder extends CopyableBuilder<Builder, StreamingInputOperationResponse> {
    }

    private static final class BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(StreamingInputOperationResponse model) {
        }

        @Override
        public StreamingInputOperationResponse build() {
            return new StreamingInputOperationResponse(this);
        }
    }
}
