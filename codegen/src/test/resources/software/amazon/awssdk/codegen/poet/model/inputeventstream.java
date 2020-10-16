package software.amazon.awssdk.services.jsonprotocoltests.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.jsonprotocoltests.model.inputeventstream.DefaultInputEvent;

/**
 * Base interface for all event types in InputEventStream.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface InputEventStream {
    /**
     * Create a builder for the {@code InputEvent} event type for this stream.
     */
    static InputEvent.Builder inputEventBuilder() {
        return DefaultInputEvent.builder();
    }
}

