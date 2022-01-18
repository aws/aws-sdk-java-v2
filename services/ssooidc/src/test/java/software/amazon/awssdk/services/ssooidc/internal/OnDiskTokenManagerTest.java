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

package software.amazon.awssdk.services.ssooidc.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;

public class OnDiskTokenManagerTest {
    private static final JsonNodeParser PARSER = JsonNodeParser.builder().removeErrorLocations(true).build();

    private FileSystem testFs;
    private Path cache;

    @BeforeEach
    public void setup() throws IOException {
        testFs = Jimfs.newFileSystem(Configuration.unix());
        cache = testFs.getPath("/cache");
        Files.createDirectory(cache);
    }

    @AfterEach
    public void teardown() throws IOException {
        testFs.close();
    }

    @ParameterizedTest
    @CsvSource({
        "https://d-abc123.awsapps.com/start, 40a89917e3175433e361b710a9d43528d7f1890a.json",
        "https://d-abc123.awsapps.com, ab6e8a5ac0e97bd6867ae3eacd5fd7dc0cd4de86.json",
        "https://vanity.example.com, 7ed69759fe20aa3027c01db4b744e1437b0c8f4f.json"
    })
    public void loadToken_loadsCorrectFile(String startUrl, String expectedLocation) throws IOException {
        Path tokenPath = cache.resolve(expectedLocation);
        Instant expiresAt = Instant.now();

        SsoToken token = SsoToken.builder()
                                 .accessToken("accesstoken")
                                 .expiresAt(expiresAt)
                                 .build();


        String tokenJson = String.format("{\n"
                                         + "    \"accessToken\": \"accesstoken\",\n"
                                         + "    \"expiresAt\": \"%s\"\n"
                                         + "}", DateTimeFormatter.ISO_INSTANT.format(expiresAt));

        try (OutputStream os = Files.newOutputStream(tokenPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            os.write(tokenJson.getBytes(StandardCharsets.UTF_8));
        }

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, startUrl);

        assertThat(onDiskTokenManager.loadToken().get()).isEqualTo(token);
    }

    @Test
    public void loadToken_maximal() throws IOException {
        Instant expiresAt = Instant.now();
        Instant registrationExpiresAt = expiresAt.plus(Duration.ofDays(1));

        String tokenJson = String.format("{\n"
                                         + "    \"accessToken\": \"accesstoken\",\n"
                                         + "    \"expiresAt\": \"%s\",\n"
                                         + "    \"refreshToken\": \"refreshtoken\",\n"
                                         + "    \"clientId\": \"clientid\",\n"
                                         + "    \"clientSecret\": \"clientsecret\",\n"
                                         + "    \"registrationExpiresAt\": \"%s\",\n"
                                         + "    \"region\": \"region\",\n"
                                         + "    \"startUrl\": \"starturl\"\n"
                                         + "}",
                                         DateTimeFormatter.ISO_INSTANT.format(expiresAt),
                                         DateTimeFormatter.ISO_INSTANT.format(registrationExpiresAt));

        SsoToken token = SsoToken.builder()
            .accessToken("accesstoken")
            .expiresAt(expiresAt)
            .refreshToken("refreshtoken")
            .clientId("clientid")
            .clientSecret("clientsecret")
            .registrationExpiresAt(registrationExpiresAt)
            .region("region")
            .startUrl("starturl")
            .build();

        String startUrl = "https://d-abc123.awsapps.com/start";
        String expectedFile = "40a89917e3175433e361b710a9d43528d7f1890a.json";

        try (OutputStream os = Files.newOutputStream(cache.resolve(expectedFile), StandardOpenOption.CREATE,
                                                     StandardOpenOption.WRITE)) {
            os.write(tokenJson.getBytes(StandardCharsets.UTF_8));
        }

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, startUrl);

        SsoToken loadedToken = onDiskTokenManager.loadToken().get();

        assertThat(loadedToken).isEqualTo(token);
    }

    @Test
    public void storeToken_maximal() throws IOException {
        Instant expiresAt = Instant.now();
        Instant registrationExpiresAt = expiresAt.plus(Duration.ofDays(1));
        String startUrl = "https://d-abc123.awsapps.com/start";

        String tokenJson = String.format("{\n"
                                         + "    \"accessToken\": \"accesstoken\",\n"
                                         + "    \"expiresAt\": \"%s\",\n"
                                         + "    \"refreshToken\": \"refreshtoken\",\n"
                                         + "    \"clientId\": \"clientid\",\n"
                                         + "    \"clientSecret\": \"clientsecret\",\n"
                                         + "    \"registrationExpiresAt\": \"%s\",\n"
                                         + "    \"region\": \"region\",\n"
                                         + "    \"startUrl\": \"%s\"\n"
                                         + "}",
                                         DateTimeFormatter.ISO_INSTANT.format(expiresAt),
                                         DateTimeFormatter.ISO_INSTANT.format(registrationExpiresAt),
                                         startUrl);

        SsoToken token = SsoToken.builder()
                                 .accessToken("accesstoken")
                                 .expiresAt(expiresAt)
                                 .refreshToken("refreshtoken")
                                 .clientId("clientid")
                                 .clientSecret("clientsecret")
                                 .registrationExpiresAt(registrationExpiresAt)
                                 .region("region")
                                 .startUrl(startUrl)
                                 .build();

        String expectedFile = "40a89917e3175433e361b710a9d43528d7f1890a.json";

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, startUrl);

        onDiskTokenManager.storeToken(token);

        JsonNode expectedJson = PARSER.parse(tokenJson);
        try (InputStream is = Files.newInputStream(cache.resolve(expectedFile))) {
            JsonNode storedJson = PARSER.parse(is);
            assertThat(storedJson).isEqualTo(expectedJson);
        }
    }

    @Test
    public void storeToken_loadToken_roundTrip() {
        Instant expiresAt = Instant.now();
        Instant registrationExpiresAt = expiresAt.plus(Duration.ofDays(1));
        String startUrl = "https://d-abc123.awsapps.com/start";

        SsoToken token = SsoToken.builder()
                                 .accessToken("accesstoken")
                                 .expiresAt(expiresAt)
                                 .refreshToken("refreshtoken")
                                 .clientId("clientid")
                                 .clientSecret("clientsecret")
                                 .registrationExpiresAt(registrationExpiresAt)
                                 .region("region")
                                 .startUrl(startUrl)
                                 .build();

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, startUrl);

        onDiskTokenManager.storeToken(token);
        SsoToken loadedToken = onDiskTokenManager.loadToken().get();

        assertThat(loadedToken).isEqualTo(token);
    }

    @Test
    public void loadToken_cachedValueNotFound_returnsEmpty() {
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, "https://does-not-exist.com");
        assertThat(onDiskTokenManager.loadToken()).isEmpty();
    }

    @Test
    public void storeToken_tokenHasDifferentStartUrl_throws() {
        String startUrl = "https://my-start-url.com";
        String startUrl2 = "https://my-other-start-url.com";

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, startUrl);
        SsoToken token = SsoToken.builder()
                                 .accessToken("accesstoken")
                                 .expiresAt(Instant.now())
                                 .startUrl(startUrl2)
                                 .build();

        assertThatThrownBy(() -> onDiskTokenManager.storeToken(token))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Cannot store token with different startUrl");
    }
}
