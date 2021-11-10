package software.amazon.awssdk.services.s3control;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class EndpointResolutionTest {
    @Test(expected = IllegalStateException.class)
    public void duplicateDualstackSettings_throwsException() {
        S3ControlClient.builder()
                       .region(Region.US_WEST_2)
                       .credentialsProvider(AnonymousCredentialsProvider.create())
                       .dualstackEnabled(true)
                       .serviceConfiguration(c -> c.dualstackEnabled(true))
                       .build();
    }
}
