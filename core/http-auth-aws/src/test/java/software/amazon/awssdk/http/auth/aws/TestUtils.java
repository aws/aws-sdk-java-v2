package software.amazon.awssdk.http.auth.aws;

import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.async.SimplePublisher;

public final class TestUtils {

    private TestUtils() {
    }

    // Helpers for generating test requests
    public static <T extends AwsCredentialsIdentity> SignRequest<T> generateBasicRequest(
        T credentials,
        Consumer<? super SdkHttpRequest.Builder> requestOverrides,
        Consumer<? super SignRequest.Builder<T>> signRequestOverrides
    ) {
        return SignRequest.builder(credentials)
                          .request(SdkHttpRequest.builder()
                                                     .method(SdkHttpMethod.POST)
                                                     .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                     .putHeader("x-amz-archive-description", "test  test")
                                                     .encodedPath("/")
                                                     .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                     .build()
                                                     .copy(requestOverrides))
                          .payload(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                          .putProperty(REGION_NAME, "us-east-1")
                          .putProperty(SERVICE_SIGNING_NAME, "demo")
                          .putProperty(SIGNING_CLOCK,
                                           new TickingClock(Instant.ofEpochMilli(351153000968L)))
                          .build()
                          .copy(signRequestOverrides);
    }

    public static <T extends AwsCredentialsIdentity> AsyncSignRequest<T> generateBasicAsyncRequest(
        T credentials,
        Consumer<? super SdkHttpRequest.Builder> requestOverrides,
        Consumer<? super AsyncSignRequest.Builder<T>> signRequestOverrides
    ) {
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();

        publisher.send(ByteBuffer.wrap("{\"TableName\": \"foo\"}".getBytes()));
        publisher.complete();

        return AsyncSignRequest.builder(credentials)
                               .request(SdkHttpRequest.builder()
                                                      .protocol("https")
                                                      .method(SdkHttpMethod.POST)
                                                      .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                      .putHeader("x-amz-archive-description", "test  test")
                                                      .encodedPath("/")
                                                      .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                      .build()
                                                      .copy(requestOverrides))
                               .payload(publisher)
                               .putProperty(REGION_NAME, "us-east-1")
                               .putProperty(SERVICE_SIGNING_NAME, "demo")
                               .putProperty(SIGNING_CLOCK,
                                            new TickingClock(Instant.ofEpochMilli(351153000968L)))
                               .build()
                               .copy(signRequestOverrides);
    }

    public static class AnonymousCredentialsIdentity implements AwsCredentialsIdentity {

        @Override
        public String accessKeyId() {
            return null;
        }

        @Override
        public String secretAccessKey() {
            return null;
        }
    }

    public static class TickingClock extends Clock {
        private final Duration tick = Duration.ofSeconds(1);
        private Instant time;

        public TickingClock(Instant time) {
            this.time = time;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            Instant time = this.time;
            this.time = time.plus(tick);
            return time;
        }
    }
}
