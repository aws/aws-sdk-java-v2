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
import static org.assertj.core.api.Assertions.within;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Platform;

public class ProcessCredentialsProviderTest {

    private static final String PROCESS_RESOURCE_PATH = "/resources/process/";
    private static final String RANDOM_SESSION_TOKEN = "RANDOM_TOKEN";

    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String SECRET_ACCESS_KEY = "secretAccessKey";
    private static final String SESSION_TOKEN = "sessionToken";
    private static final String ACCOUNT_ID = "01234567891111";

    private static String scriptLocation;
    private static String errorScriptLocation;
 
    @BeforeAll
    static void setup()  {
        scriptLocation = copyHappyCaseProcessCredentialsScript();
        errorScriptLocation = copyErrorCaseProcessCredentialsScript();
    }
 
    @AfterAll
    static void teardown() {
        if (scriptLocation != null && !new File(scriptLocation).delete()) {
            throw new IllegalStateException("Failed to delete file: " + scriptLocation);
        }

        if (errorScriptLocation != null && !new File(errorScriptLocation).delete()) {
            throw new IllegalStateException("Failed to delete file: " + errorScriptLocation);
        }
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("staticCredentialsValues")
    void staticCredentialsCanBeLoaded(String description, String staticAccountId, Optional<String> expectedValue,
                                      String cmd) {
        ProcessCredentialsProvider.Builder providerBuilder = ProcessCredentialsProvider.builder().command(cmd);
        if (staticAccountId != null) {
            providerBuilder.staticAccountId(staticAccountId);
        }
        AwsCredentials credentials = providerBuilder.build().resolveCredentials();

        verifyCredentials(credentials);
        assertThat(credentials).isNotInstanceOf(AwsSessionCredentials.class);

        if (expectedValue.isPresent()) {
            assertThat(credentials.accountId()).isPresent().hasValue(expectedValue.get());
        } else {
            assertThat(credentials.accountId()).isNotPresent();
        }
    }

    private static List<Arguments> staticCredentialsValues() {
        return Arrays.asList(
            Arguments.of("when only containing access key id, secret", null, Optional.empty(),
                         String.format("%s accessKeyId secretAccessKey", scriptLocation)),
            Arguments.of("when output has account id", null, Optional.of(ACCOUNT_ID),
                         String.format("%s %s %s acctid=%s", scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, ACCOUNT_ID)),
            Arguments.of("when output has account id, static account id configured", "staticAccountId", Optional.of(ACCOUNT_ID),
                         String.format("%s %s %s acctid=%s", scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, ACCOUNT_ID)),
            Arguments.of("when only static account id is configured", "staticAccountId", Optional.of("staticAccountId"),
                         String.format("%s %s %s", scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY))
        );
    }
 
    @Test
    void staticCredentialsCanBeLoaded() {
        AwsCredentials credentials =
                ProcessCredentialsProvider.builder()
                                          .command(String.format("%s accessKeyId secretAccessKey", scriptLocation))
                                          .build()
                                          .resolveCredentials();
 
        assertThat(credentials).isNotInstanceOf(AwsSessionCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        assertThat(credentials.secretAccessKey()).isEqualTo(SECRET_ACCESS_KEY);
        assertThat(credentials.accountId()).isNotPresent();
    }

    @Test
    void staticCredentialsWithAccountIdCanBeLoaded() {
        AwsCredentials credentials =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s acctid=%s",
                                                             scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, ACCOUNT_ID))
                                      .build()
                                      .resolveCredentials();

        assertThat(credentials).isNotInstanceOf(AwsSessionCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        assertThat(credentials.secretAccessKey()).isEqualTo(SECRET_ACCESS_KEY);
        assertThat(credentials.accountId()).isPresent().isEqualTo(Optional.of(ACCOUNT_ID));
    }

    @Test
    void staticCredentials_commandAsListOfStrings_CanBeLoaded() {
        AwsCredentials credentials =
            ProcessCredentialsProvider.builder()
                                      .command(Arrays.asList(scriptLocation, "accessKeyId", "secretAccessKey"))
                                      .build()
                                      .resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("accessKeyId");
        assertThat(credentials.secretAccessKey()).isEqualTo("secretAccessKey");
        assertThat(credentials.providerName()).isPresent().hasValue(BusinessMetricFeatureId.CREDENTIALS_PROCESS.value());
    }
 
    @Test
    void sessionCredentialsCanBeLoaded() {
        String expiration = DateUtils.formatIso8601Date(Instant.now());
        ProcessCredentialsProvider credentialsProvider =
                ProcessCredentialsProvider.builder()
                                          .command(String.format("%s %s %s token=%s exp=%s",
                                                                 scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY,
                                                                 SESSION_TOKEN, expiration))
                                          .credentialRefreshThreshold(Duration.ofSeconds(1))
                                          .build();

        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        verifySessionCredentials(credentials, expiration);
        assertThat(credentials.accountId()).isNotPresent();
    }

    @Test
    void sessionCredentialsWithAccountIdCanBeLoaded() {
        String expiration = DateUtils.formatIso8601Date(Instant.now());
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s token=sessionToken exp=%s acctid=%s",
                                                             scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, expiration, ACCOUNT_ID))
                                      .credentialRefreshThreshold(Duration.ofSeconds(1))
                                      .build();

        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        verifySessionCredentials(credentials, expiration);
        assertThat(credentials.accountId()).isPresent().isEqualTo(Optional.of(ACCOUNT_ID));
    }

    @Test
    void sessionCredentialsWithStaticAccountIdCanBeLoaded() {
        String expiration = DateUtils.formatIso8601Date(Instant.now());
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s token=sessionToken exp=%s",
                                                             scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, expiration))
                                      .credentialRefreshThreshold(Duration.ofSeconds(1))
                                      .staticAccountId("staticAccountId")
                                      .source("v")
                                      .build();

        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        verifySessionCredentials(credentials, expiration);
        assertThat(credentials.accountId()).isPresent().hasValue("staticAccountId");
        assertThat(credentials.providerName()).isPresent().hasValue("v,w");
    }

    private void verifySessionCredentials(AwsCredentials credentials, String expiration) {
        verifyCredentials(credentials);

        assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
        AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;
        assertThat(sessionCredentials.sessionToken()).isEqualTo(SESSION_TOKEN);

        assertThat(sessionCredentials.expirationTime()).isPresent();
        Instant exp = sessionCredentials.expirationTime().get();
        assertThat(exp).isCloseTo(expiration, within(1, ChronoUnit.MICROS));
    }

    private void verifyCredentials(AwsCredentials credentials) {
        assertThat(credentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        assertThat(credentials.secretAccessKey()).isEqualTo(SECRET_ACCESS_KEY);
    }

    @Test
    void resultsAreCached() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s token=%s exp=%s",
                                                             scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, SESSION_TOKEN,
                                                             DateUtils.formatIso8601Date(Instant.now().plusSeconds(20))))
                                      .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void expirationBufferOverrideIsApplied() {
        ProcessCredentialsProvider credentialsProvider =
                ProcessCredentialsProvider.builder()
                                          .command(String.format("%s %s %s token=%s exp=%s",
                                                         scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, RANDOM_SESSION_TOKEN,
                                                         DateUtils.formatIso8601Date(Instant.now().plusSeconds(20))))
                                          .credentialRefreshThreshold(Duration.ofSeconds(20))
                                          .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void processFailed_shouldContainErrorMessage() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(errorScriptLocation)
                                      .credentialRefreshThreshold(Duration.ofSeconds(20))
                                      .build();

        assertThatThrownBy(credentialsProvider::resolveCredentials)
            .satisfies(throwable -> assertThat(throwable.getCause())
                .hasMessageContaining("(125) with error message: Some error case"));
    }

    @Test
    void lackOfExpirationIsCachedForever() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s token=%s",
                                                             scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, SESSION_TOKEN))
                                      .credentialRefreshThreshold(Duration.ofSeconds(20))
                                      .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        assertThat(request1).isEqualTo(request2);
    }
 
    @Test
    public void processOutputLimitIsEnforced() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s",
                                                             scriptLocation,
                                                             ACCESS_KEY_ID,
                                                             SECRET_ACCESS_KEY))
                                      .processOutputLimit(1)
                                      .build();
        assertThatThrownBy(() -> credentialsProvider.resolveCredentials()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void processOutputLimitDefaultPassesLargeInput() {
        String longSessionToken = "lYzvmByqdS1E69QQVEavDDHabQ2GuYKYABKRA4xLbAXpdnFtV030UH4" +
                "bQoZWCDcfADFvBwBm3ixEFTYMjn5XQozpFV2QAsWHirCVcEJ5DC60KPCNBcDi4KLNJfbsp3r6kKTOmYOeqhEyiC4emDX33X2ppZsa5" +
                "1iwr6ShIZPOUPmuR4WDglmWubgO2q5tZv48xA5idkcHEmtGdoL343sY24q4gMh21eeBnF6ikjZdfvZ0Mn86UQ8r05AD346rSwM5bFs" +
                "t019ZkJIjLHD3HoKJ44EndRvSvQClXfJCmmQDH5INiXdFLLNm0dzT3ynbVIW5x1YYBWptyts4NUSy2eJ3dTPjYICpQVCkbuNVA7PqR" +
                "ctUyE8lU7uvnrIVnx9xTgl34J6D9VJKHQkPuGvbtN6w4CVtXoPAQcE8tlkKyOQmIeqEahhaqLW15t692SI6hwBW0b8DxCQawX5ukt4" +
                "f5gZoRFz3u8qHMSnm5oEnTgv7C5AAs0V680YvelFMNYvSoSbDnoThxfTIG9msj7WBh7iNa7mI8TXmvOegQtDWR011ZOo8dR3jnhWNH" +
                "nSW4CRB7iSC5DMZ2y56dYS28XGBl01LYXF5ZTJJfLwQEhbRWSTdXIBJq07E0YxRu0SaLokA4uknOoicwXnD7LMCld4hFjuypYgWBuk" +
                "3pC09CPA0MJjQNTTAvxGqDTqSWoXWDZRIMUWyGyz3FCkpPUjv4mIpVYt2bGl6cHsMBzVnpL6yXMCw2mNqJx8Rvi4gQaHH6LzvHbVKR" +
                "w4kE53703DNOc8cA9Zc0efJa4NHOFxc4XmMOtjGW7vbWPp0CTVCJLG94ddSFJrimpamPM59bs12x2ih51EpOFR5ITIxJnd79HEkYDU" +
                "xRIOuPIe4VpM01RnFN4g3ChDqmjQ03wQY9I8Mvh59u3MujggQfwAhCc84MAz0jVukoMfhAAhMNUPLuwRj0qpqr6B3DdKZ4KDFWF2Ga" +
                "Iu1sEFlKvPdfF1uefbTj6YdjUciWu1UBH47VbIcTbvbwmUiu2javB21kOenyDoelK5GUM4u0uPeXIOOhtZsJb8kz88h1joWkaKr2fc" +
                "jrIS08FM47Y4Z2Mi4zfwyN54L";

        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s token=%s",
                                                             scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, longSessionToken))
                .build();

        AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentialsProvider.resolveCredentials();

        assertThat(sessionCredentials.accessKeyId()).isEqualTo("accessKeyId");
        assertThat(sessionCredentials.sessionToken()).isNotNull();
    }
    
    @Test
    void closeDoesNotRaise() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(String.format("%s %s %s token=%s",
                                                             scriptLocation, ACCESS_KEY_ID, SECRET_ACCESS_KEY, SESSION_TOKEN))
                                      .build();
        credentialsProvider.resolveCredentials();
        credentialsProvider.close();
    }

    @Test
    void commandAsListOfStrings_isNotExecutedInAShell() {
        ProcessCredentialsProvider providerWithSingleStringCommand =
            ProcessCredentialsProvider.builder()
                                      .command("echo \"Hello, World!\" > output.txt; rm output.txt")
                                      .build();

        try {
            providerWithSingleStringCommand.resolveCredentials();
        } catch (IllegalStateException e) {
            // executed in a shell
            assertThat(e.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(e.getMessage()).isEqualTo("Failed to refresh process-based credentials.");
        }

        ProcessCredentialsProvider providerWithCommandAsListOfStrings =
            ProcessCredentialsProvider.builder()
                                      .command(Arrays.asList("echo \"Hello, World!\" > output.txt; rm output.txt"))
                                      .build();

        try {
            providerWithCommandAsListOfStrings.resolveCredentials();
        } catch (IllegalStateException e) {
            // executed not in a shell
            assertThat(e.getCause()).isInstanceOf(IOException.class);
            assertThat(e.getCause().getMessage())
                .isEqualTo("Cannot run program \"echo \"Hello, World!\" > output.txt; rm output.txt\": error=2, "
                           + "No such file or directory");
        }
    }

    public static String copyHappyCaseProcessCredentialsScript() {
        String scriptClasspathFilename = Platform.isWindows() ? "windows-credentials-script.bat"
                                                              : "linux-credentials-script.sh";

        return copyProcessCredentialsScript(scriptClasspathFilename);
    }

    public static String copyErrorCaseProcessCredentialsScript() {
        String scriptClasspathFilename = Platform.isWindows() ? "windows-credentials-error-script.bat"
                                                              : "linux-credentials-error-script.sh";

        return copyProcessCredentialsScript(scriptClasspathFilename);
    }

    private static String copyProcessCredentialsScript(String scriptClasspathFilename) {
        String scriptClasspathLocation = PROCESS_RESOURCE_PATH + scriptClasspathFilename;

        InputStream scriptInputStream = null;
        OutputStream scriptOutputStream = null;

        try {
            scriptInputStream = ProcessCredentialsProviderTest.class.getResourceAsStream(scriptClasspathLocation);

            File scriptFileOnDisk = File.createTempFile("ProcessCredentialsProviderTest", scriptClasspathFilename);
            scriptFileOnDisk.deleteOnExit();

            if (!scriptFileOnDisk.setExecutable(true)) {
                throw new IllegalStateException("Could not make " + scriptFileOnDisk + " executable.");
            }

            scriptOutputStream = new FileOutputStream(scriptFileOnDisk);

            IoUtils.copy(scriptInputStream, scriptOutputStream);

            return scriptFileOnDisk.getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IoUtils.closeQuietly(scriptInputStream, null);
            IoUtils.closeQuietly(scriptOutputStream, null);
        }
    }
}