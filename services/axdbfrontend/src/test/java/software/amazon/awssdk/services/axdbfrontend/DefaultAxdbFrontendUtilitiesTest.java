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
package software.amazon.awssdk.services.axdbfrontend;

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
import software.amazon.awssdk.services.axdbfrontend.DefaultAxdbFrontendUtilities.DefaultBuilder;
import software.amazon.awssdk.services.axdbfrontend.model.Action;
import software.amazon.awssdk.services.axdbfrontend.model.GenerateAuthenticationTokenRequest;

public class DefaultAxdbFrontendUtilitiesTest {
    private final ZoneId utcZone = ZoneId.of("UTC").normalized();
    private final Clock fixedClock = Clock.fixed(ZonedDateTime.of(2024, 11, 7, 17, 39, 33, 0, utcZone).toInstant(), utcZone);

    @Test
    public void tokenGenerationWithBuilderDefaultsUsingAwsCredentialsProvider_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_EAST_1);

        tokenGenerationWithBuilderDefaults(utilitiesBuilder);
    }

    @Test
    public void tokenGenerationWithBuilderDefaultsUsingIdentityProvider_isSuccessful() {
        IdentityProvider<AwsCredentialsIdentity> credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_EAST_1);

        tokenGenerationWithBuilderDefaults(utilitiesBuilder);
    }

    private void tokenGenerationWithBuilderDefaults(DefaultBuilder utilitiesBuilder) {
        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);
        Action action = Action.DB_CONNECT_SUPERUSER;

        String authenticationToken = AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.hostname("test.us-east-1.prod.sql.axdb.aws.dev")
                   .action(action);
        });

        String expectedToken = "test.us-east-1.prod.sql.axdb.aws.dev/?Action=DbConnectSuperuser&" +
                               "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20241107T173933Z&X-Amz-SignedHeaders=host&" +
                               "X-Amz-Expires=900&X-Amz-Credential=access_key%2F20241107%2Fus-east-1%2Fxanadu" +
                               "%2Faws4_request&" +
                               "X-Amz-Signature=f7666e716762021d3a381a2030a41b29419c70b39b0d669dd44dfd56870a860b";
        assertThat(authenticationToken).isEqualTo(expectedToken);
    }

    @Test
    public void tokenGenerationWithOverriddenCredentialsUsingAwsCredentialsProvider_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("foo", "bar")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_EAST_1);
        tokenGenerationWithOverriddenCredentials(utilitiesBuilder, builder -> {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("access_key", "secret_key")));
        });
    }

    @Test
    public void tokenGenerationWithOverriddenCredentialsUsingIdentityProvider_isSuccessful() {
        IdentityProvider<AwsCredentialsIdentity> credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("foo", "bar")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_EAST_1);
        tokenGenerationWithOverriddenCredentials(utilitiesBuilder, builder -> {
            builder.credentialsProvider((IdentityProvider<AwsCredentialsIdentity>) StaticCredentialsProvider.create(
                AwsBasicCredentials.create("access_key", "secret_key")));
        });
    }

    private void tokenGenerationWithOverriddenCredentials(DefaultBuilder utilitiesBuilder,
                                                              Consumer<GenerateAuthenticationTokenRequest.Builder> credsBuilder) {
        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);
        Action action = Action.DB_CONNECT_SUPERUSER;

        String authenticationToken = AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.hostname("test.us-east-1.prod.sql.axdb.aws.dev")
                   .action(action)
                   .applyMutation(credsBuilder);
        });

        String expectedToken = "test.us-east-1.prod.sql.axdb.aws.dev/?Action=DbConnectSuperuser&" +
                               "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20241107T173933Z&X-Amz-SignedHeaders=host&" +
                               "X-Amz-Expires=900&X-Amz-Credential=access_key%2F20241107%2Fus-east-1%2Fxanadu" +
                               "%2Faws4_request&" +
                               "X-Amz-Signature=f7666e716762021d3a381a2030a41b29419c70b39b0d669dd44dfd56870a860b";
        assertThat(authenticationToken).isEqualTo(expectedToken);
    }

    @Test
    public void tokenGenerationWithOverriddenRegion_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_EAST_1);

        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);
        Action action = Action.DB_CONNECT_SUPERUSER;

        String authenticationToken = AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.hostname("test.us-east-1.prod.sql.axdb.aws.dev")
                   .action(action)
                   .region(Region.US_WEST_2);
        });

        String expectedToken = "test.us-east-1.prod.sql.axdb.aws.dev/?Action=DbConnectSuperuser&" +
                               "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20241107T173933Z&X-Amz-SignedHeaders=host&" +
                               "X-Amz-Expires=900&X-Amz-Credential=access_key%2F20241107%2Fus-west-2%2Fxanadu" +
                               "%2Faws4_request&" +
                               "X-Amz-Signature=7da651e0e1811750c55246d38b99917ded3679af2dfd3cd1eced38946bce94e5";
        assertThat(authenticationToken).isEqualTo(expectedToken);
    }

    @Test
    public void missingRegion_throwsException() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider);

        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);
        Action action = Action.DB_CONNECT;

        assertThatThrownBy(() -> AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.hostname("test.us-east-1.prod.sql.axdb.aws.dev")
                   .action(action);
        })).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("Region should be provided");
    }

    @Test
    public void missingCredentials_throwsException() {
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                       .region(Region.US_WEST_2);

        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);
        Action action = Action.DB_CONNECT;

        assertThatThrownBy(() -> AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.hostname("test.us-east-1.prod.sql.axdb.aws.dev")
                   .action(action);
        })).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("CredentialProvider should be provided");
    }

    @Test
    public void missingHostname_throwsException() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                                .credentialsProvider(credentialsProvider);

        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);
        Action action = Action.DB_CONNECT;

        assertThatThrownBy(() -> AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.action(action);
        })).isInstanceOf(NullPointerException.class)
           .hasMessageContaining("hostname");
    }

    @Test
    public void missingAction_throwsException() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                                .credentialsProvider(credentialsProvider)
                                                                                .region(Region.US_EAST_1);

        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);

        assertThatThrownBy(() -> AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.hostname("test.us-east-1.prod.sql.axdb.aws.dev");
        })).isInstanceOf(NullPointerException.class)
           .hasMessageContaining("action");
    }

    @Test
    public void tokenGenerationWithCustomExpiry_isSuccessful() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) AxdbFrontendUtilities.builder()
                                                                                .credentialsProvider(credentialsProvider)
                                                                                .region(Region.US_EAST_1);

        DefaultAxdbFrontendUtilities AxdbFrontendUtilities = new DefaultAxdbFrontendUtilities(utilitiesBuilder, fixedClock);
        Action action = Action.DB_CONNECT;
        Duration expiry = Duration.ofSeconds(3600L);

        String authenticationToken = AxdbFrontendUtilities.generateAuthenticationToken(builder -> {
            builder.hostname("test.us-east-1.prod.sql.axdb.aws.dev")
                   .action(action)
                   .expiresIn(expiry);
        });

        String expectedToken = "test.us-east-1.prod.sql.axdb.aws.dev/?Action=DbConnect&" +
                               "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20241107T173933Z&X-Amz-SignedHeaders=host&" +
                               "X-Amz-Expires=3600&X-Amz-Credential=access_key%2F20241107%2Fus-east-1%2Fxanadu" +
                               "%2Faws4_request&" +
                               "X-Amz-Signature=2effdbadd1d7172c6ec5e3293d6209109bc8969b8d8b9d3394363a8e986a2377";
        assertThat(authenticationToken).isEqualTo(expectedToken);
    }
}