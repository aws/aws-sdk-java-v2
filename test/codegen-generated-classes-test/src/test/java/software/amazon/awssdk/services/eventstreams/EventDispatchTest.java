/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.eventstreams;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventOne;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventStream;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventTwo;

/**
 * Tests to ensure that the generated classes that represent each event on an
 * event stream call the correct visitor methods; i.e. that the double
 * dispatching works as expected.
 */
@RunWith(MockitoJUnitRunner.class)
public class EventDispatchTest {

    @Mock
    private EventStreamOperationResponseHandler.Visitor visitor;

    @Mock
    private Consumer<EventStream> onDefaultConsumer;

    @Mock
    private Consumer<EventOne> eventOneConsumer;

    @Mock
    private Consumer<EventOne> legacyGeneratedEventConsumer;

    @Mock
    private Consumer<EventTwo> eventTwoConsumer;

    @Mock
    private Consumer<EventTwo> secondEventTwoConsumer;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void test_acceptEventOne_correctVisitorMethodCalled() {
        EventStream eventStream = EventStream.eventOneBuilder().build();
        eventStream.accept(visitor);

        verify(visitor).visitEventOne(Mockito.eq((EventOne) eventStream));
        verifyNoMoreInteractions(visitor);
    }

    @Test
    public void test_acceptEventOne_visitorBuiltWithBuilder_correctVisitorMethodCalled() {
        EventStreamOperationResponseHandler.Visitor visitor = visitorBuiltWithBuilder();
        EventStream eventStream = EventStream.eventOneBuilder().build();

        eventStream.accept(visitor);

        verify(eventOneConsumer).accept(eq((EventOne) eventStream));
        verifyNoMoreConsumerInteractions();
    }

    @Test
    public void test_acceptLegacyGeneratedEvent_correctVisitorMethodCalled() {
        EventStream eventStream = EventStream.legacyGeneratedEventBuilder().build();
        eventStream.accept(visitor);

        // Note: notice the visit() method rather than visitLegacyGeneratedEvent()
        verify(visitor).visit(Mockito.eq((EventOne) eventStream));
        verifyNoMoreInteractions(visitor);
    }

    @Test
    public void test_acceptLegacyGeneratedEvent_visitorBuiltWithBuilder_correctVisitorMethodCalled() {
        EventStreamOperationResponseHandler.Visitor visitor = visitorBuiltWithBuilder();
        EventStream eventStream = EventStream.legacyGeneratedEventBuilder().build();

        eventStream.accept(visitor);

        verify(legacyGeneratedEventConsumer).accept(eq((EventOne) eventStream));
        verifyNoMoreConsumerInteractions();
    }

    @Test
    public void test_acceptEventTwo_correctVisitorMethodCalled() {
        EventStream eventStream = EventStream.eventTwoBuilder().build();
        eventStream.accept(visitor);

        verify(visitor).visitEventTwo(Mockito.eq((EventTwo) eventStream));
        verifyNoMoreInteractions(visitor);
    }

    @Test
    public void test_acceptEvenTwo_visitorBuiltWithBuilder_correctVisitorMethodCalled() {
        EventStreamOperationResponseHandler.Visitor visitor = visitorBuiltWithBuilder();

        EventStream eventStream = EventStream.eventTwoBuilder().build();

        eventStream.accept(visitor);

        verify(eventTwoConsumer).accept(eq((EventTwo) eventStream));
        verifyNoMoreConsumerInteractions();
    }

    @Test
    public void test_acceptSecondEventTwo_correctVisitorMethodCalled() {
        EventStream eventStream = EventStream.secondEventTwoBuilder().build();
        eventStream.accept(visitor);

        verify(visitor).visitSecondEventTwo(Mockito.eq((EventTwo) eventStream));
        verifyNoMoreInteractions(visitor);
    }

    @Test
    public void test_acceptSecondEvenTwo_visitorBuiltWithBuilder_correctVisitorMethodCalled() {
        EventStreamOperationResponseHandler.Visitor visitor = visitorBuiltWithBuilder();

        EventStream eventStream = EventStream.secondEventTwoBuilder().build();

        eventStream.accept(visitor);

        verify(secondEventTwoConsumer).accept(eq((EventTwo) eventStream));
        verifyNoMoreConsumerInteractions();
    }

    @Test
    public void test_acceptOnBaseClass_UnCustomizedEvent_throwsException() {
        expected.expect(UnsupportedOperationException.class);

        EventTwo eventTwo = EventTwo.builder().build();
        eventTwo.accept(visitor);
    }

    private EventStreamOperationResponseHandler.Visitor visitorBuiltWithBuilder() {
        return EventStreamOperationResponseHandler.Visitor.builder()
                .onDefault(onDefaultConsumer)
                .onEventOne(eventOneConsumer)
                .onEventTwo(eventTwoConsumer)
                .onLegacyGeneratedEvent(legacyGeneratedEventConsumer)
                .onSecondEventTwo(secondEventTwoConsumer)
                .build();
    }

    private void verifyNoMoreConsumerInteractions() {
        verifyNoMoreInteractions(onDefaultConsumer, eventOneConsumer, eventTwoConsumer, legacyGeneratedEventConsumer,
                secondEventTwoConsumer);
    }
}
