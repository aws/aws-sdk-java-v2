package software.amazon.awssdk.http.auth;

import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

public class TestUtils {

    static class AnonymousCredentialsIdentity implements AwsCredentialsIdentity {

        @Override
        public String accessKeyId() {
            return null;
        }

        @Override
        public String secretAccessKey() {
            return null;
        }
    }
}
