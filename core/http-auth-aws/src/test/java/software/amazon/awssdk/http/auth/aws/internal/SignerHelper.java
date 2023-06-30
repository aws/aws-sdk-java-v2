package software.amazon.awssdk.http.auth.aws.internal;

import software.amazon.awssdk.http.auth.aws.AwsS3V4HttpSigner;
import software.amazon.awssdk.http.auth.aws.internal.io.AwsChunkedEncodingConfig;

public class SignerHelper {

    public static void setEncodingConfig(AwsS3V4HttpSigner signer, AwsChunkedEncodingConfig config) {
        if (signer instanceof DefaultAwsS3V4HttpSigner) {
            ((DefaultAwsS3V4HttpSigner) signer).encodingConfig = config;
        }
    }
}
