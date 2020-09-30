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
public final class EventTwo implements SdkPojo, Serializable, ToCopyableBuilder<EventTwo.Builder, EventTwo>, EventStream {
    private static final SdkField<String> BAR_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Bar")
                                                                                                       .getter(getter(EventTwo::bar)).setter(setter(Builder::bar))
                                                                                                       .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Bar").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(BAR_FIELD));

    private static final long serialVersionUID = 1L;

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
        if (!(obj instanceof EventTwo)) {
            return false;
        }
        EventTwo other = (EventTwo) obj;
        return Objects.equals(bar(), other.bar());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
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

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<EventTwo, T> g) {
        return obj -> g.apply((EventTwo) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
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

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, EventTwo> {
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

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
