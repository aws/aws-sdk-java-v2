package software.amazon.awssdk.auth.credentials;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AwsCredentialsProviderTest {

    @Test
    public void provide() {
        final String testAccessKeyId = "test-access-key-id";
        final String testSecretAccessKey = "test-secret-access-key";
        final AwsCredentialsProvider awsCredentialsProvider = AwsCredentialsProvider.provide(() -> testAccessKeyId, () -> testSecretAccessKey);
        assertNotNull(awsCredentialsProvider);
        final AwsCredentials awsCredentials = awsCredentialsProvider.resolveCredentials();
        assertNotNull(awsCredentials);
        assertEquals(testAccessKeyId, awsCredentials.accessKeyId());
        assertEquals(testSecretAccessKey, awsCredentials.secretAccessKey());
    }
}