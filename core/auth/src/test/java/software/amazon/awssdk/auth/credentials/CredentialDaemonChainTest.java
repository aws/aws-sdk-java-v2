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
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

class CredentialDaemonChainTest {
    private final EnvironmentVariableHelper env = new EnvironmentVariableHelper();
    private FakeCredentialDaemon daemon;
    private Path configDirectory;

    @BeforeEach
    void setup() throws Exception {
        assumeTrue(FakeCredentialDaemon.isSupported());
        daemon = FakeCredentialDaemon.replying(
            FakeCredentialDaemon.successReply("ASIADAEMON", Instant.now().plus(Duration.ofMinutes(15))));
        configDirectory = FakeCredentialDaemon.newSocketDirectory();
        env.remove("AWS_ACCESS_KEY_ID");
        env.remove("AWS_SECRET_ACCESS_KEY");
        env.remove("AWS_SESSION_TOKEN");
        env.remove("AWS_WEB_IDENTITY_TOKEN_FILE");
        env.remove("AWS_ROLE_ARN");
        env.remove("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI");
        env.remove("AWS_CONTAINER_CREDENTIALS_FULL_URI");
        env.set("AWS_EC2_METADATA_DISABLED", "true");
        env.set("AWS_SHARED_CREDENTIALS_FILE", configDirectory.resolve("credentials-absent").toString());
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", daemon.socketPath().toString());
        env.set("AWS_PROFILE", "dev");
    }

    @AfterEach
    void teardown() throws Exception {
        env.reset();
        if (daemon != null) {
            daemon.close();
        }
        if (configDirectory != null) {
            try (java.util.stream.Stream<Path> files = Files.list(configDirectory)) {
                files.forEach(f -> f.toFile().delete());
            }
            Files.deleteIfExists(configDirectory);
        }
    }

    private void config(String content) throws Exception {
        Path config = configDirectory.resolve("config");
        Files.write(config, content.getBytes(StandardCharsets.UTF_8));
        env.set("AWS_CONFIG_FILE", config.toString());
    }

    @Test
    void resolveCredentials_whenConfigProfileSetsDaemonCredential_returnsDaemonCredentials() throws Exception {
        config("[profile dev]\ndaemon_credential = dev\n");

        AwsCredentials credentials = DefaultCredentialsProvider.builder().build().resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ASIADAEMON");
        assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
        assertThat(((AwsSessionCredentials) credentials).sessionToken()).isEqualTo("token");
        assertThat(daemon.requests())
            .containsExactly("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"credential/get\",\"params\":{\"credential\":\"dev\"}}");
    }

    @Test
    void resolveCredentials_whenEnvironmentHasCredentials_doesNotCallDaemon() throws Exception {
        config("[profile dev]\ndaemon_credential = dev\n");
        env.set("AWS_ACCESS_KEY_ID", "akid");
        env.set("AWS_SECRET_ACCESS_KEY", "skid");

        AwsCredentials credentials = DefaultCredentialsProvider.builder().build().resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("akid");
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void resolveCredentials_whenWebIdentityEnvironmentAndDaemonProfile_returnsDaemonCredentials() throws Exception {
        config("[profile dev]\ndaemon_credential = dev\n");
        env.set("AWS_ROLE_ARN", "arn:aws:iam::123456789012:role/r");
        env.set("AWS_WEB_IDENTITY_TOKEN_FILE", "/nonexistent/aws-web-identity-token");

        AwsCredentials credentials = DefaultCredentialsProvider.builder().build().resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ASIADAEMON");
        assertThat(daemon.requests()).hasSize(1);
    }

    @Test
    void resolveCredentials_whenDaemonNotRunning_movesOnAndReportsDaemonFailureInChainAggregate() throws Exception {
        config("[profile dev]\ndaemon_credential = dev\n");
        Path missing = configDirectory.resolve("missing.sock");
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", missing.toString());

        assertThatThrownBy(() -> DefaultCredentialsProvider.builder().build().resolveCredentials())
            .isInstanceOf(SdkClientException.class)
            .isNotInstanceOf(CredentialDaemonException.class)
            .hasMessageContaining("any of the providers in the chain")
            .hasMessageContaining(missing.toString());
    }

    @Test
    void resolveCredentials_whenProfileNameInvalid_raisesInvalidConfig() throws Exception {
        config("[profile dev]\ndaemon_credential = .dev\naws_access_key_id = akid\naws_secret_access_key = skid\n");

        assertThatThrownBy(() -> DefaultCredentialsProvider.builder().build().resolveCredentials())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("any of the providers in the chain")
            .hasMessageContaining("daemon_credential");
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void resolveCredentials_whenDaemonCredentialOnlyInCredentialsFile_ignoresIt() throws Exception {
        config("[profile other]\nregion = us-west-2\n");
        Path credentialsFile = configDirectory.resolve("credentials");
        Files.write(credentialsFile, ("[dev]\ndaemon_credential = dev\naws_access_key_id = akid\n"
                                      + "aws_secret_access_key = skid\n").getBytes(StandardCharsets.UTF_8));
        env.set("AWS_SHARED_CREDENTIALS_FILE", credentialsFile.toString());

        AwsCredentials credentials = DefaultCredentialsProvider.builder().build().resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("akid");
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void resolveCredentials_chainEndingInAnonymousWhenDaemonNotRunning_movesOnToAnonymous() {
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", configDirectory.resolve("missing.sock").toString());
        AwsCredentialsProviderChain chain =
            AwsCredentialsProviderChain.of(CredentialDaemonCredentialsProvider.builder().credentialName("dev").build(),
                                           AnonymousCredentialsProvider.create());

        AwsCredentials credentials = chain.resolveCredentials();

        assertThat(credentials.accessKeyId()).isNull();
    }
}
