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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Verifies that {@link FastV4HeaderSigner} produces byte-identical output to the legacy SigV4 signing pipeline for
 * every input shape that the fast path's gating function accepts.
 *
 * <p>Each test signs the same {@link SignRequest} through both paths via
 * {@link DefaultAwsV4HttpSigner#signLegacyPath} and {@link DefaultAwsV4HttpSigner#sign} (which dispatches to the
 * fast path for these inputs), then asserts the resulting {@code Authorization}, {@code X-Amz-Date}, and
 * {@code x-amz-content-sha256} headers match exactly. If any signing-relevant field diverges (canonical headers,
 * canonical query, body hash, signing-key derivation, ASCII byte path), the signatures will differ.
 */
class FastV4HeaderSignerTest {

    private static final Clock FIXED_CLOCK =
        Clock.fixed(Instant.parse("2024-01-01T12:34:56Z"), ZoneOffset.UTC);

    private final DefaultAwsV4HttpSigner signer = new DefaultAwsV4HttpSigner();

    @Test
    @DisplayName("byte-equivalent: minimal POST with body")
    void minimalPostWithBody() {
        assertSameAsLegacy(basicPost(awsCreds(), c -> {}));
    }

    @Test
    @DisplayName("byte-equivalent: POST without body")
    void postWithoutBody() {
        assertSameAsLegacy(SignRequest.builder(awsCreds())
                                      .request(httpsRequest(b -> {}))
                                      .putProperty(REGION_NAME, "us-east-1")
                                      .putProperty(SERVICE_SIGNING_NAME, "demo")
                                      .putProperty(SIGNING_CLOCK, FIXED_CLOCK)
                                      .build());
    }

    @Test
    @DisplayName("byte-equivalent: GET with query parameters")
    void getWithQueryParameters() {
        SignRequest<? extends AwsCredentialsIdentity> req = SignRequest.builder(awsCreds())
            .request(httpsRequest(b -> b.method(SdkHttpMethod.GET)
                                        .putRawQueryParameter("foo", "bar")
                                        .putRawQueryParameter("alpha", "beta")
                                        .putRawQueryParameter("zulu", "x")))
            .putProperty(REGION_NAME, "us-east-1")
            .putProperty(SERVICE_SIGNING_NAME, "demo")
            .putProperty(SIGNING_CLOCK, FIXED_CLOCK)
            .build();
        assertSameAsLegacy(req);
    }

    @Test
    @DisplayName("byte-equivalent: header values with whitespace get trimmed and collapsed")
    void headerWithWhitespace() {
        assertSameAsLegacy(basicPost(awsCreds(),
                                     b -> b.putHeader("x-amz-archive-description", "  test   test  ")));
    }

    @Test
    @DisplayName("byte-equivalent: ignored headers don't appear in canonical or signed-headers")
    void ignoredHeaders() {
        assertSameAsLegacy(basicPost(awsCreds(), b -> b
            .putHeader("X-Amzn-Trace-Id", "Root=1-aaaa-bbbb")
            .putHeader("User-Agent", "my-test-agent/1.0")
            .putHeader("Connection", "keep-alive")
            .putHeader("expect", "100-continue")));
    }

    @Test
    @DisplayName("byte-equivalent: session credentials emit x-amz-security-token")
    void sessionCredentials() {
        AwsSessionCredentialsIdentity creds =
            AwsSessionCredentialsIdentity.create("access", "secret", "session-token-value");
        assertSameAsLegacy(basicPost(creds, c -> {}));
    }

    @Test
    @DisplayName("byte-equivalent: payload signing disabled emits UNSIGNED-PAYLOAD")
    void payloadSigningDisabled() {
        SignRequest<? extends AwsCredentialsIdentity> req =
            ((SignRequest.Builder<AwsCredentialsIdentity>)
                basicPost(awsCreds(), c -> {}).toBuilder())
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
                .build();
        assertSameAsLegacy(req);
    }

    @Test
    @DisplayName("byte-equivalent: multi-valued header values are comma-joined")
    void multiValuedHeader() {
        assertSameAsLegacy(basicPost(awsCreds(), b -> b
            .appendHeader("x-amz-meta-tag", "first")
            .appendHeader("x-amz-meta-tag", "second")));
    }

