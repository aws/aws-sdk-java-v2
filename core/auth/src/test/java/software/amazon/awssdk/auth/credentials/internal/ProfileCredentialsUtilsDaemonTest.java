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

package software.amazon.awssdk.auth.credentials.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.CredentialDaemonCredentialsProvider;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.auth.credentials.FakeCredentialDaemon;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

class ProfileCredentialsUtilsDaemonTest {
    private final EnvironmentVariableHelper env = new EnvironmentVariableHelper();
    private FakeCredentialDaemon daemon;

    @BeforeEach
    void setup() throws Exception {
        assumeTrue(FakeCredentialDaemon.isSupported());
        daemon = FakeCredentialDaemon.replying(
            FakeCredentialDaemon.successReply("ASIADAEMON", Instant.now().plus(Duration.ofMinutes(15))));
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", daemon.socketPath().toString());
    }

    @AfterEach
    void teardown() {
        env.reset();
        if (daemon != null) {
            daemon.close();
        }
    }

    private static ProfileFile config(String content) {
        return ProfileFile.builder()
                          .content(new StringInputStream(content))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    private static Optional<AwsCredentialsProvider> provider(String content, String profileName) {
        ProfileFile file = config(content);
        return new ProfileCredentialsUtils(file, file.profile(profileName).get(), file::profile).credentialsProvider();
    }

    private static CredentialDaemonException invalidConfig(String content) {
        try {
            provider(content, "dev");
        } catch (CredentialDaemonException e) {
            assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.INVALID_CONFIG);
            return e;
        }
        throw new AssertionError("Expected INVALID_CONFIG");
    }

