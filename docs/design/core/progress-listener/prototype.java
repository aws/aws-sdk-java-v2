package software.amazon.awssdk.core.progress;

/**
 * Listener interface to listen to the progress events.
 *
 * @see ProgressEvent the progress event
 */
@SdkPublicApi
@FunctionalInterface
public interface ProgressEventListener {

    /**
     * Data notification sent by the {@link ProgressEventPublisher} when an event is available.
     *
     * <p>
     * It is recommended to process the events in a non-blocking way
     *
     * @param progressEvent the progress event
     * @return the complete future
     */
    CompletableFuture<?> onProgressEvent(ProgressEvent progressEvent);
}

/**
 * interface to publish {@link ProgressEvent}s to {@link ProgressEventListener}s.
 */
@SdkPublicApi
@FunctionalInterface
public interface ProgressEventPublisher {

    /**
     * Publish the progress events
     *
     * @param progressEvent the event to publish
     * @return the future contains the results whether the publish is successful or not.
     */
    CompletableFuture<?> publishProgressEvent(ProgressEvent progressEvent);
}

/**
 * A progress event. Typically this is used to notify a chunk of bytes has been
 * transferred. This can also used to notify other types of progress events such as a
 * transfer starting, or failing.
 *
 * @see ProgressEventPublisher
 * @see ProgressEventListener
 */
@SdkPublicApi
public final class ProgressEvent {

    private final ProgressEventType eventType;
    private final ProgressEventData eventData;

    private ProgressEvent(ProgressEventType eventType, ProgressEventData eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    public ProgressEvent(ProgressEventType eventType) {
        this(eventType, null);
    }

    public static ProgressEvent create(ProgressEventType eventType) {
        return new ProgressEvent(eventType);
    }

    public static ProgressEvent create(ProgressEventType eventType, ProgressEventData eventData) {
        return new ProgressEvent(eventType, eventData);
    }

    /**
     * @return the event type
     */
    public ProgressEventType eventType() {
        return eventType;
    }

    /**
     * @return the event data
     */
    public Optional<ProgressEventData> eventData() {
        return Optional.ofNullable(eventData);
    }
}

/**
 * The event data to be sent along within an {@link ProgressEvent}.
 */
@SdkPublicApi
public interface ProgressEventData {
}

/**
 * Progress Event type
 */
@SdkPublicApi
public interface ProgressEventType {
}

/**
 * Sdk default progress events.
 */
@SdkPublicApi
public enum SdkProgressEventType implements ProgressEventType {


    ////////////////////////////////////////
    // Byte counting progress events
    ////////////////////////////////////////

    /**
     * Event of the content length to be sent in a request.
     */
    REQUEST_CONTENT_LENGTH_EVENT,
    /**
     * Event of the content length received in a response.
     */
    RESPONSE_CONTENT_LENGTH_EVENT,

    /**
     * Used to indicate the number of bytes to be sent to the services.
     */
    REQUEST_BYTE_TRANSFER_EVENT,

    /**
     * Used to indicate the number of bytes received from services.
     */
    RESPONSE_BYTE_TRANSFER_EVENT,

    ////////////////////////////////////////
    // Request/Response progress events
    ////////////////////////////////////////

    /**
     * Used to indicate the request body has been cancelled.
     */
    REQUEST_BODY_CANCEL_EVENT,

    /**
     * Used to indicate the request body is complete.
     */
    REQUEST_BODY_COMPLETE_EVENT,

    /**
     * Used to indicate the request has been reset.
     */
    REQUEST_BODY_RESET_EVENT,

    RESPONSE_BODY_CANCEL_EVENT,

    RESPONSE_BODY_COMPLETE_EVENT,

    RESPONSE_BODY_RESET_EVENT
}