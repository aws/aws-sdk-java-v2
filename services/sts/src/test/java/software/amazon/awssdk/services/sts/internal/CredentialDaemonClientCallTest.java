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

package software.amazon.awssdk.services.sts.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Calls STS with credentials from a fake credential daemon, and checks what reaches the HTTP layer.
 */
class CredentialDaemonClientCallTest {
    private static final String CALLER_IDENTITY_RESPONSE =
        "<GetCallerIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\"><GetCallerIdentityResult>"
        + "<Arn>arn:aws:iam::123456789012:user/u</Arn><UserId>AIDA</UserId><Account>123456789012</Account>"
        + "</GetCallerIdentityResult></GetCallerIdentityResponse>";
    private static final String ASSUME_ROLE_RESPONSE =
        "<AssumeRoleResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\"><AssumeRoleResult><Credentials>"
        + "<AccessKeyId>ASIAROLE</AccessKeyId><SecretAccessKey>rolesecret</SecretAccessKey><SessionToken>roletoken"
        + "</SessionToken><Expiration>2099-01-01T00:00:00Z</Expiration></Credentials><AssumedRoleUser>"
        + "<Arn>arn:aws:sts::123456789012:assumed-role/r/s</Arn><AssumedRoleId>AROA:s</AssumedRoleId></AssumedRoleUser>"
        + "</AssumeRoleResult></AssumeRoleResponse>";
    private static final String CONFIG = "[profile dev]\n"
                                         + "daemon_credential = dev\n"
                                         + "[profile role]\n"
                                         + "region = us-west-2\n"
                                         + "role_arn = arn:aws:iam::123456789012:role/r\n"
                                         + "role_session_name = s\n"
                                         + "source_profile = dev\n";

    private final EnvironmentVariableHelper env = new EnvironmentVariableHelper();
    private WireMockServer wireMock;
    private Path directory;
    private ServerSocketChannel daemon;
    private Path configFile;

    @BeforeEach
    void setup() throws Exception {
        assumeTrue(unixSocketsSupported());
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        directory = Files.createTempDirectory(Paths.get("/tmp"), "cd");
        configFile = directory.resolve("config");
        Files.write(configFile, CONFIG.getBytes(StandardCharsets.UTF_8));

        env.remove("AWS_ACCESS_KEY_ID");
        env.remove("AWS_SECRET_ACCESS_KEY");
        env.remove("AWS_SESSION_TOKEN");
        env.remove("AWS_WEB_IDENTITY_TOKEN_FILE");
        env.remove("AWS_ROLE_ARN");
        env.set("AWS_EC2_METADATA_DISABLED", "true");
        env.set("AWS_CONFIG_FILE", configFile.toString());
        env.set("AWS_SHARED_CREDENTIALS_FILE", directory.resolve("credentials-absent").toString());
        env.set("AWS_ENDPOINT_URL_STS", "http://localhost:" + wireMock.port());
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", startDaemon().toString());
    }

    @AfterEach
    void teardown() throws Exception {
        env.reset();
        if (wireMock != null) {
            wireMock.stop();
        }
        if (daemon != null) {
            daemon.close();
        }
        if (directory != null) {
            for (Path p : Files.list(directory).toArray(Path[]::new)) {
                Files.deleteIfExists(p);
            }
            Files.deleteIfExists(directory);
        }
    }

    private ProfileFile profileFile() {
        return ProfileFile.builder().content(configFile).type(ProfileFile.Type.CONFIGURATION).build();
    }

    private StsClient stsClient(ProfileCredentialsProvider credentialsProvider) {
        return StsClient.builder()
                        .region(Region.US_WEST_2)
                        .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                        .credentialsProvider(credentialsProvider)
                        .build();
    }

    private void useMissingSocket() {
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", directory.resolve("missing.sock").toString());
    }

