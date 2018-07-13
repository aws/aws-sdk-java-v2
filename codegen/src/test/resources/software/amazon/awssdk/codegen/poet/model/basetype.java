package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.BaseTypeMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class BaseType implements StructuredPojo, ToCopyableBuilder<BaseType.Builder, BaseType> {
    private final String baseMember;

    private BaseType(BuilderImpl builder) {
        this.baseMember = builder.baseMember;
    }

    /**
     * Returns the value of the BaseMember property for this object.
     *
     * @return The value of the BaseMember property for this object.
     */
    public String baseMember() {
        return baseMember;
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
        hashCode = 31 * hashCode + Objects.hashCode(baseMember());
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
        if (!(obj instanceof BaseType)) {
            return false;
        }
        BaseType other = (BaseType) obj;
        return Objects.equals(baseMember(), other.baseMember());
    }

    @Override
    public String toString() {
        return ToString.builder("BaseType").add("BaseMember", baseMember()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "BaseMember":
                return Optional.ofNullable(clazz.cast(baseMember()));
            default:
                return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        BaseTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, BaseType> {
        /**
         * Sets the value of the BaseMember property for this object.
         *
         * @param baseMember
         *        The new value for the BaseMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder baseMember(String baseMember);
    }

    static final class BuilderImpl implements Builder {
        private String baseMember;

        private BuilderImpl() {
        }

        private BuilderImpl(BaseType model) {
            baseMember(model.baseMember);
        }

        public final String getBaseMember() {
            return baseMember;
        }

        @Override
        public final Builder baseMember(String baseMember) {
            this.baseMember = baseMember;
            return this;
        }

        public final void setBaseMember(String baseMember) {
            this.baseMember = baseMember;
        }

        @Override
        public BaseType build() {
            return new BaseType(this);
        }
    }
}
