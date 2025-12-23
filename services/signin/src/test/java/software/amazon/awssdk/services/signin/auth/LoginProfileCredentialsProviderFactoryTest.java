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

package software.amazon.awssdk.services.signin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.VALID_TEST_PEM;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileProviderCredentialsContext;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.signin.internal.AccessTokenManager;
import software.amazon.awssdk.services.signin.internal.LoginAccessToken;
import software.amazon.awssdk.services.signin.internal.LoginCacheDirectorySystemSetting;
import software.amazon.awssdk.services.signin.internal.OnDiskTokenManager;
import software.amazon.awssdk.utils.StringInputStream;

public class LoginProfileCredentialsProviderFactoryTest {
    private static final String LOGIN_SESSION_ID = "loginSessionId";

    @TempDir
    Path tempDir;

    private AccessTokenManager tokenManager;

    @BeforeEach
    public void setup() {
        System.setProperty(new LoginCacheDirectorySystemSetting().property(), tempDir.toString());
        tokenManager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);
    }

    @AfterEach
    public void teardown() {
        System.clearProperty(new LoginCacheDirectorySystemSetting().property());
    }

    @Test
    public void create_returnsLoginCredentialsFromProfile() {
        AwsSessionCredentials creds = buildCredentials( Instant.now().plusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        ProfileFile profileFile = configFile("[profile foo]\n" +
                                             "login_session=" + LOGIN_SESSION_ID + "\n");

        AwsCredentialsProvider provider = new LoginProfileCredentialsProviderFactory().create(
            ProfileProviderCredentialsContext
                .builder()
                .profile(profileFile.profile("foo").get())
                .profileFile(profileFile)
                .build()
        );

        // validate the creds are from cached token file stored above
        AwsCredentials credentials = provider.resolveCredentials();
        assertInstanceOf(AwsSessionCredentials.class, credentials);
        AwsSessionCredentials resolvedCredentials = (AwsSessionCredentials) credentials;
        assertEquals(creds.accessKeyId(), resolvedCredentials.accessKeyId());
        assertEquals(creds.secretAccessKey(), resolvedCredentials.secretAccessKey());
        assertEquals(creds.sessionToken(), resolvedCredentials.sessionToken());
        assertEquals(creds.accountId(), resolvedCredentials.accountId());
        assertEquals(BusinessMetricFeatureId.CREDENTIALS_LOGIN.value(), resolvedCredentials.providerName().get());
    }

    /**
     * This test validates that the {@link ProfileCredentialsUtils} used in the {@link ProfileCredentialsProvider}
     * correctly loads and configures login session credentials using the factory.
     * This test exists in this module because it depends on having the `signin` module available.
     */
    @Test
    public void profileCredentialsUtils_returnsLoginCredentialsFromProfile() {
        AwsSessionCredentials creds = buildCredentials( Instant.now().plusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        ProfileFile profileFile = configFile("[profile foo]\n" +
                                             "login_session=" + LOGIN_SESSION_ID + "\n");

        ProfileCredentialsUtils profileCredentialsUtils = new ProfileCredentialsUtils(
            profileFile, profileFile.profile("foo").get(), profileFile::profile
        );

        Optional<AwsCredentialsProvider> resolvedProvider = profileCredentialsUtils.credentialsProvider();
        assertTrue(resolvedProvider.isPresent());

        // validate the creds are from cached token file stored above
        AwsCredentials credentials = resolvedProvider.get().resolveCredentials();
        assertInstanceOf(AwsSessionCredentials.class, credentials);
        AwsSessionCredentials resolvedCredentials = (AwsSessionCredentials) credentials;
        assertEquals(creds.accessKeyId(), resolvedCredentials.accessKeyId());
        assertEquals(creds.secretAccessKey(), resolvedCredentials.secretAccessKey());
        assertEquals(creds.sessionToken(), resolvedCredentials.sessionToken());
        assertEquals(creds.accountId(), resolvedCredentials.accountId());
        String expectedCredSource = BusinessMetricFeatureId.CREDENTIALS_PROFILE_LOGIN.value() + ","
                                    + BusinessMetricFeatureId.CREDENTIALS_LOGIN.value();
        assertEquals(expectedCredSource, resolvedCredentials.providerName().get());
    }

    private static ProfileFile configFile(String configFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(configFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }



    private AwsSessionCredentials buildCredentials(Instant expirationTime) {
        return AwsSessionCredentials.builder()
                                    .accessKeyId("akid")
                                    .secretAccessKey("skid")
                                    .sessionToken("sessionToken")
                                    .accountId("123456789012")
                                    .expirationTime(expirationTime)
                                    .build();
    }

    private LoginAccessToken buildAccessToken(AwsSessionCredentials credentials) {
        return LoginAccessToken.builder()
                               .accessToken(credentials)
                               .clientId("client-123")
                               .dpopKey(VALID_TEST_PEM)
                               .refreshToken("refresh-token")
                               .tokenType("aws_sigv4")
                               .identityToken("id-token")
                               .build();
    }
}
