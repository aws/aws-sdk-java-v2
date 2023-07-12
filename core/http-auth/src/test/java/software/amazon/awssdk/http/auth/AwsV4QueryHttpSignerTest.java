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
import static software.amazon.awssdk.http.auth.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.TestUtils.generateBasicRequest;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4QueryHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

class AwsV4QueryHttpSignerTest {

    private static final AwsV4HttpSigner<?> signer = new DefaultAwsV4QueryHttpSigner(
        AwsV4HttpSigner.create()
    );

    @Test
    public void sign_withBasicRequest_shouldSign() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "1f5edd69683e1762cd093ee0e7107f55e23000c26d089079144d19cb11e85347";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";

        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
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
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isNotPresent();
    }

    @Test
    public void sign_withSessionCredentials_shouldSignAndAddTokenParam() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "24f30be5d23a2baeec6086cf51e234a9fa688d7af9e6f276c3bbf638c892a4f5";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
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
        assertEquals(expectedAmzTokenParam, signedRequest.request().rawQueryParameters().get("X-Amz-Security-Token").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isNotPresent();
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
        final String expectedAmzSignature = "1f5edd69683e1762cd093ee0e7107f55e23000c26d089079144d19cb11e85347";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";

        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
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
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isNotPresent();
    }

    @Test
    public void signAsync_withSessionCredentials_shouldSignAndAddTokenParam() {
        final String expectedAmzAlgorithm = "AWS4-HMAC-SHA256";
        final String expectedAmznSignedHeaders = "host;x-amz-archive-description";
        final String expectedAmzSignature = "24f30be5d23a2baeec6086cf51e234a9fa688d7af9e6f276c3bbf638c892a4f5";
        final String expectedAmzCredentials = "access/19810216/us-east-1/demo/aws4_request";
        final String expectedAmzDateHeader = "19810216T063000Z";
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
        assertEquals(expectedAmzTokenParam, signedRequest.request().rawQueryParameters().get("X-Amz-Security-Token").get(0));
        assertEquals(expectedAmzSignature, signedRequest.request().rawQueryParameters().get("X-Amz-Signature").get(0));
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).isNotPresent();
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
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertEquals(signedRequest.request().rawQueryParameters().size(), 0);
    }
}
