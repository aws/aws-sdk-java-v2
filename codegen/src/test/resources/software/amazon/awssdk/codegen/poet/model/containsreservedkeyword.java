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
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
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
public final class ContainsReservedKeyword implements SdkPojo, Serializable,
                                                      ToCopyableBuilder<ContainsReservedKeyword.Builder, ContainsReservedKeyword> {
    private static final SdkField<String> NUL_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("NUL")
                                                              .getter(getter(ContainsReservedKeyword::nul)).setter(setter(Builder::nul))
                                                              .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NULL").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NUL_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private static final long serialVersionUID = 1L;

    private final String nul;

    private ContainsReservedKeyword(BuilderImpl builder) {
        this.nul = builder.nul;
    }

    /**
     * Returns the value of the NUL property for this object.
     *
     * @return The value of the NUL property for this object.
     */
    public final String nul() {
        return nul;
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
        hashCode = 31 * hashCode + Objects.hashCode(nul());
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
        if (!(obj instanceof ContainsReservedKeyword)) {
            return false;
        }
        ContainsReservedKeyword other = (ContainsReservedKeyword) obj;
        return Objects.equals(nul(), other.nul());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ContainsReservedKeyword").add("NUL", nul()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "NUL":
                return Optional.ofNullable(clazz.cast(nul()));
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

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("NULL", NUL_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<ContainsReservedKeyword, T> g) {
        return obj -> g.apply((ContainsReservedKeyword) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ContainsReservedKeyword> {
        /**
         * Sets the value of the NUL property for this object.
         *
         * @param nul
         *        The new value for the NUL property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nul(String nul);
    }

    static final class BuilderImpl implements Builder {
        private String nul;

        private BuilderImpl() {
        }

        private BuilderImpl(ContainsReservedKeyword model) {
            nul(model.nul);
        }

        public final String getNul() {
            return nul;
        }

        public final void setNul(String nul) {
            this.nul = nul;
        }

        public final void setNull(String nul) {
            this.nul = nul;
        }

        @Override
        public final Builder nul(String nul) {
            this.nul = nul;
            return this;
        }

        @Override
        public ContainsReservedKeyword build() {
            return new ContainsReservedKeyword(this);
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
