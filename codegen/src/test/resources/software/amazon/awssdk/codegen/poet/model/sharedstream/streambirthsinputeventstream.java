package software.amazon.awssdk.services.sharedeventstream.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkEventType;
import software.amazon.awssdk.services.sharedeventstream.model.streambirthsinputeventstream.DefaultPerson;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * Base interface for all event types in StreamBirthsInputEventStream.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface StreamBirthsInputEventStream {
    /**
     * Create a builder for the {@code Person} event type for this stream.
     */
    static Person.Builder personBuilder() {
        return DefaultPerson.builder();
    }

    /**
     * The type of this event. Corresponds to the {@code :event-type} header on the Message.
     */
    SdkEventType sdkEventType();

    /**
     * The known possible types of events for {@code StreamBirthsInputEventStream}.
     */
    @Generated("software.amazon.awssdk:codegen")
    enum EventType implements SdkEventType {
        PERSON("Person"),

        UNKNOWN_TO_SDK_VERSION(null);

        private static final Map<String, EventType> VALUE_MAP = EnumUtils.uniqueIndex(EventType.class, EventType::toString);

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
            return VALUE_MAP.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);
        }

        /**
         * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will
         * return all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
         *
         * @return a {@link Set} of known {@link EventType}s
         */
        public static Set<EventType> knownValues() {
            Set<EventType> knownValues = EnumSet.allOf(EventType.class);
            knownValues.remove(UNKNOWN_TO_SDK_VERSION);
            return knownValues;
        }

        @Override
        public String id() {
            return String.valueOf(value);
        }
    }
}
