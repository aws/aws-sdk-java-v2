package software.amazon.awssdk.services.jsonprotocoltests.model;

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
}

