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

package software.amazon.awssdk.http.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.AwsV4QueryHttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4PresignedHttpSigner;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4QueryHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

class AwsV4PresignedHttpSignerTest {

    private static final AwsV4HttpSigner<?> signer = new DefaultAwsV4PresignedHttpSigner(
        new DefaultAwsV4QueryHttpSigner(
            AwsV4HttpSigner.create()
        )
    );

    @Test
    public void sign_withBasicRequest_shouldSign() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "bf7ae1c2f266d347e290a2aee7b126d38b8a695149d003b9fab2ed1eb6d6ebda";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
        final String expectedAmzExpires = "604800";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest ->
                signRequest.putProperty(SignerProperty.create(String.class, "AuthLocation"), "Query")
            )
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertEquals(expectedAmzAlgorithm, signedRequest.request().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals(expectedAmznSignedHeaders, signedRequest.request().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals(expectedAmzCredentials, signedRequest.request().rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals(expectedAmzDateHeader, signedRequest.request().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals(expectedAmzExpires, signedRequest.request().rawQueryParameters().get("X-Amz-Expires").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
    }

    @Test
    public void sign_withValidExpiration_shouldSign() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "9cd8fc0bab549a5071ad9de1f1dc11b216e51c2262167911902799c98a9af40a";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
        final String expectedAmzExpires = "600";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest ->
                signRequest
                    .putProperty(EXPIRATION_DURATION, Duration.ofSeconds(600))
            )
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertEquals(expectedAmzAlgorithm, signedRequest.request().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals(expectedAmznSignedHeaders, signedRequest.request().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals(expectedAmzCredentials, signedRequest.request().rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals(expectedAmzDateHeader, signedRequest.request().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals(expectedAmzExpires, signedRequest.request().rawQueryParameters().get("X-Amz-Expires").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
    }

    @Test
    public void sign_withInvalidExpiration_shouldThrow() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest ->
                signRequest
                    .putProperty(EXPIRATION_DURATION,
                        PRESIGN_URL_MAX_EXPIRATION_DURATION.plus(Duration.ofSeconds(1)))
            )
        );

        RuntimeException exception = assertThrows(IllegalArgumentException.class, () -> signer.sign(request));

        assertThat(exception.getMessage()).contains("valid for at most 7 days");
    }

    @Test
    public void sign_withSessionCredentials_shouldSignAndAddTokenParam() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "909d8bc528fec51c0cc6daaa6c29291c519de10f77490d8af57872c29203ebdb";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
        final String expectedAmzExpires = "604800";
        final String expectedAmzTokenParam = "token";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsSessionCredentialsIdentity.create("access", "secret", "token"),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertEquals(expectedAmzAlgorithm, signedRequest.request().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals(expectedAmznSignedHeaders, signedRequest.request().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals(expectedAmzCredentials, signedRequest.request().rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals(expectedAmzDateHeader, signedRequest.request().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals(expectedAmzExpires, signedRequest.request().rawQueryParameters().get("X-Amz-Expires").get(0));
        assertEquals(expectedAmzTokenParam, signedRequest.request().rawQueryParameters().get("X-Amz-Security-Token").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature.
     */
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

        assertEquals(signedRequest.request().rawQueryParameters().size(), 0);
    }

    @Test
    public void signAsync_withBasicRequest_shouldSign() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "bf7ae1c2f266d347e290a2aee7b126d38b8a695149d003b9fab2ed1eb6d6ebda";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
        final String expectedAmzExpires = "604800";

        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest ->
                signRequest.putProperty(SignerProperty.create(String.class, "AuthLocation"), "Query")
            )
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertEquals(expectedAmzAlgorithm, signedRequest.request().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals(expectedAmznSignedHeaders, signedRequest.request().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals(expectedAmzCredentials, signedRequest.request().rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals(expectedAmzDateHeader, signedRequest.request().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals(expectedAmzExpires, signedRequest.request().rawQueryParameters().get("X-Amz-Expires").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
    }

    @Test
    public void signAsync_withValidExpiration_shouldSign() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "9cd8fc0bab549a5071ad9de1f1dc11b216e51c2262167911902799c98a9af40a";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
        final String expectedAmzExpires = "600";

        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest ->
                signRequest
                    .putProperty(EXPIRATION_DURATION, Duration.ofSeconds(600))
            )
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertEquals(expectedAmzAlgorithm, signedRequest.request().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals(expectedAmznSignedHeaders, signedRequest.request().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals(expectedAmzCredentials, signedRequest.request().rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals(expectedAmzDateHeader, signedRequest.request().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals(expectedAmzExpires, signedRequest.request().rawQueryParameters().get("X-Amz-Expires").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
    }

    @Test
    public void signAsync_withInvalidExpiration_shouldThrow() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest -> signRequest
                .putProperty(EXPIRATION_DURATION,
                    PRESIGN_URL_MAX_EXPIRATION_DURATION.plus(Duration.ofSeconds(1)))
            )
        );

        RuntimeException exception = assertThrows(IllegalArgumentException.class, () -> signer.signAsync(request));

        assertThat(exception.getMessage()).contains("valid for at most 7 days");
    }

    @Test
    public void signAsync_withSessionCredentials_shouldSignAndAddTokenParam() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "909d8bc528fec51c0cc6daaa6c29291c519de10f77490d8af57872c29203ebdb";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
        final String expectedAmzExpires = "604800";
        final String expectedAmzTokenParam = "token";

        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsSessionCredentialsIdentity.create("access", "secret", "token"),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertEquals(expectedAmzAlgorithm, signedRequest.request().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals(expectedAmznSignedHeaders, signedRequest.request().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals(expectedAmzCredentials, signedRequest.request().rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals(expectedAmzDateHeader, signedRequest.request().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals(expectedAmzExpires, signedRequest.request().rawQueryParameters().get("X-Amz-Expires").get(0));
        assertEquals(expectedAmzTokenParam, signedRequest.request().rawQueryParameters().get("X-Amz-Security-Token").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature.
     */
    @Test
    public void signAsync_withAnonymousCredentials_shouldNotSign() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            new TestUtils.AnonymousCredentialsIdentity(),
            (httpRequest -> {
            }),
            (signRequest ->
                signRequest.putProperty(AUTH_LOCATION, "Query")
            )
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertEquals(signedRequest.request().rawQueryParameters().size(), 0);
    }
}
