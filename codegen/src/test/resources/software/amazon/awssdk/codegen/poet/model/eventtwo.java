package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.EventTwoMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class EventTwo implements StructuredPojo, ToCopyableBuilder<EventTwo.Builder, EventTwo>, EventStream {
    private final String bar;

    private EventTwo(BuilderImpl builder) {
        this.bar = builder.bar;
    }

    /**
     * Returns the value of the Bar property for this object.
     *
     * @return The value of the Bar property for this object.
     */
    public String bar() {
        return bar;
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
        hashCode = 31 * hashCode + Objects.hashCode(bar());
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
        if (!(obj instanceof EventTwo)) {
            return false;
        }
        EventTwo other = (EventTwo) obj;
        return Objects.equals(bar(), other.bar());
    }

    @Override
    public String toString() {
        return ToString.builder("EventTwo").add("Bar", bar()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Bar":
            return Optional.ofNullable(clazz.cast(bar()));
        default:
            return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        EventTwoMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventTwo}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    @Override
    public void accept(EventStreamOperationResponseHandler.Visitor visitor) {
        visitor.visit(this);
    }

    public interface Builder extends CopyableBuilder<Builder, EventTwo> {
        /**
         * Sets the value of the Bar property for this object.
         *
         * @param bar
         *        The new value for the Bar property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder bar(String bar);
    }

    static final class BuilderImpl implements Builder {
        private String bar;

        private BuilderImpl() {
        }

        private BuilderImpl(EventTwo model) {
            bar(model.bar);
        }

        public final String getBar() {
            return bar;
        }

        @Override
        public final Builder bar(String bar) {
            this.bar = bar;
            return this;
        }

        public final void setBar(String bar) {
            this.bar = bar;
        }

        @Override
        public EventTwo build() {
            return new EventTwo(this);
        }
    }
}
