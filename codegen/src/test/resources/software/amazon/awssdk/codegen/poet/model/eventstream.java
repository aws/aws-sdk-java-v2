package software.amazon.awssdk.services.jsonprotocoltests.model;

/**
 * Base interface for all event types of the EventStreamOperation API.
 */
public interface EventStream {
    /**
     * Special type of {@link EventStream} for unknown types of events that this version of the SDK does not know about
     */
    EventStream UNKNOWN = new EventStream() {
        @Override
        public void accept(EventStreamOperationResponseHandler.Visitor visitor) {
            visitor.visitDefault(this);
        }
    };;

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventStream}.
     *
     * @param visitor
     *        Visitor to invoke.
     */
    void accept(EventStreamOperationResponseHandler.Visitor visitor);
}

