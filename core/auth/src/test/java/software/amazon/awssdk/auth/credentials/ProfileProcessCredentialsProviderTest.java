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

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.utils.Platform;
import software.amazon.awssdk.utils.StringInputStream;

@Timeout(30)
class ProfileProcessCredentialsProviderTest {

    @ParameterizedTest
    @EnumSource(ProfileFile.Type.class)
    void resolveCredentials_argvProfile_executesProcess(ProfileFile.Type fileType) {
        ProfileFile file = profileFile("credential_process_argv = " + commandJson("static", "value"), fileType);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            AwsCredentials credentials = provider.resolveCredentials();

            assertThat(credentials.accessKeyId()).isEqualTo("akid");
            assertThat(credentials.secretAccessKey()).isEqualTo("skid");
            assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
            assertThat(credentials.providerName()).hasValue(
                BusinessMetricFeatureId.CREDENTIALS_PROFILE_PROCESS.value() + ","
                + BusinessMetricFeatureId.CREDENTIALS_PROCESS.value());
        }
    }

    @Test
    void resolveCredentials_defaultChain_executesArgvProcess() {
        ProfileFile file = profileFile("credential_process_argv = " + commandJson("static", "value"),
                                       ProfileFile.Type.CONFIGURATION);

        try (DefaultCredentialsProvider provider = DefaultCredentialsProvider.builder()
                                                                            .profileFile(file)
                                                                            .profileName("default")
                                                                            .build()) {
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid");
        }
    }

    @Test
    void resolveCredentials_namedProfile_executesArgvProcess() {
        ProfileFile file = ProfileFile.builder()
                                      .content(new StringInputStream("[profile spiffe]\ncredential_process_argv = "
                                                                     + commandJson("static", "value")))
                                      .type(ProfileFile.Type.CONFIGURATION)
                                      .build();

        try (ProfileCredentialsProvider provider = ProfileCredentialsProvider.builder()
                                                                              .profileFile(file)
                                                                              .profileName("spiffe")
                                                                              .build()) {
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid");
        }
    }

    @Test
    void resolveCredentials_argvAndStaticCredentials_prefersProcess() {
        ProfileFile file = profileFile("aws_access_key_id = unused\naws_secret_access_key = unused\n"
                                       + "credential_process_argv = " + commandJson("static", "value"),
                                       ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid");
        }
    }

    @Test
    void resolveCredentials_argvSourceProfile_recognizesSourceCredentials() {
        ProfileFile file = profileFile("role_arn = arn:aws:iam::123456789012:role/example\n"
                                       + "source_profile = spiffe\n[profile spiffe]\ncredential_process_argv = [\"helper\"]",
                                       ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            assertThatThrownBy(provider::resolveCredentials)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the 'sts' service module must be on the class path");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"argument with spaces", "$HOME", "$(exit 1)", "a;b|c>d", "\"quoted\"\\path", "", "  "})
    void resolveCredentials_specialArguments_passesArgumentsLiterally(String argument) {
        ProfileFile file = profileFile("credential_process_argv = " + commandJson("session", argument),
                                       ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            AwsSessionCredentials credentials = (AwsSessionCredentials) provider.resolveCredentials();

            assertThat(credentials.sessionToken()).isEqualTo("literal:" + argument);
            assertThat(credentials.expirationTime()).isPresent();
        }
    }

    @Test
    void resolveCredentials_multilineArgv_executesProcess() {
        String command = commandJson("static", "value").replace(",", ",\n  ");
        ProfileFile file = profileFile("credential_process_argv = " + command, ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid");
        }
    }

    @ParameterizedTest
    @MethodSource("accountIds")
    void resolveCredentials_accountId_preservesAccountIdHandling(String processAccountId, String profileAccountId,
                                                                String expectedAccountId) {
        List<String> command = command("static", "value");
        if (processAccountId != null) {
            command.add(processAccountId);
        }
        String properties = "credential_process_argv = " + commandJson(command);
        if (profileAccountId != null) {
            properties += "\naws_account_id = " + profileAccountId;
        }

        try (ProfileCredentialsProvider provider = profileProvider(profileFile(properties, ProfileFile.Type.CONFIGURATION))) {
            if (expectedAccountId == null) {
                assertThat(provider.resolveCredentials().accountId()).isNotPresent();
            } else {
                assertThat(provider.resolveCredentials().accountId()).hasValue(expectedAccountId);
            }
        }
    }

    private static Stream<Arguments> accountIds() {
        return Stream.of(Arguments.of(null, null, null),
                         Arguments.of(null, "profile-account", "profile-account"),
                         Arguments.of("process-account", null, "process-account"),
                         Arguments.of("process-account", "profile-account", "process-account"));
    }

    @Test
    void resolveCredentials_unexpiredCredentials_reusesCachedCredentials() {
        ProfileFile file = profileFile("credential_process_argv = " + commandJson("session", "value"),
                                       ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            AwsCredentials first = provider.resolveCredentials();

            assertThat(provider.resolveCredentials()).isSameAs(first);
        }
    }

    @Test
    void resolveCredentials_expiredCredentials_rerunsProcess() {
        ProfileFile file = profileFile("credential_process_argv = " + commandJson("expired", "value"),
                                       ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            AwsSessionCredentials first = (AwsSessionCredentials) provider.resolveCredentials();
            AwsSessionCredentials second = (AwsSessionCredentials) provider.resolveCredentials();

            assertThat(second.sessionToken()).isNotEqualTo(first.sessionToken());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-json", "null", "{}", "\"helper\"", "[]", "[null]", "[1]", "[true]", "[[]]",
                           "[{}]", "[\"\"]", "[\"  \"]", "[\"helper\",null]", "[\"helper\",1]",
                           "[\"helper\\u0000\"]", "[\"helper\",\"arg\\u0000\"]", "[", "[\"helper\"",
                           "[\"helper\",]", "[\"helper\",sensitive-command-content]", "[\"helper\"] []",
                           "[\"helper\"] sensitive-command-content", "[\"helper\"]]"})
    void resolveCredentials_invalidArgv_rejectsConfigurationWithoutExposingCommand(String value) {
        ProfileFile file = profileFile("credential_process_argv = " + value, ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            assertThatThrownBy(provider::resolveCredentials)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credential_process_argv")
                .hasMessageNotContaining("sensitive-command-content")
                .hasNoCause();
        }
    }

    @Test
    void resolveCredentials_bothProcessProperties_rejectsAmbiguousConfiguration() {
        ProfileFile file = profileFile("credential_process = must-not-execute\n"
                                       + "credential_process_argv = " + commandJson("static", "value"),
                                       ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            assertThatThrownBy(provider::resolveCredentials)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credential_process")
                .hasMessageContaining("credential_process_argv")
                .hasMessageContaining("both");
        }
    }

    @Test
    void resolveCredentials_processFails_reportsFailure() {
        ProfileFile file = profileFile("credential_process_argv = " + commandJson("error", "value"),
                                       ProfileFile.Type.CONFIGURATION);

        try (ProfileCredentialsProvider provider = profileProvider(file)) {
            assertThatThrownBy(provider::resolveCredentials)
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Command returned non-zero exit value (7) with error message: deliberate helper failure");
        }
    }

    private static ProfileCredentialsProvider profileProvider(ProfileFile file) {
        return ProfileCredentialsProvider.builder().profileFile(file).profileName("default").build();
    }

    private static ProfileFile profileFile(String properties, ProfileFile.Type fileType) {
        return ProfileFile.builder()
                          .content(new StringInputStream("[default]\n" + properties))
                          .type(fileType)
                          .build();
    }

    private static String commandJson(String mode, String value) {
        return commandJson(command(mode, value));
    }

    private static String commandJson(List<String> command) {
        try (JsonWriter writer = JsonWriter.create()) {
            writer.writeStartArray();
            command.forEach(writer::writeValue);
            writer.writeEndArray();
            return new String(writer.getBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> command(String mode, String value) {
        String java = Paths.get(System.getProperty("java.home"), "bin", Platform.isWindows() ? "java.exe" : "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return new ArrayList<>(Arrays.asList(java, "-cp", classpath, CredentialsProcess.class.getName(), mode, value));
    }

    static final class CredentialsProcess {
        public static void main(String[] args) {
            if ("error".equals(args[0])) {
                System.err.print("deliberate helper failure");
                System.exit(7);
            }

            try (JsonWriter writer = JsonWriter.create()) {
                writer.writeStartObject()
                      .writeFieldName("Version").writeValue(1)
                      .writeFieldName("AccessKeyId").writeValue("akid")
                      .writeFieldName("SecretAccessKey").writeValue("skid");
                if (!"static".equals(args[0])) {
                    boolean expired = "expired".equals(args[0]);
                    String token = expired ? UUID.randomUUID().toString() : "literal:" + args[1];
                    writer.writeFieldName("SessionToken").writeValue(token)
                          .writeFieldName("Expiration").writeValue(Instant.now().plusSeconds(expired ? -60 : 3600).toString());
                }
                if (args.length > 2) {
                    writer.writeFieldName("AccountId").writeValue(args[2]);
                }
                writer.writeEndObject();
                System.out.print(new String(writer.getBytes(), StandardCharsets.UTF_8));
            }
        }
    }
}
