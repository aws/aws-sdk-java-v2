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
        "admin , d033e22ae348aeb5660fc2140aec35850c4da997.json",
        "dev-scopes, 75e4d41276d8bd17f85986fc6cccef29fd725ce3.json"
    })
    public void loadToken_loadsCorrectFile(String sessionName, String expectedLocation) throws IOException {
        Path tokenPath = cache.resolve(expectedLocation);
        Instant expiresAt = Instant.now();

        SsoOidcToken token = SsoOidcToken.builder()
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

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, sessionName);

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

        SsoOidcToken token = SsoOidcToken.builder()
                                         .accessToken("accesstoken")
                                         .expiresAt(expiresAt)
                                         .refreshToken("refreshtoken")
                                         .clientId("clientid")
                                         .clientSecret("clientsecret")
                                         .registrationExpiresAt(registrationExpiresAt)
                                         .region("region")
                                         .startUrl("starturl")
                                         .build();

        String ssoSession = "admin";
        String expectedFile = "d033e22ae348aeb5660fc2140aec35850c4da997.json";

        try (OutputStream os = Files.newOutputStream(cache.resolve(expectedFile), StandardOpenOption.CREATE,
                                                     StandardOpenOption.WRITE)) {
            os.write(tokenJson.getBytes(StandardCharsets.UTF_8));
        }

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, ssoSession);

        SsoOidcToken loadedToken = onDiskTokenManager.loadToken().get();

        assertThat(loadedToken).isEqualTo(token);
    }

    @Test
    public void storeToken_maximal() throws IOException {
        Instant expiresAt = Instant.now();
        Instant registrationExpiresAt = expiresAt.plus(Duration.ofDays(1));
        String startUrl = "https://d-abc123.awsapps.com/start";
        String ssoSessionName = "admin";

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

        SsoOidcToken token = SsoOidcToken.builder()
                                         .accessToken("accesstoken")
                                         .expiresAt(expiresAt)
                                         .refreshToken("refreshtoken")
                                         .clientId("clientid")
                                         .clientSecret("clientsecret")
                                         .registrationExpiresAt(registrationExpiresAt)
                                         .region("region")
                                         .startUrl(startUrl)
                                         .build();

        String expectedFile = "d033e22ae348aeb5660fc2140aec35850c4da997.json";

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, ssoSessionName);

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
        String sessionName = "ssoToken-Session";

        SsoOidcToken token = SsoOidcToken.builder()
                                         .accessToken("accesstoken")
                                         .expiresAt(expiresAt)
                                         .refreshToken("refreshtoken")
                                         .clientId("clientid")
                                         .clientSecret("clientsecret")
                                         .registrationExpiresAt(registrationExpiresAt)
                                         .region("region")
                                         .startUrl(startUrl)
                                         .build();

        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, sessionName);

        onDiskTokenManager.storeToken(token);
        SsoOidcToken loadedToken = onDiskTokenManager.loadToken().get();

        assertThat(loadedToken).isEqualTo(token);
    }

    @Test
    public void loadToken_cachedValueNotFound_returnsEmpty() {
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(cache, "does-not-exist-session");
        assertThat(onDiskTokenManager.loadToken()).isEmpty();
    }


}
