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
public class EventTwo implements SdkPojo, Serializable, ToCopyableBuilder<EventTwo.Builder, EventTwo>, EventStream {
    private static final SdkField<String> BAR_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Bar")
                                                              .getter(getter(EventTwo::bar)).setter(setter(Builder::bar))
                                                              .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Bar").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(BAR_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private static final long serialVersionUID = 1L;

    private final String bar;

    protected EventTwo(BuilderImpl builder) {
        this.bar = builder.bar;
    }

    /**
     * Returns the value of the Bar property for this object.
     *
     * @return The value of the Bar property for this object.
     */
    public final String bar() {
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
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(bar());
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
    public final String toString() {
        return ToString.builder("EventTwo").add("Bar", bar()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "Bar":
                return Optional.ofNullable(clazz.cast(bar()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final EventTwo copy(Consumer<? super Builder> modifier) {
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
        map.put("Bar", BAR_FIELD);
        return Collections.unmodifiableMap(map);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public SdkEventType sdkEventType() {
        throw new UnsupportedOperationException("Unknown Event");
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

    protected static class BuilderImpl implements Builder {
        private String bar;

        protected BuilderImpl() {
        }

        protected BuilderImpl(EventTwo model) {
            bar(model.bar);
        }

        public final String getBar() {
            return bar;
        }

        public final void setBar(String bar) {
            this.bar = bar;
        }

        @Override
        public final Builder bar(String bar) {
            this.bar = bar;
            return this;
        }

        @Override
        public EventTwo build() {
            return new EventTwo(this);
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
