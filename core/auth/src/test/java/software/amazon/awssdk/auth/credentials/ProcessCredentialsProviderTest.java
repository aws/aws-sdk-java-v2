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
import static software.amazon.awssdk.auth.credentials.internal.ProcessCredentialsTestUtils.copyErrorCaseProcessCredentialsScript;
import static software.amazon.awssdk.auth.credentials.internal.ProcessCredentialsTestUtils.copyHappyCaseProcessCredentialsScript;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.DateUtils;

class ProcessCredentialsProviderTest {

    private static final String PROCESS_RESOURCE_PATH = "/resources/process/";
    private static final String RANDOM_SESSION_TOKEN = "RANDOM_TOKEN";
    private static String scriptLocation;
    private static String errorScriptLocation;
 
    @BeforeAll
    public static void setup()  {
        scriptLocation = copyHappyCaseProcessCredentialsScript();
        errorScriptLocation = copyErrorCaseProcessCredentialsScript();
    }
 
    @AfterAll
    public static void teardown() {
        if (scriptLocation != null && !new File(scriptLocation).delete()) {
            throw new IllegalStateException("Failed to delete file: " + scriptLocation);
        }

        if (errorScriptLocation != null && !new File(errorScriptLocation).delete()) {
            throw new IllegalStateException("Failed to delete file: " + errorScriptLocation);
        }
    }
 
    @Test
    void staticCredentialsCanBeLoaded() {
        AwsCredentials credentials =
                ProcessCredentialsProvider.builder()
                                          .command(scriptLocation + " accessKeyId secretAccessKey")
                                          .build()
                                          .resolveCredentials();
 
        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("accessKeyId");
        assertThat(credentials.secretAccessKey()).isEqualTo("secretAccessKey");
        assertThat(credentials.providerName()).isPresent().contains("ProcessCredentialsProvider");
    }

    @Test
    public void staticCredentials_commandAsListOfStrings_CanBeLoaded() {
        AwsCredentials credentials =
            ProcessCredentialsProvider.builder()
                                      .command(Arrays.asList(scriptLocation, "accessKeyId", "secretAccessKey"))
                                      .build()
                                      .resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("accessKeyId");
        assertThat(credentials.secretAccessKey()).isEqualTo("secretAccessKey");
        assertThat(credentials.providerName()).isPresent().contains("ProcessCredentialsProvider");
    }
 
    @Test
    void sessionCredentialsCanBeLoaded() {
        ProcessCredentialsProvider credentialsProvider =
                ProcessCredentialsProvider.builder()
                                          .command(scriptLocation + " accessKeyId secretAccessKey sessionToken " +
                                                   DateUtils.formatIso8601Date(Instant.now()))
                                          .credentialRefreshThreshold(Duration.ofSeconds(1))
                                          .build();

        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);

        AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;

        assertThat(credentials.accessKeyId()).isEqualTo("accessKeyId");
        assertThat(credentials.secretAccessKey()).isEqualTo("secretAccessKey");
        assertThat(sessionCredentials.sessionToken()).isNotNull();
    }

    @Test
    void resultsAreCached() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(scriptLocation + " accessKeyId secretAccessKey sessionToken " +
                                               DateUtils.formatIso8601Date(Instant.now().plusSeconds(20)))
                                      .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void expirationBufferOverrideIsApplied() {
        ProcessCredentialsProvider credentialsProvider =
                ProcessCredentialsProvider.builder()
                                          .command(String.format("%s accessKeyId secretAccessKey %s %s",
                                                         scriptLocation,
                                                         RANDOM_SESSION_TOKEN,
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
                                      .command(scriptLocation + " accessKeyId secretAccessKey sessionToken")
                                      .credentialRefreshThreshold(Duration.ofSeconds(20))
                                      .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        assertThat(request1).isEqualTo(request2);
    }
 
    @Test
    void processOutputLimitIsEnforced() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(scriptLocation + " accessKeyId secretAccessKey")
                                      .processOutputLimit(1)
                                      .build();
        assertThatThrownBy(credentialsProvider::resolveCredentials).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void processOutputLimitDefaultPassesLargeInput() {

        String LONG_SESSION_TOKEN = "lYzvmByqdS1E69QQVEavDDHabQ2GuYKYABKRA4xLbAXpdnFtV030UH4" +
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

        ProcessCredentialsProvider credentialsProvider = ProcessCredentialsProvider.builder()
                .command(scriptLocation + " accessKeyId secretAccessKey " + LONG_SESSION_TOKEN)
                .build();

        AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentialsProvider.resolveCredentials();

        assertThat(sessionCredentials.accessKeyId()).isEqualTo("accessKeyId");
        assertThat(sessionCredentials.sessionToken()).isNotNull();
    }
    
    @Test
    void closeDoesNotRaise() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(scriptLocation + " accessKeyId secretAccessKey sessionToken")
                                      .build();
        credentialsProvider.resolveCredentials();
        credentialsProvider.close();
    }
}