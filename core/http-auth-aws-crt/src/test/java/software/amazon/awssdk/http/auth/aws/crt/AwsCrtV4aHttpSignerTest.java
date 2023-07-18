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

package software.amazon.awssdk.http.auth.aws.crt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner.CHUNKED_ENCODING;
import static software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner.PAYLOAD_SIGNING;
import static software.amazon.awssdk.http.auth.aws.crt.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.DefaultAwsCrtV4aHttpSigner.getDelegate;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.crt.internal.BaseAwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.crt.internal.DefaultAwsCrtS3V4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Test the delegation of signing to the correct signer implementations.
 */
public class AwsCrtV4aHttpSignerTest {

    BaseAwsCrtV4aHttpSigner<?> signer = BaseAwsCrtV4aHttpSigner.create();

    @Test
    public void sign_WithNoAdditonalProperties_DelegatesToBaseSigner() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> {})
        );

        AwsCrtV4aHttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(BaseAwsCrtV4aHttpSigner.class);
    }

    @Test
    public void sign_WithPayloadSigning_DelegatesToS3Signer() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> signRequest.putProperty(PAYLOAD_SIGNING, true))
        );

        AwsCrtV4aHttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(DefaultAwsCrtS3V4aHttpSigner.class);
    }

    @Test
    public void sign_WithChunkedEncoding_DelegatesToS3Signer() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> signRequest.putProperty(CHUNKED_ENCODING, true))
        );

        AwsCrtV4aHttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(DefaultAwsCrtS3V4aHttpSigner.class);
    }

    @Test
    public void sign_WithPayloaSigningAndChunkedEncoding_DelegatesToS3Signer() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {}),
            (signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING, true)
                .putProperty(CHUNKED_ENCODING, true))
        );

        AwsCrtV4aHttpSigner delegate = getDelegate(signer, request);

        assertThat(delegate.getClass()).isEqualTo(DefaultAwsCrtS3V4aHttpSigner.class);
    }
}
