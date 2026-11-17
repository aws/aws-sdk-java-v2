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

package software.amazon.awssdk.auth.credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

class CredentialDaemonCredentialsProviderTest {
    private static final String DOC = "<CREDENTIAL_DAEMON_DOC>";
    private static final String START_HINT = "Start the credential daemon and sign in; see " + DOC + ".";
    private static final String HEAD = "{\"jsonrpc\":\"2.0\",\"id\":1,";

    /**
     * The secret of the cached credentials in the invalidated-* cases, which no error message may contain.
     */
    private static final String CACHED_SECRET = "s3cr3t";

    /**
     * For credentials that expire 15 minutes after the clock starts: after the jittered prefetch time and before stale.
     */
    private static final Duration IN_PREFETCH_WINDOW = Duration.ofMinutes(13).plusSeconds(30);

    private final EnvironmentVariableHelper env = new EnvironmentVariableHelper();
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private FakeCredentialDaemon daemon;

    @BeforeEach
    void setup() throws Exception {
        assumeTrue(FakeCredentialDaemon.isSupported());
        daemon = startDaemon(FakeCredentialDaemon.successReply("ASIADAEMON", inMinutes(15)));
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", daemon.socketPath().toString());
    }

    @AfterEach
    void teardown() throws Exception {
        env.reset();
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
    }

    private FakeCredentialDaemon startDaemon(String reply) throws Exception {
        FakeCredentialDaemon started = FakeCredentialDaemon.replying(reply);
        closeables.add(started);
        return started;
    }

    private CredentialDaemonCredentialsProvider provider() {
        return provider(CredentialDaemonCredentialsProvider.builder().credentialName("dev"));
    }

    private CredentialDaemonCredentialsProvider provider(CredentialDaemonCredentialsProvider.Builder builder) {
        CredentialDaemonCredentialsProvider provider = builder.build();
        closeables.add(provider);
        return provider;
    }

    private CredentialDaemonCredentialsProvider provider(Clock clock) {
        return provider(builder(clock).credentialName("dev"));
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static Instant inMinutes(long minutes) {
        return Instant.now().plus(Duration.ofMinutes(minutes));
    }

    private static String material(String fields) {
        return HEAD + "\"result\":{\"credential\":\"dev\",\"material\":{\"type\":\"session_credentials\"," + fields + "},"
               + "\"expires_at\":\"" + inMinutes(15).truncatedTo(ChronoUnit.SECONDS) + "\"}}";
    }

    private static String withExpiry(String expiresAt) {
        return HEAD + "\"result\":{\"material\":{\"type\":\"session_credentials\",\"access_key_id\":\"AKID\","
               + "\"secret_access_key\":\"SUPERSECRET\",\"session_token\":\"token\"},\"expires_at\":" + expiresAt + "}}";
    }

    private CredentialDaemonException failure() {
        try {
            provider().resolveCredentials();
        } catch (CredentialDaemonException e) {
            return e;
        }
        throw new AssertionError("Expected a CredentialDaemonException");
    }

    @Test
    void resolveCredentials_withSessionToken_returnsSessionCredentials() {
        AwsCredentials credentials = provider().resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
        AwsSessionCredentials session = (AwsSessionCredentials) credentials;
        assertThat(session.accessKeyId()).isEqualTo("ASIADAEMON");
        assertThat(session.secretAccessKey()).isEqualTo("secret");
        assertThat(session.sessionToken()).isEqualTo("token");
        assertThat(session.expirationTime()).isPresent();
        assertThat(session.providerName()).hasValue("Aq");
        assertThat(session.accountId()).isEmpty();
    }

    @Test
    void resolveCredentials_withoutSessionTokenWithAccountId_returnsBasicCredentials() {
        daemon.reply(material("\"access_key_id\":\"AKID\",\"secret_access_key\":\"skid\",\"account_id\":\"123456789012\""));

        AwsCredentials credentials = provider().resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("AKID");
        assertThat(credentials.accountId()).hasValue("123456789012");
        assertThat(credentials.providerName()).hasValue("Aq");
    }

    @Test
    void resolveCredentials_withEmptyOptionalFields_treatsThemAsAbsent() {
        daemon.reply(material("\"access_key_id\":\"AKID\",\"secret_access_key\":\"skid\",\"session_token\":\"\","
                              + "\"account_id\":null"));

        AwsCredentials credentials = provider().resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accountId()).isEmpty();
    }

