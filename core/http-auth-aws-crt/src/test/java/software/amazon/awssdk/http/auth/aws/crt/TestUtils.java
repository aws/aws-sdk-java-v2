package software.amazon.awssdk.http.auth.aws.crt;

import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtHttpRequestConverter.toRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.sanitizeRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.toCredentials;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningUtils;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

public class TestUtils {

    private static final String TEST_VERIFICATION_PUB_X = "b6618f6a65740a99e650b33b6b4b5bd0d43b176d721a3edfea7e7d2d56d936b1";
    private static final String TEST_VERIFICATION_PUB_Y = "865ed22a7eadc9c5cb9d2cbaca1b3699139fedc5043dc6661864218330c8e518";

    // Helpers for generating test requests
    public static <T extends AwsCredentialsIdentity> SyncSignRequest<T> generateBasicRequest(
        T credentials,
        Consumer<? super SdkHttpRequest.Builder> requestOverrides,
        Consumer<? super SyncSignRequest.Builder<T>> signRequestOverrides
    ) {
        return SyncSignRequest.builder(credentials)
                              .request(SdkHttpRequest.builder()
                                                     .method(SdkHttpMethod.POST)
                                                     .putHeader("x-amz-archive-description", "test  test")
                                                     .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                     .encodedPath("/")
                                                     .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                     .build()
                                                     .copy(requestOverrides))
                              .payload(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                              .putProperty(AwsV4aHttpSigner.REGION_NAME, "aws-global")
                              .putProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "demo")
                              .putProperty(HttpSigner.SIGNING_CLOCK, new TickingClock(Instant.ofEpochMilli(1596476903000L)))
                              .build()
                              .copy(signRequestOverrides);
    }

    public static AwsSigningConfig generateBasicSigningConfig(AwsCredentialsIdentity credentials) {
        try (AwsSigningConfig signingConfig = new AwsSigningConfig()) {
            signingConfig.setCredentials(toCredentials(credentials));
            signingConfig.setService("demo");
            signingConfig.setRegion("aws-global");
            signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
            signingConfig.setTime(1596476903000L);
            signingConfig.setUseDoubleUriEncode(true);
            signingConfig.setShouldNormalizeUriPath(true);
            return signingConfig;
        }
    }

    public static boolean verifyEcdsaSignature(SdkHttpRequest request,
                                               ContentStreamProvider payload,
                                               String expectedCanonicalRequest,
                                               AwsSigningConfig signingConfig,
                                               String signatureValue) {
        HttpRequest crtRequest = toRequest(sanitizeRequest(request), payload);

        return AwsSigningUtils.verifySigv4aEcdsaSignature(crtRequest, expectedCanonicalRequest, signingConfig,
                                                          signatureValue.getBytes(), TEST_VERIFICATION_PUB_X,
                                                          TEST_VERIFICATION_PUB_Y);
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
