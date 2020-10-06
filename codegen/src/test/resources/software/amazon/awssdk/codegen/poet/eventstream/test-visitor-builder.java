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

    private Consumer<EventOne> onSecondEventOne;

    private Consumer<EventTwo> onSecondEventTwo;

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

    @Override
    public EventStreamOperationResponseHandler.Visitor.Builder onSecondEventOne(Consumer<EventOne> c) {
        this.onSecondEventOne = c;
        return this;
    }

    @Override
    public EventStreamOperationResponseHandler.Visitor.Builder onSecondEventTwo(Consumer<EventTwo> c) {
        this.onSecondEventTwo = c;
        return this;
    }

    @Generated("software.amazon.awssdk:codegen")
    static class VisitorFromBuilder implements EventStreamOperationResponseHandler.Visitor {
        private final Consumer<EventStream> onDefault;

        private final Consumer<EventOne> onEventOne;

        private final Consumer<EventTwo> onEventTwo;

        private final Consumer<EventOne> onSecondEventOne;

        private final Consumer<EventTwo> onSecondEventTwo;

        VisitorFromBuilder(DefaultEventStreamOperationVisitorBuilder builder) {
            this.onDefault = builder.onDefault != null ? builder.onDefault
                    : EventStreamOperationResponseHandler.Visitor.super::visitDefault;
            this.onEventOne = builder.onEventOne != null ? builder.onEventOne
                    : EventStreamOperationResponseHandler.Visitor.super::visit;
            this.onEventTwo = builder.onEventTwo != null ? builder.onEventTwo
                    : EventStreamOperationResponseHandler.Visitor.super::visit;
            this.onSecondEventOne = builder.onSecondEventOne != null ? builder.onSecondEventOne
                    : EventStreamOperationResponseHandler.Visitor.super::visit;
            this.onSecondEventTwo = builder.onSecondEventTwo != null ? builder.onSecondEventTwo
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

        @Override
        public void visitSecondEventOne(EventOne event) {
            onSecondEventOne.accept(event);
        }

        @Override
        public void visitSecondEventTwo(EventTwo event) {
            onSecondEventTwo.accept(event);
        }
    }
}

