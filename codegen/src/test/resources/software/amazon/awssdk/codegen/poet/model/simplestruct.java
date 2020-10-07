package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class SimpleStruct implements SdkPojo, Serializable, ToCopyableBuilder<SimpleStruct.Builder, SimpleStruct> {
    private static final SdkField<String> STRING_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
        .memberName("StringMember").getter(getter(SimpleStruct::stringMember)).setter(setter(Builder::stringMember))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StringMember").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(STRING_MEMBER_FIELD));

    private static final long serialVersionUID = 1L;

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
        return equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
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

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("SimpleStruct").add("StringMember", stringMember()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "StringMember":
                return Optional.ofNullable(clazz.cast(stringMember()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<SimpleStruct, T> g) {
        return obj -> g.apply((SimpleStruct) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, SimpleStruct> {
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

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
