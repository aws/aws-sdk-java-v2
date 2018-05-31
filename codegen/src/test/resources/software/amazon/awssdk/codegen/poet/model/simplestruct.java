package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.SimpleStructMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class SimpleStruct implements StructuredPojo, ToCopyableBuilder<SimpleStruct.Builder, SimpleStruct> {
    private final String stringMember;

    private SimpleStruct(BuilderImpl builder) {
        this.stringMember = builder.stringMember;
    }

    /**
     * Returns the value of the StringMember property for this object.
     *
     * @return The value of the StringMember property for this object.
     */
    public String stringMember() {
        return stringMember;
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
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
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
        if (!(obj instanceof SimpleStruct)) {
            return false;
        }
        SimpleStruct other = (SimpleStruct) obj;
        return Objects.equals(stringMember(), other.stringMember());
    }

    @Override
    public String toString() {
        return ToString.builder("SimpleStruct").add("StringMember", stringMember()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "StringMember":
                return Optional.of(clazz.cast(stringMember()));
            default:
                return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        SimpleStructMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, SimpleStruct> {
        /**
         * Sets the value of the StringMember property for this object.
         *
         * @param stringMember
         *        The new value for the StringMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);
    }

    static final class BuilderImpl implements Builder {
        private String stringMember;

        private BuilderImpl() {
        }

        private BuilderImpl(SimpleStruct model) {
            stringMember(model.stringMember);
        }

        public final String getStringMember() {
            return stringMember;
        }

        @Override
        public final Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public final void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public SimpleStruct build() {
            return new SimpleStruct(this);
        }
    }
}
