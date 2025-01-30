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
package software.amazon.awssdk.services.transcribestreaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.utils.Logger;

/**
 * An example test class to show the usage of
 * {@link TranscribeStreamingAsyncClient#startStreamTranscription(StartStreamTranscriptionRequest, Publisher,
 * StartStreamTranscriptionResponseHandler)} API.
 *
 * The audio files used in this class don't have voice, so there won't be any transcripted text would be empty
 */
public class TranscribeStreamingIntegrationTest {
    private static final Logger log = Logger.loggerFor(TranscribeStreamingIntegrationTest.class);
    private TranscribeStreamingAsyncClient client;
    private MetricPublisher mockPublisher;

    private static Stream<ProtocolNegotiation> protocolNegotiations() {
        return Stream.of(ProtocolNegotiation.ASSUME_PROTOCOL, ProtocolNegotiation.ALPN);
    }

    private static boolean alpnSupported(){
        return NettyUtils.isAlpnSupported(SslProvider.JDK);
    }

    @ParameterizedTest
    @MethodSource("protocolNegotiations")
    @EnabledIf("alpnSupported")
    public void testFileWith16kRate(ProtocolNegotiation protocolNegotiation) throws Exception {
        initClient(protocolNegotiation);

        CompletableFuture<Void> result = client.startStreamTranscription(
            getRequest(16_000),
            new AudioStreamPublisher(getInputStream("silence_16kHz_s16le.wav")),
            TestResponseHandlers.responseHandlerBuilder_Classic());

        result.join();
        verifyMetrics();
    }

    @ParameterizedTest
    @MethodSource("protocolNegotiations")
    @EnabledIf("alpnSupported")
    public void testFileWith8kRate(ProtocolNegotiation protocolNegotiation) throws Exception {
        initClient(protocolNegotiation);

        CompletableFuture<Void> result = client.startStreamTranscription(
            getRequest(8_000),
            new AudioStreamPublisher(getInputStream("silence_8kHz_s16le.wav")),
            TestResponseHandlers.responseHandlerBuilder_Consumer());

        result.get();
    }

    private void initClient(ProtocolNegotiation protocolNegotiation) {
        if (client != null) {
            client.close();
        }
        if (mockPublisher != null) {
            mockPublisher.close();
        }

        mockPublisher = mock(MetricPublisher.class);
        client = TranscribeStreamingAsyncClient.builder()
                                               .region(Region.US_EAST_1)
                                               .overrideConfiguration(b -> b.addExecutionInterceptor(new VerifyHeaderInterceptor())
                                                                            .addMetricPublisher(mockPublisher))
                                               .credentialsProvider(getCredentials())
                                               .httpClient(NettyNioAsyncHttpClient.builder()
                                                                                  .protocol(Protocol.HTTP2)
                                                                                  .protocolNegotiation(protocolNegotiation)
                                                                                  .build())
                                               .build();
    }

    private static AwsCredentialsProvider getCredentials() {
        return DefaultCredentialsProvider.create();
    }

    private StartStreamTranscriptionRequest getRequest(Integer mediaSampleRateHertz) {
        return StartStreamTranscriptionRequest.builder()
                                              .languageCode(LanguageCode.EN_US.toString())
                                              .mediaEncoding(MediaEncoding.PCM)
                                              .mediaSampleRateHertz(mediaSampleRateHertz)
                                              .build();
    }

    private InputStream getInputStream(String audioFileName) {
        try {
            File inputFile = new File(getClass().getClassLoader().getResource(audioFileName).getFile());
            assertThat(inputFile).exists();
            return new FileInputStream(inputFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class AudioStreamPublisher implements Publisher<AudioStream> {
        private final InputStream inputStream;

        private AudioStreamPublisher(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new TestSubscription(s, inputStream));
        }
    }

    private static class VerifyHeaderInterceptor implements ExecutionInterceptor {

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            List<String> contentTypeHeader = context.httpRequest().headers().get(CONTENT_TYPE);
            assertThat(contentTypeHeader.size()).isEqualTo(1);
            assertThat(contentTypeHeader.get(0)).isEqualTo(Mimetype.MIMETYPE_EVENT_STREAM);
        }
    }

    private void verifyMetrics() throws InterruptedException {
        // wait for 100ms for metrics to be delivered to mockPublisher
        Thread.sleep(100);
        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());
        MetricCollection capturedCollection = collectionCaptor.getValue();
        assertThat(capturedCollection.name()).isEqualTo("ApiCall");
        log.info(() -> "captured collection: " + capturedCollection);

        assertThat(capturedCollection.metricValues(CoreMetric.CREDENTIALS_FETCH_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.MARSHALLING_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_DURATION).get(0))
            .isGreaterThan(Duration.ZERO);

        MetricCollection attemptCollection = capturedCollection.children().get(0);
        assertThat(attemptCollection.name()).isEqualTo("ApiCallAttempt");
        assertThat(attemptCollection.metricValues(HttpMetric.HTTP_STATUS_CODE))
            .containsExactly(200);
        assertThat(attemptCollection.metricValues(CoreMetric.SIGNING_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(attemptCollection.metricValues(CoreMetric.AWS_REQUEST_ID).get(0)).isNotEmpty();

        assertThat(attemptCollection.metricValues(CoreMetric.SERVICE_CALL_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ofMillis(100));
    }
}
