package software.amazon.awssdk.http.auth.aws.crt.internal;

import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner;

public class SignerHelper {

    public static AwsSigningConfig getSigningConfig(AwsCrtV4aHttpSigner signer) {
        if (signer instanceof DefaultAwsCrtV4aHttpSigner) {
            return ((DefaultAwsCrtV4aHttpSigner) signer).signingConfig;
        }
        return null;
    }
}
