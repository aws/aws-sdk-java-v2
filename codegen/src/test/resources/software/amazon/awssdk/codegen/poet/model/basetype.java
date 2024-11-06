package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = Collections
            .unmodifiableMap(new HashMap<String, SdkField<?>>() {
                {
                    put("BaseMember", BASE_MEMBER_FIELD);
                }
            });

    private static final long serialVersionUID = 1L;

    private final String baseMember;

    private final String customShape1;

    private final Integer customShape2;

    private BaseType(BuilderImpl builder) {
        this.baseMember = builder.baseMember;
        this.customShape1 = builder.customShape1;
        this.customShape2 = builder.customShape2;
    }

    /**
     * Returns the value of the BaseMember property for this object.
     * 
     * @return The value of the BaseMember property for this object.
     */
    public final String baseMember() {
        return baseMember;
    }

    /**
     * Custom shape of type string
     * 
     * @return Custom shape of type string
     */
    public final String customShape1() {
        return customShape1;
    }

    /**
     * Custom shape of type integer
     * 
     * @return Custom shape of type integer
     */
    public final Integer customShape2() {
        return customShape2;
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
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(baseMember());
        hashCode = 31 * hashCode + Objects.hashCode(customShape1());
        hashCode = 31 * hashCode + Objects.hashCode(customShape2());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
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
        return Objects.equals(baseMember(), other.baseMember()) && Objects.equals(customShape1(), other.customShape1())
                && Objects.equals(customShape2(), other.customShape2());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("BaseType").add("BaseMember", baseMember()).add("CustomShape1", customShape1())
                .add("CustomShape2", customShape2()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "BaseMember":
            return Optional.ofNullable(clazz.cast(baseMember()));
        case "CustomShape1":
            return Optional.ofNullable(clazz.cast(customShape1()));
        case "CustomShape2":
            return Optional.ofNullable(clazz.cast(customShape2()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public final Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
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

        /**
         * Custom shape of type string
         * 
         * @param customShape1
         *        Custom shape of type string
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder customShape1(String customShape1);

        /**
         * Custom shape of type integer
         * 
         * @param customShape2
         *        Custom shape of type integer
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder customShape2(Integer customShape2);
    }

    static final class BuilderImpl implements Builder {
        private String baseMember;

        private String customShape1;

        private Integer customShape2;

        private BuilderImpl() {
        }

        private BuilderImpl(BaseType model) {
            baseMember(model.baseMember);
            customShape1(model.customShape1);
            customShape2(model.customShape2);
        }

        public final String getBaseMember() {
            return baseMember;
        }

        public final void setBaseMember(String baseMember) {
            this.baseMember = baseMember;
        }

        @Override
        public final Builder baseMember(String baseMember) {
            this.baseMember = baseMember;
            return this;
        }

        public final String getCustomShape1() {
            return customShape1;
        }

        public final void setCustomShape1(String customShape1) {
            this.customShape1 = customShape1;
        }

        @Override
        public final Builder customShape1(String customShape1) {
            this.customShape1 = customShape1;
            return this;
        }

        public final Integer getCustomShape2() {
            return customShape2;
        }

        public final void setCustomShape2(Integer customShape2) {
            this.customShape2 = customShape2;
        }

        @Override
        public final Builder customShape2(Integer customShape2) {
            this.customShape2 = customShape2;
            return this;
        }

        @Override
        public BaseType build() {
            return new BaseType(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return SDK_NAME_TO_FIELD;
        }
    }
}
