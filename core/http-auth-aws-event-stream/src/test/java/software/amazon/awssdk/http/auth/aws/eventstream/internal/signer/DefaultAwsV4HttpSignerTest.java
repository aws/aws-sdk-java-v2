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

package software.amazon.awssdk.http.auth.aws.eventstream.internal.signer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.eventstream.TestUtils.generateBasicAsyncRequest;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.SigV4DataFramePublisher;
import software.amazon.awssdk.http.auth.aws.internal.signer.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Test the delegation of signing to the correct implementations.
 */
public class DefaultAwsV4HttpSignerTest {

    DefaultAwsV4HttpSigner signer = new DefaultAwsV4HttpSigner();

    @Test
    public void sign_WithEventStreamContentType_DelegatesToEventStreamPayloadSigner() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> {
            }
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request).join();

        assertThat(signedRequest.payload().get()).isInstanceOf(SigV4DataFramePublisher.class);
    }

    @Test
    public void sign_WithEventStreamContentTypeAndUnsignedPayload_Throws() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> httpRequest
                .putHeader("Content-Type", "application/vnd.amazon.eventstream"),
            signRequest -> {
                signRequest.putProperty(PAYLOAD_SIGNING_ENABLED, false);
            }
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.signAsync(request));
    }
}
