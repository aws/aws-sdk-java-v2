package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.eventstream.DefaultEventOne;
import software.amazon.awssdk.services.jsonprotocoltests.model.eventstream.DefaultEventTwo;
import software.amazon.awssdk.services.jsonprotocoltests.model.eventstream.DefaultSecondEventOne;
import software.amazon.awssdk.services.jsonprotocoltests.model.eventstream.DefaultSecondEventTwo;

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
        public void accept(EventStreamOperationResponseHandler.Visitor visitor) {
            visitor.visitDefault(this);
        }
    };

    /**
     * Create a builder for the {@code EventOne} event type for this stream.
     */
    static EventOne.Builder eventOneBuilder() {
        return DefaultEventOne.builder();
    }

    /**
     * Create a builder for the {@code SecondEventOne} event type for this stream.
     */
    static EventOne.Builder secondEventOneBuilder() {
        return DefaultSecondEventOne.builder();
    }

    /**
     * Create a builder for the {@code EventTwo} event type for this stream.
     */
    static EventTwo.Builder eventTwoBuilder() {
        return DefaultEventTwo.builder();
    }

    /**
     * Create a builder for the {@code SecondEventTwo} event type for this stream.
     */
    static EventTwo.Builder secondEventTwoBuilder() {
        return DefaultSecondEventTwo.builder();
    }

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventStream}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    void accept(EventStreamOperationResponseHandler.Visitor visitor);
}

