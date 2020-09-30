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
public final class EventOne implements SdkPojo, Serializable, ToCopyableBuilder<EventOne.Builder, EventOne>, EventStream {
    private static final SdkField<String> FOO_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Foo")
                                                                                                       .getter(getter(EventOne::foo)).setter(setter(Builder::foo))
                                                                                                       .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Foo").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(FOO_FIELD));

    private static final long serialVersionUID = 1L;

    private final String foo;

    private EventOne(BuilderImpl builder) {
        this.foo = builder.foo;
    }

    /**
     * Returns the value of the Foo property for this object.
     *
     * @return The value of the Foo property for this object.
     */
    public String foo() {
        return foo;
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
        hashCode = 31 * hashCode + Objects.hashCode(foo());
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
        if (!(obj instanceof EventOne)) {
            return false;
        }
        EventOne other = (EventOne) obj;
        return Objects.equals(foo(), other.foo());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("EventOne").add("Foo", foo()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "Foo":
                return Optional.ofNullable(clazz.cast(foo()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<EventOne, T> g) {
        return obj -> g.apply((EventOne) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventOne}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    @Override
    public void accept(EventStreamOperationResponseHandler.Visitor visitor) {
        visitor.visit(this);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, EventOne> {
        /**
         * Sets the value of the Foo property for this object.
         *
         * @param foo
         *        The new value for the Foo property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder foo(String foo);
    }

    static final class BuilderImpl implements Builder {
        private String foo;

        private BuilderImpl() {
        }

        private BuilderImpl(EventOne model) {
            foo(model.foo);
        }

        public final String getFoo() {
            return foo;
        }

        @Override
        public final Builder foo(String foo) {
            this.foo = foo;
            return this;
        }

        public final void setFoo(String foo) {
            this.foo = foo;
        }

        @Override
        public EventOne build() {
            return new EventOne(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
