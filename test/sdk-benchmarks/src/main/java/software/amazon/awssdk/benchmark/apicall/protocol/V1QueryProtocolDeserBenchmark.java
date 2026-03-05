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

package software.amazon.awssdk.benchmark.apicall.protocol;

import com.amazonaws.services.sqs.AbstractAmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V1QueryProtocolDeserBenchmark {

    private AbstractAmazonSQS sqs;
    private SendMessageRequest sendMessageRequest;
    private ReceiveMessageRequest receiveMessageRequest;

    @Setup
    public void setup() {
        SendMessageResult sendMessageResult = new SendMessageResult()
                .withMessageId("test-message-id")
                .withMD5OfMessageBody("test-md5");

        Message message = new Message()
                .withMessageId("test-message-id")
                .withBody("test-body");
        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult()
                .withMessages(Collections.singletonList(message));

        sqs = new AbstractAmazonSQS() {
            @Override
            public SendMessageResult sendMessage(SendMessageRequest request) {
                return sendMessageResult;
            }

            @Override
            public ReceiveMessageResult receiveMessage(ReceiveMessageRequest request) {
                return receiveMessageResult;
            }
        };

        sendMessageRequest = new SendMessageRequest()
                .withQueueUrl("test-queue")
                .withMessageBody("test-body");

        receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl("test-queue");
    }

    @Benchmark
    public SendMessageResult sendMessageSerialization(Blackhole bh) {
        SendMessageResult result = sqs.sendMessage(sendMessageRequest);
        bh.consume(result.getMessageId());
        return result;
    }

    @Benchmark
    public ReceiveMessageResult receiveMessageDeserialization(Blackhole bh) {
        ReceiveMessageResult result = sqs.receiveMessage(receiveMessageRequest);
        bh.consume(result.getMessages());
        return result;
    }
}
