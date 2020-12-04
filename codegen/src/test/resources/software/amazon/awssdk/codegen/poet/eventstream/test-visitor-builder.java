package software.amazon.awssdk.services.json.model;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultEventStreamOperationVisitorBuilder implements EventStreamOperationResponseHandler.Visitor.Builder {
    private Consumer<EventStream> onDefault;

    private Consumer<EventOne> onEventOne;

    private Consumer<EventTwo> onEventTheSecond;

    private Consumer<EventOne> onSecondEventOne;

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
    public EventStreamOperationResponseHandler.Visitor.Builder onEventTheSecond(Consumer<EventTwo> c) {
        this.onEventTheSecond = c;
        return this;
    }

    @Override
    public EventStreamOperationResponseHandler.Visitor.Builder onSecondEventOne(Consumer<EventOne> c) {
        this.onSecondEventOne = c;
        return this;
    }

    @Generated("software.amazon.awssdk:codegen")
    static class VisitorFromBuilder implements EventStreamOperationResponseHandler.Visitor {
        private final Consumer<EventStream> onDefault;

        private final Consumer<EventOne> onEventOne;

        private final Consumer<EventTwo> onEventTheSecond;

        private final Consumer<EventOne> onSecondEventOne;

        VisitorFromBuilder(DefaultEventStreamOperationVisitorBuilder builder) {
            this.onDefault = builder.onDefault != null ? builder.onDefault
                    : EventStreamOperationResponseHandler.Visitor.super::visitDefault;
            this.onEventOne = builder.onEventOne != null ? builder.onEventOne
                    : EventStreamOperationResponseHandler.Visitor.super::visit;
            this.onEventTheSecond = builder.onEventTheSecond != null ? builder.onEventTheSecond
                    : EventStreamOperationResponseHandler.Visitor.super::visitEventTheSecond;
            this.onSecondEventOne = builder.onSecondEventOne != null ? builder.onSecondEventOne
                    : EventStreamOperationResponseHandler.Visitor.super::visitSecondEventOne;
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
        public void visitEventTheSecond(EventTwo event) {
            onEventTheSecond.accept(event);
        }

        @Override
        public void visitSecondEventOne(EventOne event) {
            onSecondEventOne.accept(event);
        }
    }
}

