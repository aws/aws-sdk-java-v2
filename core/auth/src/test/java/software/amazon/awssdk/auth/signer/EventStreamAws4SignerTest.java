package software.amazon.awssdk.auth.signer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.internal.SignerTestUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

public class EventStreamAws4SignerTest {
    /**
     * Verify that when an event stream is open from one day to the next, the signature is properly signed for the day of the
     * event.
     */
    @Test
    public void openStreamEventSignaturesCanRollOverBetweenDays() {
        EventStreamAws4Signer signer = EventStreamAws4Signer.create();

        Region region = Region.US_WEST_2;
        AwsCredentials credentials = AwsBasicCredentials.create("a", "s");
        String signingName = "name";
        AdjustableClock clock = new AdjustableClock();
        clock.time = Instant.parse("2020-01-01T23:59:59Z");

        SdkHttpFullRequest initialRequest = SdkHttpFullRequest.builder()
                                                              .uri(URI.create("http://localhost:8080"))
                                                              .method(SdkHttpMethod.GET)
                                                              .build();
        SdkHttpFullRequest signedRequest = SignerTestUtils.signRequest(signer, initialRequest, credentials, signingName, clock,
                                                                       region.id());

        ByteBuffer event = new Message(Collections.emptyMap(), "foo".getBytes(UTF_8)).toByteBuffer();

        Callable<ByteBuffer> lastEvent = () -> {
            clock.time = Instant.parse("2020-01-02T00:00:00Z");
            return event;
        };

        AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(Flowable.concatArray(Flowable.just(event),
                                                                                           Flowable.fromCallable(lastEvent)));

        AsyncRequestBody signedBody = SignerTestUtils.signAsyncRequest(signer, signedRequest, requestBody, credentials,
                                                                       signingName, clock, region.id());

        List<Message> signedMessages = readMessages(signedBody);
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

    private List<Message> readMessages(AsyncRequestBody signedBody) {
        MessageDecoder decoder = new MessageDecoder();
        Flowable.fromPublisher(signedBody).blockingForEach(x -> decoder.feed(x.array()));
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