    @Test
    void getCallerIdentity_withDaemonProfile_signsWithDaemonCredentialsAndSendsMetric() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(CALLER_IDENTITY_RESPONSE)));
        ProfileCredentialsProvider credentials =
            ProfileCredentialsProvider.builder().profileFile(profileFile()).profileName("dev").build();

        try (StsClient sts = stsClient(credentials)) {
            sts.getCallerIdentity();
        }

        List<LoggedRequest> requests = wireMock.findAll(postRequestedFor(urlEqualTo("/")));
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getHeader("Authorization")).contains("Credential=ASIADAEMON/");
        assertThat(metrics(requests.get(0))).contains("Aq");
    }

    @Test
    void getCallerIdentity_whenDaemonNotRunning_failsWithoutSendingRequest() {
        useMissingSocket();
        ProfileCredentialsProvider credentials =
            ProfileCredentialsProvider.builder().profileFile(profileFile()).profileName("dev").build();

        try (StsClient sts = stsClient(credentials)) {
            assertThatThrownBy(sts::getCallerIdentity)
                .satisfies(e -> assertThat(daemonFailure(e).errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING));
        }

        assertThat(wireMock.findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void resolveCredentials_withRoleSourcingDaemonProfile_assumesRoleWithDaemonCredentials() {
        wireMock.stubFor(post(anyUrl()).withRequestBody(containing("Action=AssumeRole"))
                                       .willReturn(aResponse().withStatus(200).withBody(ASSUME_ROLE_RESPONSE)));
        wireMock.stubFor(post(anyUrl()).withRequestBody(containing("Action=GetCallerIdentity"))
                                       .willReturn(aResponse().withStatus(200).withBody(CALLER_IDENTITY_RESPONSE)));

        AwsCredentials credentials;
        try (ProfileCredentialsProvider provider =
                 ProfileCredentialsProvider.builder().profileFile(profileFile()).profileName("role").build();
             StsClient sts = stsClient(provider)) {
            credentials = provider.resolveCredentials();
            sts.getCallerIdentity();
        }

        assertThat(credentials.accessKeyId()).isEqualTo("ASIAROLE");
        assertThat(credentials.providerName())
            .hasValueSatisfying(name -> assertThat(name.split(",")).contains("o", "Aq"));

        LoggedRequest assumeRole = wireMock.findAll(postRequestedFor(urlEqualTo("/"))
                                                        .withRequestBody(containing("Action=AssumeRole"))).get(0);
        assertThat(assumeRole.getHeader("Authorization")).contains("Credential=ASIADAEMON/");
        assertThat(metrics(assumeRole)).contains("Aq");

        LoggedRequest callerIdentity = wireMock.findAll(postRequestedFor(urlEqualTo("/"))
                                                            .withRequestBody(containing("Action=GetCallerIdentity"))).get(0);
        assertThat(callerIdentity.getHeader("Authorization")).contains("Credential=ASIAROLE/");
        assertThat(metrics(callerIdentity)).contains("o", "Aq");
    }

    @Test
    void resolveCredentials_roleChainEndingInAnonymousWhenDaemonNotRunning_movesOnToAnonymous() {
        useMissingSocket();
        AwsCredentialsProviderChain chain =
            AwsCredentialsProviderChain.of(ProfileCredentialsProvider.builder()
                                                                     .profileFile(profileFile())
                                                                     .profileName("role")
                                                                     .build(),
                                           AnonymousCredentialsProvider.create());

        AwsCredentials credentials = chain.resolveCredentials();

        assertThat(credentials.accessKeyId()).isNull();
        assertThat(wireMock.findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void resolveCredentials_defaultProviderWithRoleProfileWhenDaemonNotRunning_reportsDaemonFailureInChainAggregate() {
        useMissingSocket();
        DefaultCredentialsProvider provider = DefaultCredentialsProvider.builder()
                                                                        .profileFile(profileFile())
                                                                        .profileName("role")
                                                                        .build();

        assertThatThrownBy(provider::resolveCredentials)
            .isInstanceOf(SdkClientException.class)
            .isNotInstanceOf(CredentialDaemonException.class)
            .hasMessageContaining("any of the providers in the chain")
            .hasMessageContaining(directory.resolve("missing.sock").toString());
        assertThat(wireMock.findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void resolveCredentials_defaultProviderWithWebIdentityEnvironmentAndDaemonProfile_returnsDaemonCredentials() {
        env.set("AWS_ROLE_ARN", "arn:aws:iam::123456789012:role/r");
        env.set("AWS_WEB_IDENTITY_TOKEN_FILE", "/nonexistent/aws-web-identity-token");
        env.set("AWS_REGION", "us-west-2");
        env.set("AWS_PROFILE", "dev");

        AwsCredentials credentials;
        try (DefaultCredentialsProvider provider = DefaultCredentialsProvider.builder().build()) {
            credentials = provider.resolveCredentials();
        }

        assertThat(credentials.accessKeyId()).isEqualTo("ASIADAEMON");
        assertThat(wireMock.findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    private static List<String> metrics(LoggedRequest request) {
        String userAgent = request.getHeader("User-Agent");
        int start = userAgent.indexOf(" m/");
        assertThat(start).isNotNegative();
        int end = userAgent.indexOf(' ', start + 1);
        String metrics = end < 0 ? userAgent.substring(start + 3) : userAgent.substring(start + 3, end);
        return Arrays.asList(metrics.split(","));
    }

    private static CredentialDaemonException daemonFailure(Throwable e) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof CredentialDaemonException) {
                return (CredentialDaemonException) t;
            }
        }
        throw new AssertionError("No CredentialDaemonException in the cause chain of " + e, e);
    }

    private static boolean unixSocketsSupported() {
        if (System.getProperty("os.name", "").toLowerCase().startsWith("windows")) {
            return false;
        }
        try {
            Class.forName("java.net.UnixDomainSocketAddress");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private Path startDaemon() throws Exception {
        Path socket = directory.resolve("d.sock");
        daemon = (ServerSocketChannel) ServerSocketChannel.class.getMethod("open", ProtocolFamily.class)
                                                                .invoke(null, StandardProtocolFamily.valueOf("UNIX"));
        daemon.bind(address(socket));
        Thread thread = new Thread(() -> {
            while (daemon.isOpen()) {
                try (SocketChannel channel = daemon.accept()) {
                    readLine(channel);
                    String reply = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"material\":{\"type\":\"session_credentials\","
                                   + "\"access_key_id\":\"ASIADAEMON\",\"secret_access_key\":\"secret\","
                                   + "\"session_token\":\"token\"},\"expires_at\":\""
                                   + Instant.now().plus(15, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS) + "\"}}\n";
                    ByteBuffer out = ByteBuffer.wrap(reply.getBytes(StandardCharsets.UTF_8));
                    while (out.hasRemaining()) {
                        channel.write(out);
                    }
                } catch (IOException e) {
                    return;
                }
            }
        }, "fake-credential-daemon");
        thread.setDaemon(true);
        thread.start();
        return socket;
    }

    private static void readLine(SocketChannel channel) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        while (channel.read(buffer) > 0) {
            if (buffer.get(0) == '\n') {
                return;
            }
            out.write(buffer.get(0));
            buffer.clear();
        }
    }

    private static SocketAddress address(Path path) throws ReflectiveOperationException {
        try {
            return (SocketAddress) Class.forName("java.net.UnixDomainSocketAddress").getMethod("of", Path.class)
                                        .invoke(null, path);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
