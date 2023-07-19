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

package software.amazon.awssdk.http.auth.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicRequest;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.internal.signer.AwsV4HeaderHttpSigner;
import software.amazon.awssdk.http.auth.aws.internal.signer.AwsV4UnsignedPayloadHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

class AwsV4UnsignedPayloadHttpSignerTest {

    private static final String AWS_4_HMAC_SHA_256_AUTHORIZATION =
        "AWS4-HMAC-SHA256 Credential=access/19810216/us-east-1/demo/aws4_request, ";

    private static final BaseAwsV4HttpSigner<?> signer = new AwsV4UnsignedPayloadHttpSigner(
        new AwsV4HeaderHttpSigner(
            BaseAwsV4HttpSigner.create()
        )
    );

    @Test
    public void sign_withBasicRequest_shouldSign() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date, " +
                "Signature=7084d1223538ce77ef27cede34f654844449063db34b4772198f511cdd2410b9";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
                httpRequest.protocol("https");
            }),
            (signRequest -> {
            })
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
    }

    @Test
    public void sign_withoutHttps_shouldSignWithActualContentHash() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date, " +
                "Signature=ccdd78ffbb28b1b1d940fef610a545cb2a03d79bea1fb3e254617b31a2379c25";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue(
            "a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
    }

    @Test
    public void sign_withSessionCredentials_shouldSignAndAddTokenHeader() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;x-amz-security-token, " +
                "Signature=813b8f9fd50bfecbdc6b23391a8664c12d49c188d9c310d02ba7ee3ba5676042";

        // Test request without 'x-amz-sha256' header
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsSessionCredentialsIdentity.create("access", "secret", "token"),
            (httpRequest -> {
                httpRequest.protocol("https");
            }),
            (signRequest -> {
            })
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Security-Token")).hasValue("token");
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
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date, " +
                "Signature=7084d1223538ce77ef27cede34f654844449063db34b4772198f511cdd2410b9";

        // Test request without 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
                httpRequest.protocol("https");
            }),
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
    }

    @Test
    public void signAsync_withoutHttps_shouldSignWithActualContentHash() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date, " +
                "Signature=ccdd78ffbb28b1b1d940fef610a545cb2a03d79bea1fb3e254617b31a2379c25";

        // Test request without 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            (httpRequest -> {
            }),
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue(
            "a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
    }

    @Test
    public void signAsync_withSessionCredentials_shouldSignAndAddTokenHeader() {
        final String expectedAuthorizationHeader =
            AWS_4_HMAC_SHA_256_AUTHORIZATION +
                "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;x-amz-security-token, " +
                "Signature=813b8f9fd50bfecbdc6b23391a8664c12d49c188d9c310d02ba7ee3ba5676042";

        // Test request without 'x-amz-sha256' header
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsSessionCredentialsIdentity.create("access", "secret", "token"),
            (httpRequest -> {
                httpRequest.protocol("https");
            }),
            (signRequest -> {
            })
        );

        AsyncSignedRequest signedRequest = signer.signAsync(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue(request.request().host());
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-archive-description")).hasValue(
            request.request().firstMatchingHeader("x-amz-archive-description").get());
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("19810216T063000Z");
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-content-sha256")).hasValue("UNSIGNED-PAYLOAD");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).hasValue(expectedAuthorizationHeader);
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Security-Token")).hasValue("token");
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
