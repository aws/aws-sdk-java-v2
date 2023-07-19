package software.amazon.awssdk.http.auth.aws.eventstream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.SIGNING_CLOCK;

import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.eventstream.Message;

public class TestUtils {

    // Helpers for generating test requests
    static <T extends AwsCredentialsIdentity> AsyncSignRequest<T> generateBasicAsyncRequest(
        T credentials,
        Consumer<? super SdkHttpRequest.Builder> requestOverrides,
        Consumer<? super AsyncSignRequest.Builder<T>> signRequestOverrides
    ) {
        AdjustableClock clock = new AdjustableClock();
        clock.time = Instant.parse("2020-01-01T23:59:59Z");
        ByteBuffer event = new Message(Collections.emptyMap(), "foo".getBytes(UTF_8)).toByteBuffer();
        Callable<ByteBuffer> lastEvent = () -> {
            clock.time = Instant.parse("2020-01-02T00:00:00Z");
            return event;
        };
        Publisher<ByteBuffer> payload = Flowable.concatArray(Flowable.just(event), Flowable.fromCallable(lastEvent));

        return AsyncSignRequest.builder(credentials)
            .request(SdkHttpRequest.builder()
                .method(SdkHttpMethod.GET)
                .uri(URI.create("http://localhost:8080"))
                .build()
                .copy(requestOverrides))
            .payload(payload)
            .putProperty(REGION_NAME, "us-west-2")
            .putProperty(SERVICE_SIGNING_NAME, "name")
            .putProperty(SIGNING_CLOCK, clock)
            .build()
            .copy(signRequestOverrides);
    }

    static class AnonymousCredentialsIdentity implements AwsCredentialsIdentity {

        @Override
        public String accessKeyId() {
            return null;
        }

        @Override
        public String secretAccessKey() {
            return null;
        }
    }

    private static class AdjustableClock extends Clock {
        private Instant time;

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
            return time;
        }
    }
}
