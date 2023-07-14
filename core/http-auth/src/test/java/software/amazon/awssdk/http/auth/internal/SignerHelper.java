package software.amazon.awssdk.http.auth.internal;

import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

public class SignerHelper {

    public static software.amazon.awssdk.http.auth.AwsV4HttpSigner getDelegate(BaseAwsV4HttpSigner<?> v4Signer,
                                                                               SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        return DefaultAwsV4HttpSigner.getDelegate(v4Signer, signRequest);
    }
}
