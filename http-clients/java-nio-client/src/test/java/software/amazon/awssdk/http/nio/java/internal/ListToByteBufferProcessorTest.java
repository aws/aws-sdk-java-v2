/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java.internal;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.processors.ReplayProcessor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.Test;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscription;

public class ListToByteBufferProcessorTest {

    private ListToByteBufferProcessor listToByteBufferProcessor = new ListToByteBufferProcessor();
    private PublishProcessor<List<ByteBuffer>> publishProcessor = PublishProcessor.create();
    private ErrorPublisher errorPublisher = new ErrorPublisher();
    private CompletePublisher completePublisher = new CompletePublisher();

    private final int byteBufferSize1 = 20;
    private final int byteBufferSize2 = 20;

    private FlowableSubscriber<ByteBuffer> simSdkSubscriber = new FlowableSubscriber<ByteBuffer>() {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            assertEquals(byteBuffer.capacity(), byteBufferSize1+byteBufferSize2);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public void onComplete() {
        }
    };

    @Test
    public void publisherTest() {
        List<ByteBuffer> list1 = new ArrayList<>();
        List<ByteBuffer> list2 = new ArrayList<>();
        String tempString1 = "Hello";
        String tempString2 = " World";
        String tempString3 = "!";
        ByteBuffer tempBuffer1 = ByteBuffer.wrap(tempString1.getBytes(StandardCharsets.UTF_8));
        ByteBuffer tempBuffer2 = ByteBuffer.wrap(tempString2.getBytes(StandardCharsets.UTF_8));
        ByteBuffer tempBuffer3 = ByteBuffer.wrap(tempString3.getBytes(StandardCharsets.UTF_8));
        list1.add(tempBuffer1);
        list1.add(tempBuffer2);
        list2.add(tempBuffer3);

        Flowable<List<ByteBuffer>> bufferListPublisher = Flowable.just(list1, list2);

        ListToByteBufferProcessor listToByteBufferProcessor = new ListToByteBufferProcessor();

        ReplayProcessor<ByteBuffer> replayProcessor = ReplayProcessor.create();

        listToByteBufferProcessor.getPublisherToSdk().subscribe(replayProcessor);
        bufferListPublisher.subscribe(listToByteBufferProcessor);
        List<ByteBuffer> resultList = Flowable.fromPublisher(replayProcessor).toList().blockingGet();
        assertThat(resultList.get(0).toString().equals("Hello World") && resultList.get(1).toString().equals("!"));
    }

    @Test
    public void mapperTest() {
        List<ByteBuffer> list = new ArrayList<>();
        String tempString1 = "Hello";
        String tempString2 = " World!";
        ByteBuffer tempBuffer1 = ByteBuffer.wrap(tempString1.getBytes(StandardCharsets.UTF_8));
        ByteBuffer tempBuffer2 = ByteBuffer.wrap(tempString2.getBytes(StandardCharsets.UTF_8));
        list.add(tempBuffer1);
        list.add(tempBuffer2);
        ByteBuffer buffer = ListToByteBufferProcessor.convertListToByteBuffer(list);
        assertEquals(tempString1+tempString2, new String(buffer.array()));
    }

    @Test
    public void onErrorTest() {
        errorPublisher.subscribe(FlowAdapters.toFlowSubscriber(listToByteBufferProcessor));
        listToByteBufferProcessor.getPublisherToSdk().subscribe(simSdkSubscriber);
        listToByteBufferProcessor.onComplete();
    }

    @Test
    public void onCompleteTest() {
        completePublisher.subscribe(FlowAdapters.toFlowSubscriber(listToByteBufferProcessor));
        listToByteBufferProcessor.getPublisherToSdk().subscribe(simSdkSubscriber);
        listToByteBufferProcessor.onComplete();
    }

    @Test
    public void ProcessorTest() {
        List<ByteBuffer> testList1 = new ArrayList<>();
        ByteBuffer buf1 = ByteBuffer.allocate(byteBufferSize1);
        ByteBuffer buf2 = ByteBuffer.allocate(byteBufferSize2);

        testList1.add(buf1);
        testList1.add(buf2);

        publishProcessor.subscribe(listToByteBufferProcessor);
        listToByteBufferProcessor.getPublisherToSdk().subscribe(simSdkSubscriber);
        publishProcessor.onNext(testList1);
        publishProcessor.onComplete();
        listToByteBufferProcessor.onComplete();
    }

    public static class ErrorPublisher implements Flow.Publisher<List<ByteBuffer>> {

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });

            subscriber.onError(new RuntimeException("onError: invoked successfully, something went wrong!"));
        }

    }

    public static class CompletePublisher implements Flow.Publisher<List<ByteBuffer>> {

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });

            subscriber.onComplete();
        }

    }
}