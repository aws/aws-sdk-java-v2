package software.amazon.awssdk.services.sharedeventstream.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkEventType;
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
public class Person implements SdkPojo, Serializable, ToCopyableBuilder<Person.Builder, Person>, StreamBirthsInputEventStream,
                               EventStream, StreamDeathsInputEventStream {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Name")
                                                               .getter(getter(Person::name)).setter(setter(Builder::name))
                                                               .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Name").build()).build();

    private static final SdkField<Instant> BIRTHDAY_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
                                                                    .memberName("Birthday").getter(getter(Person::birthday)).setter(setter(Builder::birthday))
                                                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Birthday").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD, BIRTHDAY_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private static final long serialVersionUID = 1L;

    private final String name;

    private final Instant birthday;

    protected Person(BuilderImpl builder) {
        this.name = builder.name;
        this.birthday = builder.birthday;
    }

    /**
     * Returns the value of the Name property for this object.
     *
     * @return The value of the Name property for this object.
     */
    public final String name() {
        return name;
    }

    /**
     * Returns the value of the Birthday property for this object.
     *
     * @return The value of the Birthday property for this object.
     */
    public final Instant birthday() {
        return birthday;
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
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(birthday());
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
        if (!(obj instanceof Person)) {
            return false;
        }
        Person other = (Person) obj;
        return Objects.equals(name(), other.name()) && Objects.equals(birthday(), other.birthday());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("Person").add("Name", name()).add("Birthday", birthday()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "Name":
                return Optional.ofNullable(clazz.cast(name()));
            case "Birthday":
                return Optional.ofNullable(clazz.cast(birthday()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final Person copy(Consumer<? super Builder> modifier) {
        return ToCopyableBuilder.super.copy(modifier);
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
        map.put("Name", NAME_FIELD);
        map.put("Birthday", BIRTHDAY_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<Person, T> g) {
        return obj -> g.apply((Person) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    /**
     * Calls the appropriate visit method depending on the subtype of {@link Person}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    @Override
    public void accept(StreamBirthsResponseHandler.Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Calls the appropriate visit method depending on the subtype of {@link Person}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    @Override
    public void accept(StreamDeathsResponseHandler.Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SdkEventType sdkEventType() {
        throw new UnsupportedOperationException("Unknown Event");
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, Person> {
        /**
         * Sets the value of the Name property for this object.
         *
         * @param name
         *        The new value for the Name property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * Sets the value of the Birthday property for this object.
         *
         * @param birthday
         *        The new value for the Birthday property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder birthday(Instant birthday);
    }

    protected static class BuilderImpl implements Builder {
        private String name;

        private Instant birthday;

        protected BuilderImpl() {
        }

        protected BuilderImpl(Person model) {
            name(model.name);
            birthday(model.birthday);
        }

        public final String getName() {
            return name;
        }

        public final void setName(String name) {
            this.name = name;
        }

        @Override
        public final Builder name(String name) {
            this.name = name;
            return this;
        }

        public final Instant getBirthday() {
            return birthday;
        }

        public final void setBirthday(Instant birthday) {
            this.birthday = birthday;
        }

        @Override
        public final Builder birthday(Instant birthday) {
            this.birthday = birthday;
            return this;
        }

        @Override
        public Person build() {
            return new Person(this);
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
