package software.amazon.awssdk.services.sharedeventstream.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.services.sharedeventstream.model.eventstream.DefaultPerson;

/**
 * Base interface for all event types in EventStream.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface EventStream extends SdkPojo {
    /**
     * Special type of {@link EventStream} for unknown types of events that this version of the SDK does not know about
     */
    EventStream UNKNOWN = new EventStream() {
        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public void accept(StreamBirthsResponseHandler.Visitor visitor) {
            visitor.visitDefault(this);
        }

        @Override
        public void accept(StreamDeathsResponseHandler.Visitor visitor) {
            visitor.visitDefault(this);
        }
    };

    /**
     * Create a builder for the {@code Person} event type for this stream.
     */
    static Person.Builder personBuilder() {
        return DefaultPerson.builder();
    }

    /**
     * The type of this event. Corresponds to the {@code :event-type} header on the Message.
     */
    default EventType sdkEventType() {
        return EventType.UNKNOWN_TO_SDK_VERSION;
    }

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventStream}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    void accept(StreamBirthsResponseHandler.Visitor visitor);

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventStream}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    void accept(StreamDeathsResponseHandler.Visitor visitor);

    /**
     * The known possible types of events for {@code EventStream}.
     */
    @Generated("software.amazon.awssdk:codegen")
    enum EventType {
        PERSON("Person"),

        UNKNOWN_TO_SDK_VERSION(null);

        private final String value;

        private EventType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        /**
         * Use this in place of valueOf to convert the raw string returned by the service into the enum value.
         *
         * @param value
         *        real value
         * @return EventType corresponding to the value
         */
        public static EventType fromValue(String value) {
            if (value == null) {
                return null;
            }
            return Stream.of(EventType.values()).filter(e -> e.toString().equals(value)).findFirst()
                    .orElse(UNKNOWN_TO_SDK_VERSION);
        }

        /**
         * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will
         * return all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
         *
         * @return a {@link Set} of known {@link EventType}s
         */
        public static Set<EventType> knownValues() {
            return Stream.of(values()).filter(v -> v != UNKNOWN_TO_SDK_VERSION).collect(Collectors.toSet());
        }
    }
}

