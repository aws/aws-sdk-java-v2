package software.amazon.awssdk.services.jsonprotocoltests.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class EmptyModeledException extends JsonProtocolTestsException implements
        ToCopyableBuilder<EmptyModeledException.Builder, EmptyModeledException> {
    private EmptyModeledException(BuilderImpl builder) {
        super(builder.message);
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

    public interface Builder extends CopyableBuilder<Builder, EmptyModeledException> {
        Builder message(String message);
    }

    static final class BuilderImpl implements Builder {
        private String message;

        private BuilderImpl() {
        }

        private BuilderImpl(EmptyModeledException model) {
            this.message = model.getMessage();
        }

        public String getMessage() {
            return message;
        }

        public String message() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public EmptyModeledException build() {
            return new EmptyModeledException(this);
        }
    }
}
