package software.amazon.awssdk.http.auth.eventstream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.AUTHORIZATION;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.X_AMZ_DATE;

import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

public class AwsV4EventStreamHttpSignerTest {

    private static final AwsV4EventStreamHttpSigner signer = AwsV4EventStreamHttpSigner.create();

    @Test
    public void signAsync_shouldSignWithTransformedPublisher() {
        AdjustableClock clock = new AdjustableClock();
        clock.time = Instant.parse("2020-01-01T23:59:59Z");
        ByteBuffer event = new Message(Collections.emptyMap(), "foo".getBytes(UTF_8)).toByteBuffer();
        Callable<ByteBuffer> lastEvent = () -> {
            clock.time = Instant.parse("2020-01-02T00:00:00Z");
            return event;
        };
        Publisher<ByteBuffer> payload = Flowable.concatArray(Flowable.just(event), Flowable.fromCallable(lastEvent));
        AsyncSignRequest<? extends AwsCredentialsIdentity> signRequest =
            AsyncSignRequest.builder(AwsCredentialsIdentity.create("a", "s"))
                .request(SdkHttpRequest.builder()
                    .method(SdkHttpMethod.GET)
                    .uri(URI.create("http://localhost:8080"))
                    .build())
                .payload(payload)
                .putProperty(SignerProperty.create(String.class, "RegionName"), "us-west-2")
                .putProperty(SignerProperty.create(String.class, "ServiceSigningName"), "name")
                .putProperty(SignerProperty.create(Clock.class, "SigningClock"), clock)
                .build();
        String expectedAuthorizationHeader = "AWS4-HMAC-SHA256 Credential=a/20200101/us-west-2/name/aws4_request, " +
            "SignedHeaders=host;x-amz-content-sha256;x-amz-date, " +
            "Signature=f21ae834dae1ebd27ccd3d1b8d93514ec914d4cd79575edd906667e9e12e591b";


        AsyncSignedRequest signedRequest = signer.signAsync(signRequest);

        assertThat(signedRequest.request().firstMatchingHeader(AUTHORIZATION)).hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader(X_AMZ_CONTENT_SHA256))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-EVENTS");
        assertThat(signedRequest.request().firstMatchingHeader(X_AMZ_DATE)).hasValue("20200101T235959Z");

        List<Message> signedMessages = readMessages(signedRequest.payload().get());
        assertThat(signedMessages.size()).isEqualTo(3);

        Map<String, HeaderValue> firstMessageHeaders = signedMessages.get(0).getHeaders();
        assertThat(firstMessageHeaders.get(":date").getTimestamp()).isEqualTo("2020-01-01T23:59:59Z");
        assertThat(Base64.getEncoder().encodeToString(firstMessageHeaders.get(":chunk-signature").getByteArray()))
            .isEqualTo("EFt7ZU043r/TJE8U+1GxJXscmNxoqmIdGtUIl8wE9u0=");

        Map<String, HeaderValue> lastMessageHeaders = signedMessages.get(2).getHeaders();
        assertThat(lastMessageHeaders.get(":date").getTimestamp()).isEqualTo("2020-01-02T00:00:00Z");
        assertThat(Base64.getEncoder().encodeToString(lastMessageHeaders.get(":chunk-signature").getByteArray()))
            .isEqualTo("UTRGo0D7BQytiVkH1VofR/8f3uFsM4V5QR1A8grr1+M=");
    }

    private List<Message> readMessages(Publisher<ByteBuffer> payload) {
        MessageDecoder decoder = new MessageDecoder();

        Flowable.fromPublisher(payload)
            .blockingForEach(x ->
                decoder.feed(x.array())
            );
        return decoder.getDecodedMessages();
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