    @Test
    void resolveCredentials_withSourceChain_appendsDaemonMetric() {
        AwsCredentials credentials =
            provider(CredentialDaemonCredentialsProvider.builder().credentialName("dev").sourceChain("o"))
                .resolveCredentials();

        assertThat(credentials.providerName()).hasValue("o,Aq");
    }

    @Test
    void resolveCredentials_sendsExactRequestLine() {
        provider().resolveCredentials();

        assertThat(daemon.requests())
            .containsExactly("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"credential/get\",\"params\":{\"credential\":\"dev\"}}");
    }

    @Test
    void resolveCredentials_withAccountAndRole_sendsThemInParams() {
        provider(CredentialDaemonCredentialsProvider.builder()
                                                     .credentialName("dev")
                                                     .accountId("123456789012")
                                                     .roleName("Admin\"Role"))
            .resolveCredentials();

        assertThat(daemon.requests())
            .containsExactly("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"credential/get\",\"params\":{\"credential\":\"dev\","
                             + "\"account_id\":\"123456789012\",\"role_name\":\"Admin\\\"Role\"}}");
    }

    @Test
    void resolveCredentials_whenSocketMissing_raisesNotRunningWithPathAndHint() throws Exception {
        Path directory = FakeCredentialDaemon.newSocketDirectory();
        Path missing = directory.resolve("missing.sock");
        closeables.add(() -> Files.deleteIfExists(directory));
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", missing.toString());

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
        assertThat(e.getMessage()).contains(missing.toString())
                                  .contains("No such file or directory")
                                  .contains(START_HINT);
        assertThat(e.getCause()).isNotNull();
    }

    @Test
    void resolveCredentials_whenSocketIsStale_raisesConnectionRefused() throws Exception {
        Path directory = FakeCredentialDaemon.newSocketDirectory();
        Path stale = FakeCredentialDaemon.staleSocket(directory);
        closeables.add(() -> {
            Files.deleteIfExists(stale);
            Files.deleteIfExists(directory);
        });
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", stale.toString());

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
        assertThat(e.getMessage()).contains("Connection refused").contains(stale.toString());
    }

    /**
     * Sets {@code HOME} to a new temporary folder, unsets {@code AWS_CREDENTIAL_DAEMON_SOCKET}, and returns the path of
     * {@code $HOME/.aws/credential-daemon.sock}, whose folder exists.
     */
    private Path linkLocation() throws Exception {
        Path home = FakeCredentialDaemon.newSocketDirectory();
        Path link = home.resolve(".aws").resolve("credential-daemon.sock");
        Files.createDirectories(link.getParent());
        closeables.add(() -> {
            Files.deleteIfExists(link);
            Files.deleteIfExists(link.getParent());
            Files.deleteIfExists(home);
        });
        env.remove("AWS_CREDENTIAL_DAEMON_SOCKET");
        env.set("HOME", home.toString());
        return link;
    }

    @Test
    void resolveCredentials_withLinkToDaemon_returnsDaemonCredentials() throws Exception {
        Files.createSymbolicLink(linkLocation(), daemon.socketPath());

        assertThat(provider().resolveCredentials().accessKeyId()).isEqualTo("ASIADAEMON");
        assertThat(daemon.requests()).hasSize(1);
    }

