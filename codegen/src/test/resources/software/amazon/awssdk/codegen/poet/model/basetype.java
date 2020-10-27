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
public final class BaseType implements SdkPojo, Serializable, ToCopyableBuilder<BaseType.Builder, BaseType> {
    private static final SdkField<String> BASE_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
        .memberName("BaseMember").getter(getter(BaseType::baseMember)).setter(setter(Builder::baseMember))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("BaseMember").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(BASE_MEMBER_FIELD));

    private static final long serialVersionUID = 1L;

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
        if (!(obj instanceof BaseType)) {
            return false;
        }
        BaseType other = (BaseType) obj;
        return Objects.equals(baseMember(), other.baseMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
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

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<BaseType, T> g) {
        return obj -> g.apply((BaseType) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, BaseType> {
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

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
