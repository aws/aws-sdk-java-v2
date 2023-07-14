package software.amazon.awssdk.http.auth.eventstream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.eventstream.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.AUTHORIZATION;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.X_AMZ_DATE;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.auth.internal.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.eventstream.internal.DefaultAwsV4EventStreamHttpSigner;
import software.amazon.awssdk.http.auth.internal.AwsV4HeaderHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

public class AwsV4EventStreamHttpSignerTest {

    private static final BaseAwsV4HttpSigner<?> signer = new DefaultAwsV4EventStreamHttpSigner(
        new AwsV4HeaderHttpSigner(
            BaseAwsV4HttpSigner.create()
        )
    );

    @Test
    public void signAsync_shouldSignWithTransformedPublisher() {
        String expectedAuthorizationHeader = "AWS4-HMAC-SHA256 Credential=a/20200101/us-west-2/name/aws4_request, " +
            "SignedHeaders=host;x-amz-content-sha256;x-amz-date, " +
            "Signature=f21ae834dae1ebd27ccd3d1b8d93514ec914d4cd79575edd906667e9e12e591b";

        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("a", "s"),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

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

    @Test
    public void sign_shouldThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> signer.sign(
            (SyncSignRequest<? extends AwsCredentialsIdentity>) null));
    }

    private List<Message> readMessages(Publisher<ByteBuffer> payload) {
        MessageDecoder decoder = new MessageDecoder();

        Flowable.fromPublisher(payload)
            .blockingForEach(x ->
                decoder.feed(x.array())
            );
        return decoder.getDecodedMessages();
    }

    @Test
    public void signAsync_withSessionCredentials_shouldSignAndAddTokenHeader() {
        String expectedAuthorizationHeader = "AWS4-HMAC-SHA256 Credential=a/20200101/us-west-2/name/aws4_request, " +
            "SignedHeaders=host;x-amz-content-sha256;x-amz-date;x-amz-security-token, " +
            "Signature=2387417f091dc51839800a9044510ce172ee5a064fec1733cd480686ad550aa0";
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsSessionCredentialsIdentity.create("a", "s", "t"),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader(AUTHORIZATION)).hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader(X_AMZ_CONTENT_SHA256))
            .hasValue("STREAMING-AWS4-HMAC-SHA256-EVENTS");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Security-Token")).hasValue("t");
        assertThat(signedRequest.request().firstMatchingHeader(X_AMZ_DATE)).hasValue("20200101T235959Z");
        assertThat(signedRequest.request().firstMatchingHeader(AUTHORIZATION)).hasValue(expectedAuthorizationHeader);
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature.
     */
    @Test
    public void signAsync_withAnonymousCredentials_shouldNotSign() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            new TestUtils.AnonymousCredentialsIdentity(),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertEquals(signedRequest.request().headers().size(), 0);
    }
}
