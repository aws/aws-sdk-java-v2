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
package software.amazon.awssdk.services.dsql;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dsql.DefaultDsqlUtilities.DefaultBuilder;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;

public class DefaultDsqlUtilitiesTest {
    private final ZoneId utcZone = ZoneId.of("UTC").normalized();
    private final Clock fixedClock = Clock.fixed(ZonedDateTime.of(2024, 11, 7, 17, 39, 33, 0, utcZone).toInstant(), utcZone);
    private static final String HOSTNAME = "test.dsql.us-east-1.on.aws";
    private static final String EXPECTED_TOKEN = "test.dsql.us-east-1.on.aws/?Action=DbConnect&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date"
                                                 + "=20241107T173933Z&X-Amz-SignedHeaders=host&X-Amz-Expires=900&X-Amz-Credential=access_key"
                                                 + "%2F20241107%2Fus-east-1%2Fdsql%2Faws4_request&X-Amz-Signature=e319d85380261f643d78a558f76257f05aacea758a6ccd42a2510e2ae0854a47";
    private static final String EXPECTED_ADMIN_TOKEN = "test.dsql.us-east-1.on.aws/?Action=DbConnectAdmin&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date"
                                                       + "=20241107T173933Z&X-Amz-SignedHeaders=host&X-Amz-Expires=900&X-Amz-Credential=access_key"
                                                       + "%2F20241107%2Fus-east-1%2Fdsql%2Faws4_request&X-Amz-Signature=a08adc4c84a490014ce374b90c98ba9ed015b77b451c0d9f9fb3f8ca8c6f9c36";

    @Test
    public void tokenGenerationWithBuilderDefaultsUsingAwsCredentialsProvider_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key"));

        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .credentialsProvider(credentialsProvider)
                                                     .region(Region.US_EAST_1);

