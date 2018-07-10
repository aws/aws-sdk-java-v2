package software.amazon.awssdk.services.json.model;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.eventstream.EventStreamResponseHandler;

/**
 * Response handler for the EventStreamOperation API.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface EventStreamOperationResponseHandler extends
        EventStreamResponseHandler<EventStreamOperationResponse, EventStream> {
    /**
     * Create a {@link Builder}, used to create a {@link EventStreamOperationResponseHandler}.
     */
    static Builder builder() {
        return new DefaultEventStreamOperationResponseHandlerBuilder();
    }

    /**
     * Builder for {@link EventStreamOperationResponseHandler}. This can be used to create the
     * {@link EventStreamOperationResponseHandler} in a more functional way, you may also directly implement the
     * {@link EventStreamOperationResponseHandler} interface if preferred.
     */
    @Generated("software.amazon.awssdk:codegen")
    interface Builder extends EventStreamResponseHandler.Builder<EventStreamOperationResponse, EventStream, Builder> {
        /**
         * Sets the subscriber to the {@link org.reactivestreams.Publisher} of events. The given {@link Visitor} will be
         * called for each event received by the publisher. Events are requested sequentially after each event is
         * processed. If you need more control over the backpressure strategy consider using
         * {@link #subscriber(java.util.function.Supplier)} instead.
         *
         * @param visitor
         *        Visitor that will be invoked for each incoming event.
         * @return This builder for method chaining
         */
        Builder subscriber(Visitor visitor);

        /**
         * @return A {@link EventStreamOperationResponseHandler} implementation that can be used in the
         *         EventStreamOperation API call.
         */
        EventStreamOperationResponseHandler build();
    }

    /**
     * Visitor for subtypes of {@link EventStream}.
     */
    @Generated("software.amazon.awssdk:codegen")
    interface Visitor {
        /**
         * @return A new {@link Builder}.
         */
        static Builder builder() {
            return new DefaultEventStreamOperationVisitorBuilder();
        }

        /**
         * A required "else" or "default" block, invoked when no other more-specific "visit" method is appropriate. This
         * is invoked under two circumstances:
         * <ol>
         * <li>The event encountered is newer than the current version of the SDK, so no other more-specific "visit"
         * method could be called. In this case, the provided event will be a generic {@link EventStream}. These events
         * can be processed by upgrading the SDK.</li>
         * <li>The event is known by the SDK, but the "visit" was not overridden above. In this case, the provided event
         * will be a specific type of {@link EventStream}.</li>
         * </ol>
         *
         * @param event
         *        The event that was not handled by a more-specific "visit" method.
         */
        default void visitDefault(EventStream event) {
        }

        /**
         * Invoked when a {@link EventOne} is encountered. If this is not overridden, the event will be given to
         * {@link #visitDefault(EventStream)}.
         *
         * @param event
         *        Event being visited
         */
        default void visit(EventOne event) {
            visitDefault(event);
        }

        /**
         * Invoked when a {@link EventTwo} is encountered. If this is not overridden, the event will be given to
         * {@link #visitDefault(EventStream)}.
         *
         * @param event
         *        Event being visited
         */
        default void visit(EventTwo event) {
            visitDefault(event);
        }

        /**
         * Builder for {@link Visitor}. The {@link Visitor} class may also be extended for a more traditional style but
         * this builder allows for a more functional way of creating a visitor will callback methods.
         */
        @Generated("software.amazon.awssdk:codegen")
        interface Builder {
            /**
             * Callback to invoke when either an unknown event is visited or an unhandled event is visited.
             *
             * @param c
             *        Callback to process the event.
             * @return This builder for method chaining.
             */
            Builder onDefault(Consumer<EventStream> c);

            /**
             * @return Visitor implementation.
             */
            Visitor build();

            /**
             * Callback to invoke when a {@link EventOne} is visited.
             *
             * @param c
             *        Callback to process the event.
             * @return This builder for method chaining.
             */
            Builder onEventOne(Consumer<EventOne> c);

            /**
             * Callback to invoke when a {@link EventTwo} is visited.
             *
             * @param c
             *        Callback to process the event.
             * @return This builder for method chaining.
             */
            Builder onEventTwo(Consumer<EventTwo> c);
        }
    }
}
