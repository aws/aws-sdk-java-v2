package software.amazon.awssdk.services.sharedeventstream.model;

import java.time.Instant;
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
public final class GetRandomPersonResponse extends SharedEventStreamResponse implements
                                                                             ToCopyableBuilder<GetRandomPersonResponse.Builder, GetRandomPersonResponse> {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Name")
                                                                                                        .getter(getter(GetRandomPersonResponse::name)).setter(setter(Builder::name))
                                                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Name").build()).build();

    private static final SdkField<Instant> BIRTHDAY_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
        .memberName("Birthday").getter(getter(GetRandomPersonResponse::birthday)).setter(setter(Builder::birthday))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Birthday").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD, BIRTHDAY_FIELD));

    private final String name;

    private final Instant birthday;

    private GetRandomPersonResponse(BuilderImpl builder) {
        super(builder);
        this.name = builder.name;
        this.birthday = builder.birthday;
    }

    /**
     * Returns the value of the Name property for this object.
     *
     * @return The value of the Name property for this object.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the value of the Birthday property for this object.
     *
     * @return The value of the Birthday property for this object.
     */
    public Instant birthday() {
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
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(birthday());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GetRandomPersonResponse)) {
            return false;
        }
        GetRandomPersonResponse other = (GetRandomPersonResponse) obj;
        return Objects.equals(name(), other.name()) && Objects.equals(birthday(), other.birthday());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("GetRandomPersonResponse").add("Name", name()).add("Birthday", birthday()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
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
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetRandomPersonResponse, T> g) {
        return obj -> g.apply((GetRandomPersonResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SharedEventStreamResponse.Builder, SdkPojo,
                                     CopyableBuilder<Builder, GetRandomPersonResponse> {
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

    static final class BuilderImpl extends SharedEventStreamResponse.BuilderImpl implements Builder {
        private String name;

        private Instant birthday;

        private BuilderImpl() {
        }

        private BuilderImpl(GetRandomPersonResponse model) {
            super(model);
            name(model.name);
            birthday(model.birthday);
        }

        public final String getName() {
            return name;
        }

        @Override
        public final Builder name(String name) {
            this.name = name;
            return this;
        }

        public final void setName(String name) {
            this.name = name;
        }

        public final Instant getBirthday() {
            return birthday;
        }

        @Override
        public final Builder birthday(Instant birthday) {
            this.birthday = birthday;
            return this;
        }

        public final void setBirthday(Instant birthday) {
            this.birthday = birthday;
        }

        @Override
        public GetRandomPersonResponse build() {
            return new GetRandomPersonResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