        tokenGenerationWithBuilderDefaults(builder);
    }

    @Test
    public void tokenGenerationWithBuilderDefaultsUsingIdentityProvider_isSuccessful() {
        IdentityProvider<AwsCredentialsIdentity> credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key"));

        DsqlUtilities.Builder utilitiesBuilder = DsqlUtilities.builder()
                                                              .credentialsProvider(credentialsProvider)
                                                              .region(Region.US_EAST_1);

        tokenGenerationWithBuilderDefaults(utilitiesBuilder);
    }

    private void tokenGenerationWithBuilderDefaults(DsqlUtilities.Builder builder) {
        DsqlUtilities dsqlUtilities = new DefaultDsqlUtilities((DefaultBuilder) builder, fixedClock);

        String authToken = dsqlUtilities.generateDbConnectAuthToken(b -> b.hostname(HOSTNAME));
        assertThat(authToken).isEqualTo(EXPECTED_TOKEN);

        String adminAuthToken = dsqlUtilities.generateDbConnectAdminAuthToken(b -> b.hostname(HOSTNAME));
        assertThat(adminAuthToken).isEqualTo(EXPECTED_ADMIN_TOKEN);
    }

    @Test
    public void tokenGenerationWithOverriddenCredentialsUsingAwsCredentialsProvider_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("foo", "bar"));

        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .credentialsProvider(credentialsProvider)
                                                     .region(Region.US_EAST_1);

        tokenGenerationWithOverriddenCredentials(builder, b -> b.credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key"))));
    }

    @Test
    public void tokenGenerationWithOverriddenCredentialsUsingIdentityProvider_isSuccessful() {
        IdentityProvider<AwsCredentialsIdentity> credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("foo", "bar"));

        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .credentialsProvider(credentialsProvider)
                                                     .region(Region.US_EAST_1);

        tokenGenerationWithOverriddenCredentials(builder, b -> b.credentialsProvider(
            (IdentityProvider<AwsCredentialsIdentity>) StaticCredentialsProvider.create(AwsBasicCredentials.create("access_key", "secret_key"))));
    }

    private void tokenGenerationWithOverriddenCredentials(DsqlUtilities.Builder builder,
                                                          Consumer<GenerateAuthTokenRequest.Builder> credsBuilder) {
        DsqlUtilities dsqlUtilities = new DefaultDsqlUtilities((DefaultBuilder) builder, fixedClock);

        String authToken = dsqlUtilities.generateDbConnectAuthToken(b -> b.hostname(HOSTNAME).applyMutation(credsBuilder));
        assertThat(authToken).isEqualTo(EXPECTED_TOKEN);

        String adminAuthToken = dsqlUtilities.generateDbConnectAdminAuthToken(b -> b.hostname(HOSTNAME).applyMutation(credsBuilder));
        assertThat(adminAuthToken).isEqualTo(EXPECTED_ADMIN_TOKEN);
    }

    @Test
    public void tokenGenerationWithOverriddenRegion_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key"));

        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .credentialsProvider(credentialsProvider)
                                                     .region(Region.US_WEST_2);

        DsqlUtilities utilities = new DefaultDsqlUtilities((DefaultBuilder) builder, fixedClock);

        String authToken = utilities.generateDbConnectAuthToken(b -> b.hostname(HOSTNAME).region(Region.US_EAST_1));
        assertThat(authToken).isEqualTo(EXPECTED_TOKEN);

        String adminAuthToken = utilities.generateDbConnectAdminAuthToken(b -> b.hostname(HOSTNAME).region(Region.US_EAST_1));
        assertThat(adminAuthToken).isEqualTo(EXPECTED_ADMIN_TOKEN);
    }

    @Test
    public void missingRegion_throwsException() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key"));

        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .credentialsProvider(credentialsProvider);

        DsqlUtilities utilities = new DefaultDsqlUtilities((DefaultBuilder) builder, fixedClock);

        assertThatThrownBy(() -> utilities.generateDbConnectAuthToken(b -> b.hostname(HOSTNAME)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Region must be provided in GenerateAuthTokenRequest or DsqlUtilities");

        assertThatThrownBy(() -> utilities.generateDbConnectAdminAuthToken(b -> b.hostname(HOSTNAME)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Region must be provided in GenerateAuthTokenRequest or DsqlUtilities");
    }

    @Test
    public void missingCredentials_throwsException() {
        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .region(Region.US_WEST_2);

        DsqlUtilities utilities = new DefaultDsqlUtilities((DefaultBuilder) builder, fixedClock);

        assertThatThrownBy(() -> utilities.generateDbConnectAuthToken(b -> b.hostname(HOSTNAME)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CredentialsProvider must be provided in GenerateAuthTokenRequest or DsqlUtilities");

        assertThatThrownBy(() -> utilities.generateDbConnectAdminAuthToken(b -> b.hostname(HOSTNAME)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CredentialsProvider must be provided in GenerateAuthTokenRequest or DsqlUtilities");
    }

    @Test
    public void missingHostname_throwsException() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key"));

        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .credentialsProvider(credentialsProvider);

        DsqlUtilities utilities = new DefaultDsqlUtilities((DefaultBuilder) builder, fixedClock);

        assertThatThrownBy(() -> utilities.generateDbConnectAuthToken(GenerateAuthTokenRequest.builder().build()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("hostname");

        assertThatThrownBy(() -> utilities.generateDbConnectAdminAuthToken(GenerateAuthTokenRequest.builder().build()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("hostname");
    }

    @Test
    public void tokenGenerationWithCustomExpiry_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key"));

        DsqlUtilities.Builder builder = DsqlUtilities.builder()
                                                     .credentialsProvider(credentialsProvider)
                                                     .region(Region.US_EAST_1);

        DsqlUtilities utilities = new DefaultDsqlUtilities((DefaultBuilder) builder, fixedClock);
        Duration expiry = Duration.ofSeconds(3600L);

        String authToken = utilities.generateDbConnectAuthToken(b -> b.hostname(HOSTNAME).expiresIn(expiry));
        String expectedToken = "test.dsql.us-east-1.on.aws/?Action=DbConnect&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date="
                               + "20241107T173933Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3600&X-Amz-Credential=access_key"
                               + "%2F20241107%2Fus-east-1%2Fdsql%2Faws4_request&X-Amz-Signature=63987ab6908fe81bfcaa5a5120444a8d012751992bcdfec351522db555232c51";
        assertThat(authToken).isEqualTo(expectedToken);

        String adminAuthToken = utilities.generateDbConnectAdminAuthToken(b -> b.hostname(HOSTNAME).expiresIn(expiry));
        String expectedAdminToken = "test.dsql.us-east-1.on.aws/?Action=DbConnectAdmin&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date="
                                    + "20241107T173933Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3600&X-Amz-Credential=access_key"
                                    + "%2F20241107%2Fus-east-1%2Fdsql%2Faws4_request&X-Amz-Signature=46e02dfc8d6d07289b9e5910ccd9a50f1c3b798e48ccd92153255df508bd0b82";
        assertThat(adminAuthToken).isEqualTo(expectedAdminToken);
    }
}