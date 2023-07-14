package software.amazon.awssdk.http.auth;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.PAYLOAD_SIGNING;
import static software.amazon.awssdk.http.auth.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.getDelegate;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.internal.AwsV4HeaderHttpSigner;
import software.amazon.awssdk.http.auth.internal.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.AwsV4PresignedHttpSigner;
import software.amazon.awssdk.http.auth.internal.AwsV4UnsignedPayloadHttpSigner;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4QueryHttpSigner;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Test the delegation of signing to the correct signer implementations.
 */
public class AwsV4HttpSignerTest {

    BaseAwsV4HttpSigner<?> signer = BaseAwsV4HttpSigner.create();

    @Test
    public void sign_WithNoAdditonalProperties_DelegatesToHeaderSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> {})
        );

        AwsV4HttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(AwsV4HeaderHttpSigner.class);
    }

    @Test
    public void sign_WithQueryAuthLocation_DelegatesToQuerySigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> signRequest.putProperty(AUTH_LOCATION, "QueryString"))
        );

        AwsV4HttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(DefaultAwsV4QueryHttpSigner.class);
    }

    @Test
    public void sign_WithQueryAuthLocationAndExpiration_DelegatesToPresignedSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> signRequest
                .putProperty(AUTH_LOCATION, "QueryString")
                .putProperty(EXPIRATION_DURATION, Duration.ZERO)
            )
        );

        AwsV4HttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(AwsV4PresignedHttpSigner.class);
    }

    @Test
    public void sign_WithPayloadSigningFalse_DelegatesToUnsignedPayloadSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING, false)
            )
        );

        AwsV4HttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(AwsV4UnsignedPayloadHttpSigner.class);
    }

    // TODO: Add test for s3-signer once the migration is done
}
