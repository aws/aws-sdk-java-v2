package software.amazon.awssdk.services.json.model;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultEventStreamOperationVisitorBuilder implements EventStreamOperationResponseHandler.Visitor.Builder {
    private Consumer<EventStream> onDefault;

    private Consumer<EventOne> onEventOne;

    private Consumer<EventTwo> onEventTwo;

    @Override
    public EventStreamOperationResponseHandler.Visitor.Builder onDefault(Consumer<EventStream> c) {
        this.onDefault = c;
        return this;
    }

    @Override
    public EventStreamOperationResponseHandler.Visitor build() {
        return new VisitorFromBuilder(this);
    }

    @Override
    public EventStreamOperationResponseHandler.Visitor.Builder onEventOne(Consumer<EventOne> c) {
        this.onEventOne = c;
        return this;
    }

    @Override
    public EventStreamOperationResponseHandler.Visitor.Builder onEventTwo(Consumer<EventTwo> c) {
        this.onEventTwo = c;
        return this;
    }

    @Generated("software.amazon.awssdk:codegen")
    class VisitorFromBuilder implements EventStreamOperationResponseHandler.Visitor {
        private final Consumer<EventStream> onDefault;

        private final Consumer<EventOne> onEventOne;

        private final Consumer<EventTwo> onEventTwo;

        VisitorFromBuilder(DefaultEventStreamOperationVisitorBuilder builder) {
            this.onDefault = builder.onDefault != null ? builder.onDefault
                    : EventStreamOperationResponseHandler.Visitor.super::visitDefault;
            this.onEventOne = builder.onEventOne != null ? builder.onEventOne
                    : EventStreamOperationResponseHandler.Visitor.super::visit;
            this.onEventTwo = builder.onEventTwo != null ? builder.onEventTwo
                    : EventStreamOperationResponseHandler.Visitor.super::visit;
        }

        @Override
        public void visitDefault(EventStream event) {
            onDefault.accept(event);
        }

        @Override
        public void visit(EventOne event) {
            onEventOne.accept(event);
        }

        @Override
        public void visit(EventTwo event) {
            onEventTwo.accept(event);
        }
    }
}
