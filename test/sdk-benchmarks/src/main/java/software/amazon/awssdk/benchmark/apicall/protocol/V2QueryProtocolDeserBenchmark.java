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
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V2QueryProtocolDeserBenchmark {

    private SqsClient sqs;
    private SendMessageRequest sendMessageRequest;
    private ReceiveMessageRequest receiveMessageRequest;

    @Setup
    public void setup() {
        SendMessageResponse sendMessageResponse = SendMessageResponse.builder()
                .messageId("test-message-id")
                .md5OfMessageBody("test-md5")
                .build();

        Message message = Message.builder()
                .messageId("test-message-id")
                .body("test-body")
                .build();
        ReceiveMessageResponse receiveMessageResponse = ReceiveMessageResponse.builder()
                .messages(Collections.singletonList(message))
                .build();

        sqs = new SqsClient() {
            @Override
            public String serviceName() {
                return "SQS";
            }

            @Override
            public void close() {
            }

            @Override
            public SendMessageResponse sendMessage(SendMessageRequest request) {
                return sendMessageResponse;
            }

            @Override
            public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest request) {
                return receiveMessageResponse;
            }
        };

        sendMessageRequest = SendMessageRequest.builder()
                .queueUrl("test-queue")
                .messageBody("test-body")
                .build();

        receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl("test-queue")
                .build();
    }

    @Benchmark
    public SendMessageResponse sendMessageSerialization(Blackhole bh) {
        SendMessageResponse result = sqs.sendMessage(sendMessageRequest);
        bh.consume(result.messageId());
        return result;
    }

    @Benchmark
    public ReceiveMessageResponse receiveMessageDeserialization(Blackhole bh) {
        ReceiveMessageResponse result = sqs.receiveMessage(receiveMessageRequest);
        bh.consume(result.messages());
        return result;
    }
}
