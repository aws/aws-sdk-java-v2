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

package software.amazon.awssdk.services;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.CSVInput;
import software.amazon.awssdk.services.s3.model.CSVOutput;
import software.amazon.awssdk.services.s3.model.CompressionType;
import software.amazon.awssdk.services.s3.model.ExpressionType;
import software.amazon.awssdk.services.s3.model.InputSerialization;
import software.amazon.awssdk.services.s3.model.OutputSerialization;
import software.amazon.awssdk.services.s3.model.RecordsEvent;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream;
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream.EventType;
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class SelectObjectContentIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET_NAME = temporaryBucketName(SelectObjectContentIntegrationTest.class);
    private static final String KEY = "test_object.csv";
    private static final String CSV_CONTENTS = "A,B\n"
                                               + "C,D";
    private static final String QUERY = "select s._1 from S3Object s";

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        s3.createBucket(r -> r.bucket(BUCKET_NAME));
        s3.waiter().waitUntilBucketExists(r -> r.bucket(BUCKET_NAME));
        s3.putObject(r -> r.bucket(BUCKET_NAME).key(KEY), RequestBody.fromString(CSV_CONTENTS));
    }

    private static Stream<S3AsyncClient> s3AsyncClients() {
        return Stream.of(crtClientBuilder().build(), s3AsyncClientBuilder().build());
    }

    @AfterAll
    public static void teardown() {
        try {
            deleteBucketAndAllContents(BUCKET_NAME);
        } finally {
            s3AsyncClients().forEach(SdkAutoCloseable::close);
            s3.close();
        }
    }

    @ParameterizedTest
    @MethodSource("s3AsyncClients")
    public void selectObjectContent_onResponseInvokedWithResponse(S3AsyncClient client) {
        TestHandler handler = new TestHandler();
        executeSqlQueryWithHandler(QUERY, handler, client).join();

        assertThat(handler.response).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("s3AsyncClients")
    public void selectObjectContent_recordsEventUnmarshalledCorrectly(S3AsyncClient client) {
        TestHandler handler = new TestHandler();
        executeSqlQueryWithHandler(QUERY, handler, client).join();

        RecordsEvent recordsEvent = (RecordsEvent) handler.receivedEvents.stream()
                                                                         .filter(e -> e.sdkEventType() == EventType.RECORDS)
                                                                         .findFirst()
                                                                         .orElse(null);

        assertThat(recordsEvent.payload().asUtf8String()).contains("A\nC");
    }

    @ParameterizedTest
    @MethodSource("s3AsyncClients")
    public void selectObjectContent_invalidQuery_unmarshallsErrorResponse(S3AsyncClient client) {
        TestHandler handler = new TestHandler();
        CompletableFuture<Void> responseFuture = executeSqlQueryWithHandler("not a query", handler, client);

        assertThatThrownBy(responseFuture::join).hasCauseInstanceOf(S3Exception.class);
    }

    private static CompletableFuture<Void> executeSqlQueryWithHandler(String query, SelectObjectContentResponseHandler handler,
                                                                      S3AsyncClient client) {
        InputSerialization inputSerialization = InputSerialization.builder()
                                                                  .csv(CSVInput.builder().build())
                                                                  .compressionType(CompressionType.NONE)
                                                                  .build();


        OutputSerialization outputSerialization = OutputSerialization.builder()
                                                                     .csv(CSVOutput.builder().build())
                                                                     .build();


        SelectObjectContentRequest select = SelectObjectContentRequest.builder()
                                                                      .bucket(BUCKET_NAME)
                                                                      .key(KEY)
                                                                      .expression(query)
                                                                      .expressionType(ExpressionType.SQL)
                                                                      .inputSerialization(inputSerialization)
                                                                      .outputSerialization(outputSerialization)
                                                                      .build();

        return client.selectObjectContent(select, handler);
    }

    private static class TestHandler implements SelectObjectContentResponseHandler {
        private SelectObjectContentResponse response;
        private List<SelectObjectContentEventStream> receivedEvents = new ArrayList<>();
        private Throwable exception;

        @Override
        public void responseReceived(SelectObjectContentResponse response) {
            this.response = response;
        }

        @Override
        public void onEventStream(SdkPublisher<SelectObjectContentEventStream> publisher) {
            publisher.subscribe(receivedEvents::add);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            exception = throwable;
        }

        @Override
        public void complete() {
        }
    }
}