    @ParameterizedTest
    @ValueSource(strings = {".x", "a b", "ü", "a/b", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void credentialsProvider_withInvalidName_raisesInvalidConfig(String name) {
        CredentialDaemonException e = invalidConfig("[profile dev]\ndaemon_credential = " + name + "\n");

        assertThat(e.getMessage()).contains("daemon_credential").contains("'dev'");
    }

    @Test
    void credentialsProvider_withEmptyName_raisesInvalidConfig() {
        invalidConfig("[profile dev]\ndaemon_credential =\n");
    }

    @Test
    void credentialsProvider_with64CharacterMixedCaseName_requestsNameUnchanged() {
        String name = "Ab-_.0123456789012345678901234567890123456789012345678901234567Z";
        assertThat(name).hasSize(64);

        AwsCredentialsProvider provider = provider("[profile dev]\ndaemon_credential = " + name + "\n", "dev").get();
        provider.resolveCredentials();

        assertThat(provider).isInstanceOf(CredentialDaemonCredentialsProvider.class);
        assertThat(daemon.requests().get(0)).contains("\"credential\":\"" + name + "\"");
    }

    @Test
    void credentialsProvider_withKeysAndProcessAndDaemon_usesDaemon() {
        String content = "[profile dev]\n"
                         + "daemon_credential = dev\n"
                         + "aws_access_key_id = akid\n"
                         + "aws_secret_access_key = skid\n"
                         + "credential_process = /bin/false\n"
                         + "login_session = arn:aws:iam::0123456789012:user/Admin\n";

        AwsCredentials credentials = provider(content, "dev").get().resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ASIADAEMON");
        assertThat(credentials.providerName()).hasValue("Aq");
    }

    @Test
    void credentialsProvider_withKeysAndDaemonNotRunning_doesNotFallBack() throws Exception {
        Path directory = FakeCredentialDaemon.newSocketDirectory();
        env.set("AWS_CREDENTIAL_DAEMON_SOCKET", directory.resolve("missing.sock").toString());
        String content = "[profile dev]\n"
                         + "daemon_credential = dev\n"
                         + "aws_access_key_id = akid\n"
                         + "aws_secret_access_key = skid\n";
        try {
            AwsCredentialsProvider provider = provider(content, "dev").get();

            assertThatThrownBy(provider::resolveCredentials)
                .isInstanceOfSatisfying(CredentialDaemonException.class,
                                        e -> assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING));
        } finally {
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void credentialsProvider_withRoleArn_raisesInvalidConfigWithSourceProfileHint() {
        CredentialDaemonException e = invalidConfig("[profile dev]\ndaemon_credential = dev\n"
                                                     + "role_arn = arn:aws:iam::123456789012:role/r\n"
                                                     + "source_profile = other\n");

        assertThat(e.getMessage()).contains("Profile 'dev' sets both daemon_credential and role_arn")
                                  .contains("Move role_arn into its own profile that sets source_profile = dev.");
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void credentialsProvider_withWebIdentityTokenFile_raisesInvalidConfigNamingTwoSources() {
        CredentialDaemonException e = invalidConfig("[profile dev]\ndaemon_credential = dev\n"
                                                     + "web_identity_token_file = /tmp/token\n");

        assertWebIdentityConflictMessage(e);
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void credentialsProvider_withRoleArnAndWebIdentityTokenFile_raisesWebIdentityConflict() {
        CredentialDaemonException e = invalidConfig("[profile dev]\ndaemon_credential = dev\n"
                                                     + "role_arn = arn:aws:iam::123456789012:role/r\n"
                                                     + "web_identity_token_file = /tmp/token\n");

        assertWebIdentityConflictMessage(e);
        assertThat(daemon.requests()).isEmpty();
    }

    private static void assertWebIdentityConflictMessage(CredentialDaemonException e) {
        assertThat(e.getMessage()).contains("Profile 'dev' has two credential sources")
                                  .contains("daemon_credential")
                                  .contains("web_identity_token_file")
                                  .contains("Remove one")
                                  .doesNotContain("source_profile");
    }

    @Test
    void credentialsProvider_whenRoleSourcesDaemonProfile_needsSts() {
        String content = "[profile role]\n"
                         + "role_arn = arn:aws:iam::123456789012:role/r\n"
                         + "source_profile = dev\n"
                         + "[profile dev]\n"
                         + "daemon_credential = dev\n";

        assertThatThrownBy(() -> provider(content, "role"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("the 'sts' service module must be on the class path");
    }

    @Test
    void credentialsProvider_whenSourceProfileHasInvalidDaemonName_raisesInvalidConfig() {
        String content = "[profile role]\n"
                         + "role_arn = arn:aws:iam::123456789012:role/r\n"
                         + "source_profile = dev\n"
                         + "[profile dev]\n"
                         + "daemon_credential = .bad\n";

        assertThatThrownBy(() -> provider(content, "role"))
            .isInstanceOfSatisfying(CredentialDaemonException.class,
                                    e -> assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.INVALID_CONFIG));
    }

    @Test
    void credentialsProvider_withSsoProfileAndBothKeys_sendsAccountAndRole() {
        String content = "[profile dev]\n"
                         + "daemon_credential = dev\n"
                         + "sso_session = s\n"
                         + "sso_account_id = 123456789012\n"
                         + "sso_role_name = Admin\n"
                         + "[sso-session s]\n"
                         + "sso_region = us-east-1\n"
                         + "sso_start_url = https://example.awsapps.com/start\n";

        provider(content, "dev").get().resolveCredentials();

        assertThat(daemon.requests().get(0))
            .contains("\"params\":{\"credential\":\"dev\",\"account_id\":\"123456789012\",\"role_name\":\"Admin\"}");
    }

    @Test
    void credentialsProvider_withSsoProfileAndOneKey_raisesMissingPropertyWithoutCallingDaemon() {
        String content = "[profile dev]\n"
                         + "daemon_credential = dev\n"
                         + "sso_start_url = https://example.awsapps.com/start\n"
                         + "sso_account_id = 123456789012\n";

        assertThatThrownBy(() -> provider(content, "dev"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Profile property 'sso_role_name' was not configured for 'dev'.");
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void credentialsProvider_withSsoProfileAndNeitherKey_resolvesAsIfDaemonAbsent() {
        String content = "[profile dev]\n"
                         + "daemon_credential = .not-checked\n"
                         + "sso_session = s\n";

        assertThatThrownBy(() -> provider(content, "dev"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Profile property 'sso_account_id' was not configured for 'dev'.");
        assertThat(daemon.requests()).isEmpty();
    }

    @Test
    void credentialsProvider_withoutSsoSettings_doesNotSendAccountAndRole() {
        String content = "[profile dev]\n"
                         + "daemon_credential = dev\n";

        provider(content, "dev").get().resolveCredentials();

        assertThat(daemon.requests().get(0)).doesNotContain("account_id");
    }
}
