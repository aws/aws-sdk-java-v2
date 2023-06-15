package software.amazon.awssdk.http.auth;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class TestUtils {

    // Helpers for generating test requests
    static SyncSignRequest<? extends AwsCredentialsIdentity> generateBasicRequest(
        SdkHttpRequest.Builder requestBuilder,
        SyncSignRequest.Builder<? extends AwsCredentialsIdentity> signRequestBuilder
    ) {
        return signRequestBuilder
            .request(requestBuilder
                .method(SdkHttpMethod.POST)
                .putHeader("Host", "demo.us-east-1.amazonaws.com")
                .putHeader("x-amz-archive-description", "test  test")
                .encodedPath("/")
                .uri(URI.create("http://demo.us-east-1.amazonaws.com"))
                .build())
            .payload(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
            .putProperty(SignerProperty.create(String.class, "RegionName"), "us-east-1")
            .putProperty(SignerProperty.create(String.class, "ServiceSigningName"), "demo")
            .putProperty(SignerProperty.create(Boolean.class, "DoubleUrlEncode"), false)
            .putProperty(SignerProperty.create(Boolean.class, "NormalizePath"), false)
            .putProperty(SignerProperty.create(Instant.class, "RequestSigningInstant"), Instant.ofEpochMilli(351153000968L))
            .build();
    }

    static AsyncSignRequest<? extends AwsCredentialsIdentity> generateBasicAsyncRequest(
        SdkHttpRequest.Builder requestBuilder,
        AsyncSignRequest.Builder<? extends AwsCredentialsIdentity> signRequestBuilder
    ) {

        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();

        publisher.send(ByteBuffer.wrap("{\"TableName\": \"foo\"}".getBytes()));
        publisher.complete();

        return signRequestBuilder
            .request(requestBuilder
                .method(SdkHttpMethod.POST)
                .putHeader("Host", "demo.us-east-1.amazonaws.com")
                .putHeader("x-amz-archive-description", "test  test")
                .encodedPath("/")
                .uri(URI.create("http://demo.us-east-1.amazonaws.com"))
                .build())
            .payload(publisher)
            .putProperty(SignerProperty.create(String.class, "RegionName"), "us-east-1")
            .putProperty(SignerProperty.create(String.class, "ServiceSigningName"), "demo")
            .putProperty(SignerProperty.create(Boolean.class, "DoubleUrlEncode"), false)
            .putProperty(SignerProperty.create(Boolean.class, "NormalizePath"), false)
            .putProperty(SignerProperty.create(Instant.class, "RequestSigningInstant"), Instant.ofEpochMilli(351153000968L))
            .build();
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
}
