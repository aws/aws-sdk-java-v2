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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CSVInput;
import software.amazon.awssdk.services.s3.model.CSVOutput;
import software.amazon.awssdk.services.s3.model.CompressionType;
import software.amazon.awssdk.services.s3.model.ContinuationEvent;
import software.amazon.awssdk.services.s3.model.EndEvent;
import software.amazon.awssdk.services.s3.model.ExpressionType;
import software.amazon.awssdk.services.s3.model.InputSerialization;
import software.amazon.awssdk.services.s3.model.OutputSerialization;
import software.amazon.awssdk.services.s3.model.Progress;
import software.amazon.awssdk.services.s3.model.ProgressEvent;
import software.amazon.awssdk.services.s3.model.RecordsEvent;
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream;
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler;
import software.amazon.awssdk.services.s3.model.Stats;
import software.amazon.awssdk.services.s3.model.StatsEvent;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class SelectObjectContentTest {
    private static final AwsCredentialsProvider TEST_CREDENTIALS = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        "akid", "skid"));
    private static final Region TEST_REGION = Region.US_WEST_2;

    private S3AsyncClient s3;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Before
    public void setup() {
        s3 = S3AsyncClient.builder()
                          .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                          .serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
                          .region(TEST_REGION)
                          .credentialsProvider(TEST_CREDENTIALS)
                          .build();
    }

    @After
    public void teardown() {
        s3.close();
    }

    @Test
    public void selectObjectContent_canUnmarshallAllEvents() {
        List<SelectObjectContentEventStream> events = Arrays.asList(
            SelectObjectContentEventStream.recordsBuilder().payload(SdkBytes.fromUtf8String("abc")).build(),
            SelectObjectContentEventStream.contBuilder().build(),
            SelectObjectContentEventStream.statsBuilder()
                                          .details(Stats.builder()
                                                        .bytesProcessed(1L)
                                                        .bytesScanned(2L)
                                                        .bytesReturned(3L)
                                                        .build()
                                          ).build(),
            SelectObjectContentEventStream.progressBuilder()
                                          .details(Progress.builder()
                                                           .bytesProcessed(1L)
                                                           .bytesScanned(2L)
                                                           .bytesReturned(3L)
                                                           .build()
                                          ).build(),
            SelectObjectContentEventStream.endBuilder().build()
        );

        byte[] eventStream = encodedEvents(events);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(eventStream)));

        TestHandler testHandler = new TestHandler();
        runSimpleQuery(s3, testHandler).join();

        assertThat(testHandler.receivedEvents).isEqualTo(events);
    }

    private byte[] encodedEvents(List<SelectObjectContentEventStream> events) {
        ByteArrayOutputStream eventStreamBytes = new ByteArrayOutputStream();

        MarshallingVisitor marshaller = new MarshallingVisitor();
        events.stream()
              .map(e -> {
                  marshaller.reset();
                  e.accept(marshaller);

                  Map<String, HeaderValue> headers = new HashMap<>();
                  headers.put(":message-type", HeaderValue.fromString("event"));
                  headers.put(":event-type", HeaderValue.fromString(e.sdkEventType().toString()));

                  return new Message(headers, marshaller.marshalledBytes());
              })
              .forEach(m -> m.encode(eventStreamBytes));


        return eventStreamBytes.toByteArray();
    }

    private static class MarshallingVisitor implements SelectObjectContentResponseHandler.Visitor {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void visitEnd(EndEvent event) {
            // no payload
        }

        @Override
        public void visitCont(ContinuationEvent event) {
            // no payload
        }

        @Override
        public void visitRecords(RecordsEvent event) {
            writeUnchecked(event.payload().asByteArray());
        }

        @Override
        public void visitStats(StatsEvent event) {
            Stats details = event.details();
            writeUnchecked(bytes("<Details>"));
            writeUnchecked(bytes("<BytesScanned>"));
            writeUnchecked(bytes(details.bytesScanned().toString()));
            writeUnchecked(bytes("</BytesScanned>"));
            writeUnchecked(bytes("<BytesProcessed>"));
            writeUnchecked(bytes(details.bytesProcessed().toString()));
            writeUnchecked(bytes("</BytesProcessed>"));
            writeUnchecked(bytes("<BytesReturned>"));
            writeUnchecked(bytes(details.bytesReturned().toString()));
            writeUnchecked(bytes("</BytesReturned>"));
            writeUnchecked(bytes("</Details>"));
        }

        @Override
        public void visitProgress(ProgressEvent event) {
            Progress details = event.details();
            writeUnchecked(bytes("<Details>"));
            writeUnchecked(bytes("<BytesScanned>"));
            writeUnchecked(bytes(details.bytesScanned().toString()));
            writeUnchecked(bytes("</BytesScanned>"));
            writeUnchecked(bytes("<BytesProcessed>"));
            writeUnchecked(bytes(details.bytesProcessed().toString()));
            writeUnchecked(bytes("</BytesProcessed>"));
            writeUnchecked(bytes("<BytesReturned>"));
            writeUnchecked(bytes(details.bytesReturned().toString()));
            writeUnchecked(bytes("</BytesReturned>"));
            writeUnchecked(bytes("</Details>"));
        }

        public byte[] marshalledBytes() {
            return baos.toByteArray();
        }

        public void reset() {
            baos.reset();
        }

        private void writeUnchecked(byte[] data) {
            try {
                baos.write(data);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static CompletableFuture<Void> runSimpleQuery(S3AsyncClient s3, SelectObjectContentResponseHandler handler) {
        InputSerialization inputSerialization = InputSerialization.builder()
                                                                  .csv(CSVInput.builder().build())
                                                                  .compressionType(CompressionType.NONE)
                                                                  .build();


        OutputSerialization outputSerialization = OutputSerialization.builder()
                                                                     .csv(CSVOutput.builder().build())
                                                                     .build();


        SelectObjectContentRequest select = SelectObjectContentRequest.builder()
                                                                      .bucket("test-bucket")
                                                                      .key("test-key")
                                                                      .expression("test-query")
                                                                      .expressionType(ExpressionType.SQL)
                                                                      .inputSerialization(inputSerialization)
                                                                      .outputSerialization(outputSerialization)
                                                                      .build();

        return s3.selectObjectContent(select, handler);
    }

    private static class TestHandler implements SelectObjectContentResponseHandler {
        private List<SelectObjectContentEventStream> receivedEvents = new ArrayList<>();

        @Override
        public void responseReceived(SelectObjectContentResponse response) {
        }

        @Override
        public void onEventStream(SdkPublisher<SelectObjectContentEventStream> publisher) {
            publisher.subscribe(receivedEvents::add);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
        }

        @Override
        public void complete() {
        }
    }
}
