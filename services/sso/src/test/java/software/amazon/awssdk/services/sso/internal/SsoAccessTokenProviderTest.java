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
import org.junit.Test;

/**
 * Check if the behavior of {@link SsoAccessTokenProvider} is correct while consuming different formats of cached token
 * file.
 */
public class SsoAccessTokenProviderTest {

    private static final String START_URL = "https//d-abc123.awsapps.com/start";
    private static final String GENERATED_TOKEN_FILE_NAME = "6a888bdb653a4ba345dd68f21b896ec2e218c6f4.json";
    private static final String WRONG_TOKEN_FILE_NAME = "wrong-token-file.json";

    @Test
    public void cachedTokenFile_correctFormat_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThat(provider.resolveAccessToken()).isEqualTo("base64string");
    }

    @Test
    public void cachedTokenFile_accessTokenMissing_throwNullPointerException() throws IOException {
        String tokenFile = "{\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(provider::resolveAccessToken).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void cachedTokenFile_expiresAtMissing_throwNullPointerException() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";

        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(provider::resolveAccessToken).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void cachedTokenFile_optionalRegionMissing_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"startUrl\": \""+ START_URL +"\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThat(provider.resolveAccessToken()).isEqualTo("base64string");
    }

    @Test
    public void cachedTokenFile_optionalStartUrlMissing_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThat(provider.resolveAccessToken()).isEqualTo("base64string");
    }

    @Test
    public void cachedTokenFile_alreadyExpired_resolveAccessTokenCorrectly() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2019-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\"\n" +
                           "}";
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(provider::resolveAccessToken).hasMessageContaining("The SSO session associated with this profile "
                                                                              + "has expired or is otherwise invalid.");
    }

    @Test
    public void cachedTokenFile_tokenFileNotExist_throwNullPointerException() throws IOException {
        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2019-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\"\n" +
                           "}";
        prepareTestCachedTokenFile(tokenFile, WRONG_TOKEN_FILE_NAME);
        SsoAccessTokenProvider provider = new SsoAccessTokenProvider(createTestCachedTokenFilePath(
            Jimfs.newFileSystem(Configuration.unix()).getPath("./foo"), GENERATED_TOKEN_FILE_NAME));
        assertThatThrownBy(provider::resolveAccessToken).isInstanceOf(UncheckedIOException.class);
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

}
