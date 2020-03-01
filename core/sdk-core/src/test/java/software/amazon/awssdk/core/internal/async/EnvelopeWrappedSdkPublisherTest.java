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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;

import utils.FakePublisher;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeWrappedSdkPublisherTest {
    private static final BiFunction<String, String, String> CONCAT_STRINGS = (s1, s2) -> s1 + s2;

    private final FakePublisher<String> fakePublisher = new FakePublisher<>();

    @Mock
    private Subscriber<String> mockSubscriber;

    @Test
    public void noPrefixOrSuffix_noEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, null, null, CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void noPrefixOrSuffix_singleEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, null, null, CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test1");
        verify(mockSubscriber).onNext("test1");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void noPrefixOrSuffix_multipleEvents() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, null, null, CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test1");
        verify(mockSubscriber).onNext("test1");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test2");
        verify(mockSubscriber).onNext("test2");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void prefixOnly_noEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", null, CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }


    @Test
    public void prefixOnly_singleEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", null, CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test1");
        verify(mockSubscriber).onNext("test-prefix:test1");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void prefixOnly_multipleEvents() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", null, CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test1");
        verify(mockSubscriber).onNext("test-prefix:test1");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test2");
        verify(mockSubscriber).onNext("test2");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void suffixOnly_noEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, null, ":test-suffix", CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void suffixOnly_singleEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, null, ":test-suffix", CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test");
        verify(mockSubscriber).onNext("test");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onNext(":test-suffix");
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void suffixOnly_multipleEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, null, ":test-suffix", CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test1");
        verify(mockSubscriber).onNext("test1");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test2");
        verify(mockSubscriber).onNext("test2");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onNext(":test-suffix");
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void prefixAndSuffix_noEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", ":test-suffix", CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void prefixAndSuffix_singleEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", ":test-suffix", CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test");
        verify(mockSubscriber).onNext("test-prefix:test");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onNext(":test-suffix");
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void prefixAndSuffix_multipleEvent() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", ":test-suffix", CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test1");
        verify(mockSubscriber).onNext("test-prefix:test1");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.publish("test2");
        verify(mockSubscriber).onNext("test2");
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        fakePublisher.complete();
        verify(mockSubscriber).onNext(":test-suffix");
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void onError() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", ":test-suffix", CONCAT_STRINGS);

        verifyZeroInteractions(mockSubscriber);

        contentWrappingPublisher.subscribe(mockSubscriber);
        verify(mockSubscriber, never()).onNext(anyString());
        verify(mockSubscriber).onSubscribe(any());
        verifyNoMoreInteractions(mockSubscriber);
        reset(mockSubscriber);

        RuntimeException exception = new RuntimeException("boom");
        fakePublisher.doThrow(exception);
        verify(mockSubscriber).onError(exception);
    }

    @Test
    public void subscribe_nullSubscriber_throwsNpe() {
        EnvelopeWrappedSdkPublisher<String> contentWrappingPublisher =
            EnvelopeWrappedSdkPublisher.of(fakePublisher, "test-prefix:", ":test-suffix", CONCAT_STRINGS);

        assertThatThrownBy(() -> contentWrappingPublisher.subscribe((Subscriber<String>)null))
            .isInstanceOf(NullPointerException.class);
    }
}