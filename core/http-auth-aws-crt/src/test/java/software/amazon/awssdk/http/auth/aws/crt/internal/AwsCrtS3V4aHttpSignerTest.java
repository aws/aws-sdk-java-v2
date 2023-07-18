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

package software.amazon.awssdk.http.auth.aws.crt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner.CHUNKED_ENCODING;
import static software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner.PAYLOAD_SIGNING;
import static software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.crt.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.aws.crt.TestUtils.generateBasicRequest;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.crt.TestUtils;
import software.amazon.awssdk.http.auth.aws.internal.chunkedencoding.AwsSignedChunkedEncodingInputStream;
import software.amazon.awssdk.http.auth.aws.internal.io.AwsChunkedEncodingConfig;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Functional tests for the S3 specific Sigv4a signer. These tests call the CRT native signer code.
 */
public class AwsCrtS3V4aHttpSignerTest {

    private static final DefaultAwsCrtS3V4aHttpSigner signer = new DefaultAwsCrtS3V4aHttpSigner(
        BaseAwsCrtV4aHttpSigner.create(),
        AwsChunkedEncodingConfig.create()
    );

    @Test
    public void sign_withBasicRequest_shouldSign() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(credentials,
            (httpRequest) -> {
                httpRequest.port(443);
            },
            (signRequest) -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("X-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
    }

    @Test
    public void sign_withPayloadSigningEnabled_shouldSignRequestButNotPayload() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(credentials,
            (httpRequest) -> {
                httpRequest.port(443);
            },
            (signRequest) -> {
                signRequest
                    .putProperty(PAYLOAD_SIGNING, true)
                    .putProperty(CHUNKED_ENCODING, false);
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("X-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.payload()).isPresent();
        assertThat(signedRequest.payload().get().newStream()).isNotInstanceOf(AwsSignedChunkedEncodingInputStream.class);
    }

    @Test
    public void sign_withChunkingEnabled_shouldSignRequestButNotPayload() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(credentials,
            (httpRequest) -> {
                httpRequest.port(443);
            },
            (signRequest) -> {
                signRequest
                    .putProperty(PAYLOAD_SIGNING, false)
                    .putProperty(CHUNKED_ENCODING, true);
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("X-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.payload()).isPresent();
        assertThat(signedRequest.payload().get().newStream()).isNotInstanceOf(AwsSignedChunkedEncodingInputStream.class);
    }

    @Test
    public void sign_withPayloadSigningAndChunkingEnabled_shouldSignRequestAndChunkEncode() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(credentials,
            (httpRequest) -> {
                httpRequest.port(443);
            },
            (signRequest) -> {
                signRequest
                    .putProperty(PAYLOAD_SIGNING, true)
                    .putProperty(CHUNKED_ENCODING, true);
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("X-amz-content-sha256")).hasValue(
            "STREAMING-AWS4-ECDSA-P256-SHA256-PAYLOAD");
        assertThat(signedRequest.payload()).isPresent();
        assertThat(signedRequest.payload().get().newStream()).isInstanceOf(AwsSignedChunkedEncodingInputStream.class);
    }

    @Test
    public void sign_withTrailing_shouldSignWithTrailing() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(credentials,
            (httpRequest) -> {
                httpRequest.putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER");
            },
            (signRequest) -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("X-amz-content-sha256")).hasValue(
            "STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(signedRequest.payload()).isPresent();
        assertThat(signedRequest.payload().get().newStream()).isNotInstanceOf(AwsSignedChunkedEncodingInputStream.class);
    }

    @Test
    public void sign_withTrailingAndSignedEncoding_shouldSignWithPayloadTrailing() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");

        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(credentials,
            (httpRequest) -> {
                httpRequest.putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER");
            },
            (signRequest) -> {
                signRequest
                    .putProperty(PAYLOAD_SIGNING, true)
                    .putProperty(CHUNKED_ENCODING, true);
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("X-amz-content-sha256")).hasValue(
            "STREAMING-AWS4-ECDSA-P256-SHA256-PAYLOAD-TRAILER");
        assertThat(signedRequest.payload()).isPresent();
        assertThat(signedRequest.payload().get().newStream()).isInstanceOf(AwsSignedChunkedEncodingInputStream.class);
    }

    @Test
    public void sign_withAnonymousCredentials_shouldNotSign() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            new TestUtils.AnonymousCredentialsIdentity(),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertNull(signedRequest.request().headers().get("Authorization"));
    }

    @Test
    public void sign_withoutRegionNameProperty_throws() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest -> signRequest.putProperty(REGION_NAME, null))
        );

        NullPointerException exception = assertThrows(NullPointerException.class, () -> signer.sign(request));

        assertThat(exception.getMessage()).contains("must not be null");
    }

    @Test
    public void sign_withoutServiceSigningNameProperty_throws() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest -> signRequest.putProperty(SERVICE_SIGNING_NAME, null))
        );

        NullPointerException exception = assertThrows(NullPointerException.class, () -> signer.sign(request));

        assertThat(exception.getMessage()).contains("must not be null");
    }

    @Test
    public void signAsync_throwsUnsupportedOperationException() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        assertThrows(UnsupportedOperationException.class, () -> signer.signAsync(request));
    }
}
