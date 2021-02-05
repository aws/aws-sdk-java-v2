package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.jsonprotocoltests.model.inputeventstreamtwo.DefaultInputEventTwo;

/**
 * Base interface for all event types in InputEventStreamTwo.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface InputEventStreamTwo {
    /**
     * Create a builder for the {@code InputEventTwo} event type for this stream.
     */
    static InputEventTwo.Builder inputEventTwoBuilder() {
        return DefaultInputEventTwo.builder();
    }

    /**
     * The type of this event. Corresponds to the {@code :event-type} header on the Message.
     */
    default EventType sdkEventType() {
        return EventType.UNKNOWN_TO_SDK_VERSION;
    }

    /**
     * The known possible types of events for {@code InputEventStreamTwo}.
     */
    @Generated("software.amazon.awssdk:codegen")
    enum EventType {
        INPUT_EVENT_TWO("InputEventTwo"),

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

