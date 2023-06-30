package software.amazon.awssdk.http.auth.aws;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

public class TestUtils {

    // Helpers for generating test requests
    static <T extends AwsCredentialsIdentity> SyncSignRequest<T> generateBasicRequest(
        T credentials,
        Consumer<? super SdkHttpRequest.Builder> requestOverrides,
        Consumer<? super SyncSignRequest.Builder<T>> signRequestOverrides
    ) {
        byte[] data = new byte[1000];
        ThreadLocalRandom.current().nextBytes(data);
        URI target = URI.create("https://test.com/./foo");

        return SyncSignRequest.builder(credentials)
            .request(SdkHttpRequest.builder()
                .method(SdkHttpMethod.GET)
                .uri(target)
                .encodedPath(target.getPath())
                .build()
                .copy(requestOverrides))
            .payload(() -> new ByteArrayInputStream(data))
            .putProperty(SignerProperty.create(String.class, "RegionName"), "us-west-2")
            .putProperty(SignerProperty.create(String.class, "ServiceSigningName"), "s3")
            .putProperty(SignerProperty.create(Clock.class, "SigningClock"), new TickingClock(Instant.EPOCH))
            .build()
            .copy(signRequestOverrides);
    }

    static <T extends AwsCredentialsIdentity> AsyncSignRequest<T> generateBasicAsyncRequest(
        T credentials,
        Consumer<? super SdkHttpRequest.Builder> requestOverrides,
        Consumer<? super AsyncSignRequest.Builder<T>> signRequestOverrides
    ) {
        return AsyncSignRequest.builder(credentials)
            .request(SdkHttpRequest.builder()
                .method(SdkHttpMethod.POST)
                .encodedPath("/")
                .uri(URI.create("http://demo.us-east-1.amazonaws.com"))
                .build()
                .copy(requestOverrides))
            .putProperty(SignerProperty.create(String.class, "RegionName"), "us-west-2")
            .putProperty(SignerProperty.create(String.class, "ServiceSigningName"), "s3")
            .putProperty(SignerProperty.create(Clock.class, "SigningClock"), new TickingClock(Instant.EPOCH))
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

    static class TickingClock extends Clock {
        private final Duration tick = Duration.ofSeconds(1);
        private Instant time;

        TickingClock(Instant time) {
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
