package software.amazon.awssdk.http.auth.eventstream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.EVENT_STREAMING;
import static software.amazon.awssdk.http.auth.eventstream.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner.getDelegate;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.eventstream.internal.DefaultAwsV4EventStreamHttpSigner;
import software.amazon.awssdk.http.auth.internal.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Test the delegation of signing to the correct signer implementations, specifically for event-stream
 */
public class AwsV4HttpSignerTest {

    BaseAwsV4HttpSigner<?> signer = BaseAwsV4HttpSigner.create();

    @Test
    public void sign_WithEventStreaming_DelegatesToEventStreamSigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest -> signRequest
                .putProperty(EVENT_STREAMING, true)
            )
        );

        AwsV4HttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(DefaultAwsV4EventStreamHttpSigner.class);
    }
}
