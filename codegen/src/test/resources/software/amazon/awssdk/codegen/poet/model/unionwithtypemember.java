package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class UnionWithTypeMember implements SdkPojo, Serializable,
                                                  ToCopyableBuilder<UnionWithTypeMember.Builder, UnionWithTypeMember> {
    private static final SdkField<String> STRING_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                        .memberName("StringMember").getter(getter(UnionWithTypeMember::stringMember)).setter(setter(Builder::stringMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StringMember").build()).build();

    private static final SdkField<String> TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Type")
                                                               .getter(getter(UnionWithTypeMember::typeValue)).setter(setter(Builder::typeValue))
                                                               .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Type").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(STRING_MEMBER_FIELD,
                                                                                                   TYPE_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private static final long serialVersionUID = 1L;

    private final String stringMember;

    private final String typeValue;

    private final Type type;

    private UnionWithTypeMember(BuilderImpl builder) {
        this.stringMember = builder.stringMember;
        this.typeValue = builder.typeValue;
        this.type = builder.type;
    }

    /**
     * Returns the value of the StringMember property for this object.
     *
     * @return The value of the StringMember property for this object.
     */
    public final String stringMember() {
        return stringMember;
    }

    /**
     * Returns the value of the Type property for this object.
     *
     * @return The value of the Type property for this object.
     */
    public final String typeValue() {
        return typeValue;
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
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
        hashCode = 31 * hashCode + Objects.hashCode(typeValue());
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
        if (!(obj instanceof UnionWithTypeMember)) {
            return false;
        }
        UnionWithTypeMember other = (UnionWithTypeMember) obj;
        return Objects.equals(stringMember(), other.stringMember()) && Objects.equals(typeValue(), other.typeValue());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("UnionWithTypeMember").add("StringMember", stringMember()).add("Type", typeValue()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "StringMember":
                return Optional.ofNullable(clazz.cast(stringMember()));
            case "Type":
                return Optional.ofNullable(clazz.cast(typeValue()));
            default:
                return Optional.empty();
        }
    }

    /**
     * Create an instance of this class with {@link #stringMember()} initialized to the given value.
     *
     * Sets the value of the StringMember property for this object.
     *
     * @param stringMember
     *        The new value for the StringMember property for this object.
     */
    public static UnionWithTypeMember fromStringMember(String stringMember) {
        return builder().stringMember(stringMember).build();
    }

    /**
     * Create an instance of this class with {@link #typeValue()} initialized to the given value.
     *
     * Sets the value of the Type property for this object.
     *
     * @param typeValue
     *        The new value for the Type property for this object.
     */
    public static UnionWithTypeMember fromTypeValue(String typeValue) {
        return builder().typeValue(typeValue).build();
    }

    /**
     * Retrieve an enum value representing which member of this object is populated.
     *
     * When this class is returned in a service response, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if the
     * service returned a member that is only known to a newer SDK version.
     *
     * When this class is created directly in your code, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if zero
     * members are set, and {@code null} if more than one member is set.
     */
    public Type type() {
        return type;
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
        map.put("StringMember", STRING_MEMBER_FIELD);
        map.put("Type", TYPE_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<UnionWithTypeMember, T> g) {
        return obj -> g.apply((UnionWithTypeMember) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends SdkPojo, CopyableBuilder<Builder, UnionWithTypeMember> {
        /**
         * Sets the value of the StringMember property for this object.
         *
         * @param stringMember
         *        The new value for the StringMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);

        /**
         * Sets the value of the Type property for this object.
         *
         * @param typeValue
         *        The new value for the Type property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder typeValue(String typeValue);
    }

    static final class BuilderImpl implements Builder {
        private String stringMember;

        private String typeValue;

        private Type type = Type.UNKNOWN_TO_SDK_VERSION;

        private Set<Type> setTypes = EnumSet.noneOf(Type.class);

        private BuilderImpl() {
        }

        private BuilderImpl(UnionWithTypeMember model) {
            stringMember(model.stringMember);
            typeValue(model.typeValue);
        }

        public final String getStringMember() {
            return stringMember;
        }

        public final void setStringMember(String stringMember) {
            Object oldValue = this.stringMember;
            this.stringMember = stringMember;
            handleUnionValueChange(Type.STRING_MEMBER, oldValue, this.stringMember);
        }

        @Override
        public final Builder stringMember(String stringMember) {
            Object oldValue = this.stringMember;
            this.stringMember = stringMember;
            handleUnionValueChange(Type.STRING_MEMBER, oldValue, this.stringMember);
            return this;
        }

        public final String getTypeValue() {
            return typeValue;
        }

        public final void setTypeValue(String typeValue) {
            Object oldValue = this.typeValue;
            this.typeValue = typeValue;
            handleUnionValueChange(Type.TYPE, oldValue, this.typeValue);
        }

        @Override
        public final Builder typeValue(String typeValue) {
            Object oldValue = this.typeValue;
            this.typeValue = typeValue;
            handleUnionValueChange(Type.TYPE, oldValue, this.typeValue);
            return this;
        }

        @Override
        public UnionWithTypeMember build() {
            return new UnionWithTypeMember(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return SDK_NAME_TO_FIELD;
        }

        private final void handleUnionValueChange(Type type, Object oldValue, Object newValue) {
            if (this.type == type || oldValue == newValue) {
                return;
            }
            if (newValue == null || newValue instanceof SdkAutoConstructList || newValue instanceof SdkAutoConstructMap) {
                setTypes.remove(type);
            } else if (oldValue == null || oldValue instanceof SdkAutoConstructList || oldValue instanceof SdkAutoConstructMap) {
                setTypes.add(type);
            }
            if (setTypes.size() == 1) {
                this.type = setTypes.iterator().next();
            } else if (setTypes.isEmpty()) {
                this.type = Type.UNKNOWN_TO_SDK_VERSION;
            } else {
                this.type = null;
            }
        }
    }

    /**
     * @see UnionWithTypeMember#type()
     */
    public enum Type {
        STRING_MEMBER,

        TYPE,

        UNKNOWN_TO_SDK_VERSION
    }
}
