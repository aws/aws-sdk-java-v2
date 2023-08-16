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

package software.amazon.awssdk.services.sso.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

/**
 * Check if the behavior of {@link SsoAccessTokenProvider} is correct while consuming different formats of cached token
 * file.
 */
public class SsoAccessTokenProviderTest {

    private static final String START_URL = "https//d-abc123.awsapps.com/start";
    private static final String GENERATED_TOKEN_FILE_NAME = "6a888bdb653a4ba345dd68f21b896ec2e218c6f4.json";
    private static final String WRONG_TOKEN_FILE_NAME = "wrong-token-file.json";

    @Test
    void cachedTokenFile_correctFormat_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThat(provider.resolveToken().token()).isEqualTo("base64string");
    }

    @Test
    void cachedTokenFile_accessTokenMissing_throwNullPointerException() throws IOException {
        String tokenFile = "{\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(() -> provider.resolveToken().token()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void cachedTokenFile_expiresAtMissing_throwNullPointerException() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";

        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(() -> provider.resolveToken().token()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void cachedTokenFile_optionalRegionMissing_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThat(provider.resolveToken().token()).isEqualTo("base64string");
    }

    @Test
    void cachedTokenFile_optionalStartUrlMissing_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThat(provider.resolveToken().token()).isEqualTo("base64string");
    }

    @Test
    void cachedTokenFile_alreadyExpired_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2019-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(() -> provider.resolveToken().token()).hasMessageContaining("The SSO session associated with this profile "
                                                                              + "has expired or is otherwise invalid.");
    }

    @Test
    void cachedTokenFile_tokenFileNotExist_throwNullPointerException() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2019-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\"\n" +
                           "}";
        prepareTestCachedTokenFile(tokenFile, WRONG_TOKEN_FILE_NAME);
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(createTestCachedTokenFilePath(
            Jimfs.newFileSystem(Configuration.unix()).getPath("./foo"), GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(() -> provider.resolveToken().token()).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void cachedTokenFile_AboutToExpire_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = String.format("{\n" +
                                         "\"accessToken\": \"base64string\",\n" +
                                         "\"expiresAt\": \"%s\",\n" +
                                         "\"startUrl\": \""+ START_URL +"\"\n" +
                                         "}", stringFormattedTime(Instant.now().plusSeconds(10)));
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThat(provider.resolveToken().token()).isEqualTo("base64string");
    }

    @Test
    void cachedTokenFile_JustExpired_throwsExpiredTokenException() throws IOException {
        String tokenFile = String.format("{\n" +
                                         "\"accessToken\": \"base64string\",\n" +
                                         "\"expiresAt\": \"%s\",\n" +
                                         "\"startUrl\": \""+ START_URL +"\"\n" +
                                         "}", stringFormattedTime(Instant.now()));
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(() -> provider.resolveToken().token()).hasMessageContaining("The SSO session associated with this profile "
                                                                                       + "has expired or is otherwise invalid.");
    }

    @Test
    void cachedTokenFile_ExpiredFewSecondsAgo_throwsExpiredTokenException() throws IOException {
        String tokenFile = String.format("{\n" +
                                         "\"accessToken\": \"base64string\",\n" +
                                         "\"expiresAt\": \"%s\",\n" +
                                         "\"startUrl\": \""+ START_URL +"\"\n" +
                                         "}", stringFormattedTime(Instant.now().minusSeconds(1)));
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(() -> provider.resolveToken().token()).hasMessageContaining("The SSO session associated with this profile "
                                                                                       + "has expired or is otherwise invalid.");
    }

    private Path prepareTestCachedTokenFile(String tokenFileContent, String generatedTokenFileName) throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path fileDirectory = fs.getPath("./foo");

        Files.createDirectory(fileDirectory);
        Path cachedTokenFilePath = createTestCachedTokenFilePath(fileDirectory, generatedTokenFileName);
        Files.write(cachedTokenFilePath, ImmutableList.of(tokenFileContent), StandardCharsets.UTF_8);

        return cachedTokenFilePath;
    }

    private Path createTestCachedTokenFilePath(Path directory, String tokenFileName) {
        return directory.resolve(tokenFileName);
    }

    private String stringFormattedTime(Instant instant){
        // Convert Instant to ZonedDateTime with UTC time zone
        ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return formatter.format(zonedDateTime);
    }

}
