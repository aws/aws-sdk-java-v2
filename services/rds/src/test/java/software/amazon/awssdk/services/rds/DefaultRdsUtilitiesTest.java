package software.amazon.awssdk.services.rds;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import software.amazon.awssdk.services.rds.DefaultRdsUtilities.DefaultBuilder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DefaultRdsUtilitiesTest {
    private final ZoneId utcZone = ZoneId.of("UTC").normalized();
    private final Clock fixedClock = Clock.fixed(ZonedDateTime.of(2016, 11, 7, 17, 39, 33, 0, utcZone).toInstant(), utcZone);

    @Test
    public void testTokenGenerationWithBuilderDefaults() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) RdsUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_EAST_1);

        DefaultRdsUtilities rdsUtilities = new DefaultRdsUtilities(utilitiesBuilder, fixedClock);

        String authenticationToken = rdsUtilities.generateAuthenticationToken(builder -> {
            builder.username("mySQLUser")
                   .hostname("host.us-east-1.amazonaws.com")
                   .port(3306);
        });

        String expectedToken = "host.us-east-1.amazonaws.com:3306/?DBUser=mySQLUser&Action=connect&" +
                               "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20161107T173933Z&X-Amz-SignedHeaders=host&" +
                               "X-Amz-Expires=900&X-Amz-Credential=access_key%2F20161107%2Fus-east-1%2Frds-db%2Faws4_request&" +
                               "X-Amz-Signature=87ab58107ef49f1c311a412f98b7f976b0b5152ffb559f0d36c6c9a0c5e0e362";
        assertThat(authenticationToken).isEqualTo(expectedToken);
    }

    @Test
    public void testTokenGenerationWithOverriddenCredentials() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("foo", "bar")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) RdsUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_EAST_1);
        DefaultRdsUtilities rdsUtilities = new DefaultRdsUtilities(utilitiesBuilder, fixedClock);

        String authenticationToken = rdsUtilities.generateAuthenticationToken(builder -> {
            builder.username("mySQLUser")
                   .hostname("host.us-east-1.amazonaws.com")
                   .port(3306)
                   .credentialsProvider(StaticCredentialsProvider.create(
                       AwsBasicCredentials.create("access_key", "secret_key")
                   ));
        });

        String expectedToken = "host.us-east-1.amazonaws.com:3306/?DBUser=mySQLUser&Action=connect&" +
                               "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20161107T173933Z&X-Amz-SignedHeaders=host&" +
                               "X-Amz-Expires=900&X-Amz-Credential=access_key%2F20161107%2Fus-east-1%2Frds-db%2Faws4_request&" +
                               "X-Amz-Signature=87ab58107ef49f1c311a412f98b7f976b0b5152ffb559f0d36c6c9a0c5e0e362";
        assertThat(authenticationToken).isEqualTo(expectedToken);
    }

    @Test
    public void testTokenGenerationWithOverriddenRegion() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) RdsUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider)
                                                                       .region(Region.US_WEST_2);

        DefaultRdsUtilities rdsUtilities = new DefaultRdsUtilities(utilitiesBuilder, fixedClock);

        String authenticationToken = rdsUtilities.generateAuthenticationToken(builder -> {
            builder.username("mySQLUser")
                   .hostname("host.us-east-1.amazonaws.com")
                   .port(3306)
                   .region(Region.US_EAST_1);
        });

        String expectedToken = "host.us-east-1.amazonaws.com:3306/?DBUser=mySQLUser&Action=connect&" +
                               "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20161107T173933Z&X-Amz-SignedHeaders=host&" +
                               "X-Amz-Expires=900&X-Amz-Credential=access_key%2F20161107%2Fus-east-1%2Frds-db%2Faws4_request&" +
                               "X-Amz-Signature=87ab58107ef49f1c311a412f98b7f976b0b5152ffb559f0d36c6c9a0c5e0e362";
        assertThat(authenticationToken).isEqualTo(expectedToken);
    }

    @Test
    public void testMissingRegionThrowsException() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("access_key", "secret_key")
        );
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) RdsUtilities.builder()
                                                                       .credentialsProvider(credentialsProvider);

        DefaultRdsUtilities rdsUtilities = new DefaultRdsUtilities(utilitiesBuilder, fixedClock);

        assertThatThrownBy(() -> rdsUtilities.generateAuthenticationToken(builder -> {
            builder.username("mySQLUser")
                   .hostname("host.us-east-1.amazonaws.com")
                   .port(3306);
        })).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("Region should be provided");
    }

    @Test
    public void testMissingCredentialsThrowsException() {
        DefaultBuilder utilitiesBuilder = (DefaultBuilder) RdsUtilities.builder()
                                                                       .region(Region.US_WEST_2);

        DefaultRdsUtilities rdsUtilities = new DefaultRdsUtilities(utilitiesBuilder, fixedClock);

        assertThatThrownBy(() -> rdsUtilities.generateAuthenticationToken(builder -> {
            builder.username("mySQLUser")
                   .hostname("host.us-east-1.amazonaws.com")
                   .port(3306);
        })).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("CredentialProvider should be provided");
    }
}