    @Test
    void resolveCredentials_linkStale_raisesNotRunningNamingTheLink() throws Exception {
        Path link = linkLocation();
        Path directory = FakeCredentialDaemon.newSocketDirectory();
        Path stale = FakeCredentialDaemon.staleSocket(directory);
        closeables.add(() -> {
            Files.deleteIfExists(stale);
            Files.deleteIfExists(directory);
        });
        Files.createSymbolicLink(link, stale);

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
        assertThat(e.getMessage()).contains("Could not connect to the credential daemon at " + link + ":")
                                  .contains("Connection refused")
                                  .doesNotContain(stale.toString())
                                  .contains(START_HINT);
    }

    @Test
    void resolveCredentials_linkDangling_raisesNotRunningNamingTheLinkAndItsTarget() throws Exception {
        Path link = linkLocation();
        Path missing = link.getParent().resolve("missing-daemon.sock");
        Files.createSymbolicLink(link, missing);

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
        assertThat(e.getMessage()).contains(link.toString()).contains(missing.toString()).contains(START_HINT);
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void resolveCredentials_noLink_raisesNotRunningWithInstallHint() throws Exception {
        Path link = linkLocation();

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
        assertThat(e.getMessage()).contains("nothing is at " + link)
                                  .contains("Install the credential daemon or link its socket to "
                                            + "$HOME/.aws/credential-daemon.sock, or set AWS_CREDENTIAL_DAEMON_SOCKET")
                                  .contains(DOC);
    }

    @Test
    void resolveCredentials_noLinkHomeUnset_raisesNotRunningWithInstallHint() throws Exception {
        linkLocation();
        env.remove("HOME");

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
        assertThat(e.getMessage()).contains("HOME is not set to an absolute path")
                                  .contains("link its socket to $HOME/.aws/credential-daemon.sock");
    }

    @Test
    void resolveCredentials_whenDaemonNeverAnswers_raisesNotRunningForCallTimeout() {
        daemon.responder(r -> FakeCredentialDaemon.NO_REPLY);
        CredentialDaemonCredentialsProvider provider =
            provider(CredentialDaemonCredentialsProvider.builder().credentialName("dev").callTimeout(Duration.ofMillis(300)));

        long start = System.nanoTime();
        assertThatThrownBy(provider::resolveCredentials)
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains("call timeout (300 ms)").contains(daemon.socketPath().toString())
                                          .contains(START_HINT);
            });
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(4));
    }

    @Test
    void resolveCredentials_whenBacklogFull_raisesNotRunningAtOnce() throws Exception {
        assumeTrue(FakeCredentialDaemon.isLinux());
        FakeCredentialDaemon full = FakeCredentialDaemon.withFullBacklog();
        closeables.add(full);
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", full.socketPath().toString());
        CredentialDaemonCredentialsProvider provider =
            provider(CredentialDaemonCredentialsProvider.builder()
                                                         .credentialName("dev")
                                                         .connectTimeout(Duration.ofSeconds(5))
                                                         .callTimeout(Duration.ofSeconds(5)));

        long start = System.nanoTime();
        assertThatThrownBy(provider::resolveCredentials)
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains(full.socketPath().toString())
                                          .doesNotContain("connect timeout")
                                          .doesNotContain("call timeout");
            });
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500));
    }

    @Test
    void resolveCredentials_whenDaemonClosesBeforeReading_raisesProtocolError() {
        daemon.closeBeforeReading(true);

        assertThat(failure().errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
    }

    @Test
    void resolveCredentials_whenDaemonClosesAfterReading_raisesProtocolError() {
        daemon.responder(r -> FakeCredentialDaemon.CLOSE_AT_ONCE);

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
        assertThat(e.getMessage()).contains("connection closed before a complete response line, after 0 bytes");
    }

    @Test
    void resolveCredentials_whenReplyHasNoNewline_raisesProtocolError() {
        daemon.responder(r -> FakeCredentialDaemon.successReply("AKID", inMinutes(15)).getBytes(StandardCharsets.UTF_8));

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
        assertThat(e.getMessage()).contains("connection closed before a complete response line").doesNotContain("secret");
    }

    @Test
    void resolveCredentials_whenLineIs64000Bytes_accepts() {
        daemon.reply(paddedReply(64_000));

        assertThat(provider().resolveCredentials().accessKeyId()).isEqualTo("ASIADAEMON");
    }

    @Test
    void resolveCredentials_whenLineIs64001Bytes_raisesProtocolError() {
        daemon.reply(paddedReply(64_001));

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
        assertThat(e.getMessage()).contains("response line exceeds 64000 bytes");
    }

    private static String paddedReply(int length) {
        String success = FakeCredentialDaemon.successReply("ASIADAEMON", inMinutes(15));
        String prefix = "{\"provenance\":\"";
        String suffix = "\"," + success.substring(1);
        int padding = length - prefix.length() - suffix.length();
        StringBuilder reply = new StringBuilder(prefix);
        for (int i = 0; i < padding; i++) {
            reply.append('x');
        }
        String result = reply.append(suffix).toString();
        assertThat(result.getBytes(StandardCharsets.UTF_8)).hasSize(length);
        return result;
    }

    @Test
    void resolveCredentials_whenReauthRequired_raisesWithStrippedDaemonMessage() {
        daemon.reply(FakeCredentialDaemon.errorReply("REAUTH_REQUIRED", "bad\\u0007\\u001b[31m\\u009b"));

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.REAUTH_REQUIRED);
        assertThat(e.getMessage()).contains("credential 'dev' requires you to sign in again")
                                  .contains("Daemon message: bad[31m")
                                  .doesNotContain("\u0007")
                                  .doesNotContain("\u001b")
                                  .doesNotContain("\u009b");
    }

    @Test
    void resolveCredentials_whenCredentialNotFound_raisesCredentialNotFound() {
        daemon.reply(FakeCredentialDaemon.errorReply("CREDENTIAL_NOT_FOUND", "no such credential"));

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.CREDENTIAL_NOT_FOUND);
        assertThat(e.getMessage()).contains("this user has no credential named 'dev'; sign in again")
                                  .contains("Daemon message: no such credential");
    }

    @Test
    void resolveCredentials_whenOtherErrorCode_raisesRequestRejectedNamingCode() {
        daemon.reply(FakeCredentialDaemon.errorReply("THROTTLED\\u0007", "slow down"));

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.REQUEST_REJECTED);
        assertThat(e.getMessage()).contains("THROTTLED").contains("Daemon message: slow down").doesNotContain("\u0007");
    }

    @Test
    void resolveCredentials_whenErrorHasNoData_raisesRequestRejected() {
        daemon.reply(HEAD + "\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}");

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.REQUEST_REJECTED);
        assertThat(e.getMessage()).contains("Daemon message: Method not found");
    }

    @Test
    void resolveCredentials_whenMaterialTypeUnsupported_raisesUnsupportedMaterialBeforeOtherChecks() {
        daemon.reply(HEAD + "\"result\":{\"material\":{\"type\":\"x509\"}}}");

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.UNSUPPORTED_MATERIAL);
        assertThat(e.getMessage()).contains("x509").contains("Update the AWS SDK for Java");
    }

    @Test
    void resolveCredentials_withUnknownFields_ignoresThem() {
        daemon.reply("{\"jsonrpc\":\"2.0\",\"id\":42,\"provenance\":{\"source\":\"sso\"},\"result\":{\"credential\":\"other\","
                     + "\"extra\":[1,2],\"material\":{\"type\":\"session_credentials\",\"access_key_id\":\"AKID\","
                     + "\"secret_access_key\":\"skid\",\"unknown\":true},\"expires_at\":\""
                     + inMinutes(15).truncatedTo(ChronoUnit.SECONDS) + "\"}}");

        assertThat(provider().resolveCredentials().accessKeyId()).isEqualTo("AKID");
    }

    @Test
    void resolveCredentials_withNineFractionDigits_parsesExpiry() {
        daemon.reply(withExpiry("\"2099-01-02T03:04:05.123456789Z\""));

        AwsSessionCredentials credentials = (AwsSessionCredentials) provider().resolveCredentials();

        assertThat(credentials.expirationTime()).hasValue(Instant.parse("2099-01-02T03:04:05.123456789Z"));
    }

    @Test
    void resolveCredentials_withNoFractionDigits_parsesExpiry() {
        daemon.reply(withExpiry("\"2099-01-02T03:04:05Z\""));

        AwsSessionCredentials credentials = (AwsSessionCredentials) provider().resolveCredentials();

        assertThat(credentials.expirationTime()).hasValue(Instant.parse("2099-01-02T03:04:05Z"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protocolErrors")
    void resolveCredentials_withInvalidReply_raisesProtocolErrorWithoutReplyContent(String description, String reply,
                                                                                      String expectedMessage) {
        daemon.reply(reply);

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
        assertThat(e.getMessage()).contains(expectedMessage).doesNotContain("SUPERSECRET");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void resolveCredentials_withResultAndErrorWithMessage_raisesProtocolErrorWithoutErrorMessage() {
        daemon.reply(HEAD + "\"result\":{\"material\":{\"type\":\"session_credentials\",\"access_key_id\":\"AKID\","
                     + "\"secret_access_key\":\"SUPERSECRET\"},\"expires_at\":\"2099-01-01T00:00:00Z\"},"
                     + "\"error\":{\"code\":-32000,\"message\":\"denied\",\"data\":{\"code\":\"REAUTH_REQUIRED\"}}}");

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
        assertThat(e.getMessage()).contains("both result and error")
                                  .doesNotContain("denied")
                                  .doesNotContain("SUPERSECRET");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void resolveCredentials_withNullResultAndError_raisesDaemonError() {
        daemon.reply(HEAD + "\"result\":null,\"error\":{\"code\":-32000,\"message\":\"sign in\","
                     + "\"data\":{\"code\":\"REAUTH_REQUIRED\"}}}");

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.REAUTH_REQUIRED);
        assertThat(e.getMessage()).contains("Daemon message: sign in");
    }

    @Test
    void resolveCredentials_withResultAndNullError_returnsCredentials() {
        String success = FakeCredentialDaemon.successReply("ASIANULLERROR", inMinutes(15));
        daemon.reply(success.substring(0, success.length() - 1) + ",\"error\":null}");

        assertThat(provider().resolveCredentials().accessKeyId()).isEqualTo("ASIANULLERROR");
    }

    static Stream<Arguments> protocolErrors() {
        String okMaterial = "\"material\":{\"type\":\"session_credentials\",\"access_key_id\":\"AKID\","
                            + "\"secret_access_key\":\"SUPERSECRET\"}";
        String okResult = "{" + okMaterial + ",\"expires_at\":\"2099-01-01T00:00:00Z\"}";
        return Stream.of(
            Arguments.of("not JSON", "{\"secret\":\"SUPERSECRET\"", "response is not valid JSON"),
            Arguments.of("empty line", "", "response is not valid JSON"),
            Arguments.of("not an object", "[\"SUPERSECRET\"]", "not a JSON object"),
            Arguments.of("missing jsonrpc", "{\"id\":1,\"result\":" + okResult + "}", "missing jsonrpc"),
            Arguments.of("wrong jsonrpc", "{\"jsonrpc\":\"1.0\",\"result\":" + okResult + "}", "jsonrpc"),
            Arguments.of("numeric jsonrpc", "{\"jsonrpc\":2.0,\"result\":" + okResult + "}", "jsonrpc"),
            Arguments.of("both result and error", HEAD + "\"result\":" + okResult + ",\"error\":{}}", "both result and error"),
            Arguments.of("neither result nor error", "{\"jsonrpc\":\"2.0\",\"id\":1}", "neither result nor error"),
            Arguments.of("null result", HEAD + "\"result\":null}", "neither result nor error"),
            Arguments.of("null result and null error", HEAD + "\"result\":null,\"error\":null}",
                         "neither result nor error"),
            Arguments.of("error not an object", HEAD + "\"error\":\"SUPERSECRET\"}", "error is not a JSON object"),
            Arguments.of("material not an object", HEAD + "\"result\":{\"material\":\"SUPERSECRET\"}}", "material"),
            Arguments.of("empty material type", HEAD + "\"result\":{\"material\":{\"type\":\"\"}}}", "material.type"),
            Arguments.of("missing access key", HEAD + "\"result\":{\"material\":{\"type\":\"session_credentials\","
                                               + "\"secret_access_key\":\"SUPERSECRET\"},\"expires_at\":\"2099-01-01T00:00:00Z\"}}",
                         "missing material.access_key_id"),
            Arguments.of("empty access key", HEAD + "\"result\":{\"material\":{\"type\":\"session_credentials\","
                                             + "\"access_key_id\":\"\",\"secret_access_key\":\"SUPERSECRET\"},"
                                             + "\"expires_at\":\"2099-01-01T00:00:00Z\"}}",
                         "missing material.access_key_id"),
            Arguments.of("null secret", HEAD + "\"result\":{\"material\":{\"type\":\"session_credentials\","
                                        + "\"access_key_id\":\"AKID\",\"secret_access_key\":null},"
                                        + "\"expires_at\":\"2099-01-01T00:00:00Z\"}}",
                         "missing material.secret_access_key"),
            Arguments.of("numeric secret", HEAD + "\"result\":{\"material\":{\"type\":\"session_credentials\","
                                           + "\"access_key_id\":\"AKID\",\"secret_access_key\":12},"
                                           + "\"expires_at\":\"2099-01-01T00:00:00Z\"}}",
                         "material.secret_access_key is not a string"),
            Arguments.of("missing expiry", HEAD + "\"result\":{" + okMaterial + "}}", "missing result.expires_at"),
            Arguments.of("empty expiry", withExpiry("\"\""), "missing result.expires_at"),
            Arguments.of("null expiry", withExpiry("null"), "missing result.expires_at"),
            Arguments.of("numeric session token", HEAD + "\"result\":{\"material\":{\"type\":\"session_credentials\","
                                                  + "\"access_key_id\":\"AKID\",\"secret_access_key\":\"SUPERSECRET\","
                                                  + "\"session_token\":5},\"expires_at\":\"2099-01-01T00:00:00Z\"}}",
                         "material.session_token is not a string"),
            Arguments.of("unparseable expiry", withExpiry("\"tomorrow SUPERSECRET\""),
                         "expires_at is not an RFC 3339 UTC timestamp"),
            Arguments.of("offset expiry", withExpiry("\"2099-01-01T00:00:00+00:00\""),
                         "expires_at is not an RFC 3339 UTC timestamp"));
    }

    @Test
    void resolveCredentials_withInvalidUtf8_raisesProtocolError() {
        daemon.responder(r -> new byte[] {'{', '"', (byte) 0xff, '"', ':', '1', '}', '\n'});

        CredentialDaemonException e = failure();

        assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
        assertThat(e.getMessage()).contains("not valid UTF-8");
    }

    // expiry-past: a requiresCredentialRefresh case that passes on the existing refresh code, so it runs as a plain test.
    @Test
    void resolveCredentials_whenFirstFetchAlreadyExpired_returnsCredentials() {
        daemon.reply(FakeCredentialDaemon.successReply("EXPIRED", Instant.now().minus(Duration.ofMinutes(1))));

        assertThat(provider().resolveCredentials().accessKeyId()).isEqualTo("EXPIRED");
    }

    @Test
    void resolveCredentials_whenRefreshReturnsExpired_returnsItAsReceived() {
        Instant start = now();
        AdjustableClock clock = new AdjustableClock(start);
        daemon.reply(FakeCredentialDaemon.successReply("EXPIRED", start.minus(Duration.ofMinutes(1))));
        CredentialDaemonCredentialsProvider provider = provider(clock);
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("EXPIRED");

        daemon.reply(FakeCredentialDaemon.successReply("EXPIRED2", start.minus(Duration.ofMinutes(1))));
        clock.advance(Duration.ofSeconds(2));

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("EXPIRED2");
        assertThat(daemon.requests()).hasSize(2);
    }

    @Test
    void resolveCredentials_whenNonRecoverableDuringPrefetch_returnsCachedUntilStale() {
        Instant start = now();
        AdjustableClock clock = new AdjustableClock(start);
        daemon.reply(FakeCredentialDaemon.successReply("FIRST", start.plus(Duration.ofMinutes(15))));
        CredentialDaemonCredentialsProvider provider = provider(clock);
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("FIRST");

        daemon.reply(FakeCredentialDaemon.errorReply("REAUTH_REQUIRED", "sign in"));
        clock.advance(IN_PREFETCH_WINDOW);

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("FIRST");
        assertThat(daemon.requests()).hasSize(2);

        clock.advance(Duration.ofMinutes(1));

        assertThatThrownBy(provider::resolveCredentials)
            .isInstanceOfSatisfying(CredentialDaemonException.class,
                                    e -> assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.REAUTH_REQUIRED));
    }

    @Test
    void resolveCredentials_whenRecoverableDuringPrefetch_returnsCachedCredentials() throws Exception {
        daemon.reply(FakeCredentialDaemon.successReply("FIRST", inMinutes(2)));
        CredentialDaemonCredentialsProvider provider = provider();
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("FIRST");

        daemon.reply("not json");

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("FIRST");
        assertThat(daemon.requests()).hasSize(2);
    }

    // requiresCredentialRefresh cases of the credential daemon SEP. expiry-past
    // (resolveCredentials_whenFirstFetchAlreadyExpired_returnsCredentials) passes on CachedSupplier today and runs as a plain
    // test. The invalidated-* cases need the Credential Refresh SEP. Each one runs the SEP's assertions and expects them to
    // fail, so the test fails once the case passes.

    @Test
    void invalidatedRefreshes_expectedFailure_untilCredentialRefresh() {
        CredentialDaemonCredentialsProvider provider = cachedRejectedCredentials();
        // needs the Credential Refresh SEP
        expectedFailureUntilCredentialRefresh(() -> {
            daemon.reply(FakeCredentialDaemon.successReply("ASIADAEMON", inMinutes(15)));

            // Java has no Invalidate(), so the case calls resolveCredentials() again after the caller rejects the key.
            AwsCredentials credentials = provider.resolveCredentials();

            assertThat(daemon.requests()).hasSize(2);
            assertThat(credentials.accessKeyId()).isEqualTo("ASIADAEMON");
        });
    }

    @Test
    void invalidatedSameKey_expectedFailure_untilCredentialRefresh() {
        CredentialDaemonCredentialsProvider provider = cachedRejectedCredentials();
        // needs the Credential Refresh SEP
        expectedFailureUntilCredentialRefresh(() -> {
            // Java has no Invalidate(), so the case calls resolveCredentials() again after the caller rejects the key.
            assertThatThrownBy(provider::resolveCredentials)
                .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                    // A recoverable refresh failure with no daemon error code.
                    assertThat(e.errorCode()).isNotIn(CredentialDaemonException.REAUTH_REQUIRED,
                                                      CredentialDaemonException.CREDENTIAL_NOT_FOUND,
                                                      CredentialDaemonException.REQUEST_REJECTED,
                                                      CredentialDaemonException.UNSUPPORTED_MATERIAL);
                    assertThat(e.getMessage()).doesNotContain(CACHED_SECRET);
                });
            assertThat(daemon.requests()).hasSize(2);
        });
    }

    private CredentialDaemonCredentialsProvider cachedRejectedCredentials() {
        daemon.reply(FakeCredentialDaemon.successReply("ASIAREJECTED", CACHED_SECRET, inMinutes(60)));
        CredentialDaemonCredentialsProvider provider = provider();
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("ASIAREJECTED");
        return provider;
    }

    /**
     * Runs a case whose assertions fail until the Credential Refresh SEP is released in the SDK.
     */
    private static void expectedFailureUntilCredentialRefresh(ThrowingCallable sepCase) {
        assertThatThrownBy(sepCase).isInstanceOf(AssertionError.class);
    }

    @Test
    void resolveCredentials_whenSocketVariableChanges_dialsNewLocationOnNextFetch() throws Exception {
        daemon.reply(FakeCredentialDaemon.successReply("FIRST", inMinutes(2)));
        FakeCredentialDaemon second = startDaemon(FakeCredentialDaemon.successReply("SECOND", inMinutes(15)));
        CredentialDaemonCredentialsProvider provider = provider();
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("FIRST");

        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", second.socketPath().toString());

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("SECOND");
        assertThat(daemon.requests()).hasSize(1);
        assertThat(second.requests()).hasSize(1);
    }

    @Test
    void resolveCredentials_withAsyncUpdate_returnsCredentials() {
        CredentialDaemonCredentialsProvider provider =
            provider(CredentialDaemonCredentialsProvider.builder().credentialName("dev").asyncCredentialUpdateEnabled(true));

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("ASIADAEMON");
    }

    @Test
    void build_withInvalidName_raisesInvalidConfig() {
        assertThatThrownBy(() -> CredentialDaemonCredentialsProvider.builder().credentialName(".hidden").build())
            .isInstanceOfSatisfying(CredentialDaemonException.class,
                                    e -> assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.INVALID_CONFIG));
        assertThatThrownBy(() -> CredentialDaemonCredentialsProvider.builder().build())
            .isInstanceOf(CredentialDaemonException.class);
    }

    @Test
    void build_withOnlyAccountId_raisesIllegalArgument() {
        assertThatThrownBy(() -> CredentialDaemonCredentialsProvider.builder().credentialName("dev").accountId("1").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accountId and roleName");
        assertThatThrownBy(() -> CredentialDaemonCredentialsProvider.builder().credentialName("dev").roleName("r").build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_withNonPositiveTimeout_raisesIllegalArgument() {
        assertThatThrownBy(() -> CredentialDaemonCredentialsProvider.builder().credentialName("dev")
                                                                     .connectTimeout(Duration.ZERO).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toBuilder_keepsSettings() {
        CredentialDaemonCredentialsProvider provider =
            provider(CredentialDaemonCredentialsProvider.builder().credentialName("dev").accountId("1").roleName("r"));

        provider(provider.toBuilder()).resolveCredentials();

        assertThat(daemon.requests().get(0)).contains("\"account_id\":\"1\",\"role_name\":\"r\"");
        assertThat(provider.toString()).isEqualTo("CredentialDaemonCredentialsProvider(credential=dev)");
    }

    @Test
    void exceptionToBuilder_keepsErrorCode() {
        CredentialDaemonException e = CredentialDaemonException.builder()
                                                                 .errorCode(CredentialDaemonException.NOT_RUNNING)
                                                                 .message("m")
                                                                 .build();

        CredentialDaemonException copy = (CredentialDaemonException) e.toBuilder().build();

        assertThat(copy.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
        assertThat(copy.getMessage()).isEqualTo("m");
    }

    private static CredentialDaemonCredentialsProvider.Builder builder(Clock clock) {
        return ((CredentialDaemonCredentialsProvider.BuilderImpl) CredentialDaemonCredentialsProvider.builder()).clock(clock);
    }
}