    @Test
    @DisplayName("byte-equivalent: mixed-case header names get lowercased in canonical")
    void mixedCaseHeaderNames() {
        assertSameAsLegacy(basicPost(awsCreds(), b -> b
            .putHeader("X-Custom-Header", "value-one")
            .putHeader("Content-Type", "application/json")));
    }

    @Test
    @DisplayName("byte-equivalent: Host header on non-default port")
    void nonStandardPort() {
        SignRequest<? extends AwsCredentialsIdentity> req = SignRequest.builder(awsCreds())
            .request(SdkHttpRequest.builder()
                                   .method(SdkHttpMethod.POST)
                                   .uri(URI.create("https://demo.us-east-1.amazonaws.com:8443/"))
                                   .encodedPath("/")
                                   .build())
            .putProperty(REGION_NAME, "us-east-1")
            .putProperty(SERVICE_SIGNING_NAME, "demo")
            .putProperty(SIGNING_CLOCK, FIXED_CLOCK)
            .payload(() -> new ByteArrayInputStream("hello".getBytes()))
            .build();
        assertSameAsLegacy(req);
    }

    @Test
    @DisplayName("byte-equivalent: nested URI path with normalize behaviour")
    void nestedPath() {
        SignRequest<? extends AwsCredentialsIdentity> req = SignRequest.builder(awsCreds())
            .request(httpsRequest(b -> b.encodedPath("/foo/bar/../baz")))
            .putProperty(REGION_NAME, "us-east-1")
            .putProperty(SERVICE_SIGNING_NAME, "demo")
            .putProperty(SIGNING_CLOCK, FIXED_CLOCK)
            .payload(() -> new ByteArrayInputStream("body".getBytes()))
            .build();
        assertSameAsLegacy(req);
    }

    /**
     * Sign through both paths and assert byte equality of the headers the signer is responsible for. Calls into the
     * fast path on the first run and the legacy path on the second so that the signing-key cache has at most one
     * derivation that depends on the (test) credentials per UTC day; the cache is shared between paths.
     */
    private void assertSameAsLegacy(SignRequest<? extends AwsCredentialsIdentity> request) {
        SignedRequest fast = signer.sign(request);
        SignedRequest legacy = signer.signLegacyPath(request);

        String fastAuth = fast.request().firstMatchingHeader("Authorization").orElse("");
        String legacyAuth = legacy.request().firstMatchingHeader("Authorization").orElse("");
        String fastDate = fast.request().firstMatchingHeader("X-Amz-Date").orElse("");
        String legacyDate = legacy.request().firstMatchingHeader("X-Amz-Date").orElse("");
        String fastHash = fast.request().firstMatchingHeader("x-amz-content-sha256").orElse("");
        String legacyHash = legacy.request().firstMatchingHeader("x-amz-content-sha256").orElse("");

        assertThat(fastAuth)
            .as("Authorization header byte-equivalent")
            .isEqualTo(legacyAuth)
            .isNotEmpty();
        assertThat(fastDate)
            .as("X-Amz-Date byte-equivalent")
            .isEqualTo(legacyDate);
        assertThat(fastHash)
            .as("x-amz-content-sha256 byte-equivalent")
            .isEqualTo(legacyHash);
    }

    private static AwsCredentialsIdentity awsCreds() {
        return AwsCredentialsIdentity.create("access", "secret");
    }

    private static SignRequest<AwsCredentialsIdentity> basicPost(
        AwsCredentialsIdentity credentials,
        Consumer<? super SdkHttpRequest.Builder> overrides) {
        return SignRequest.builder(credentials)
                          .request(httpsRequest(overrides))
                          .payload(() -> new ByteArrayInputStream("{\"TableName\":\"foo\"}".getBytes()))
                          .putProperty(REGION_NAME, "us-east-1")
                          .putProperty(SERVICE_SIGNING_NAME, "demo")
                          .putProperty(SIGNING_CLOCK, FIXED_CLOCK)
                          .build();
    }

    private static SdkHttpRequest httpsRequest(Consumer<? super SdkHttpRequest.Builder> overrides) {
        SdkHttpRequest.Builder builder = SdkHttpRequest.builder()
                                                       .protocol("https")
                                                       .method(SdkHttpMethod.POST)
                                                       .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                       .putHeader("x-amz-archive-description", "test  test")
                                                       .encodedPath("/")
                                                       .uri(URI.create("https://demo.us-east-1.amazonaws.com"));
        overrides.accept(builder);
        return builder.build();
    }
}
