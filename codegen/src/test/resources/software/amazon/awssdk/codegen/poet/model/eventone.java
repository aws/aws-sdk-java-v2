package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.EventOneMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class EventOne implements StructuredPojo, ToCopyableBuilder<EventOne.Builder, EventOne>, EventStream {
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

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        EventOneMarshaller.getInstance().marshall(this, protocolMarshaller);
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

    public interface Builder extends CopyableBuilder<Builder, EventOne> {
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
    }
}
