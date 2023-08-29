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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner.AuthLocation;
import static software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner.PAYLOAD_SIGNING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.crt.TestUtils.generateBasicRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.toCredentials;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;


/**
 * Functional tests for the Sigv4a signer. These tests call the CRT native signer code.
 */
public class DefaultAwsCrtV4aHttpSignerTest {

    DefaultAwsCrtV4aHttpSigner signer = new DefaultAwsCrtV4aHttpSigner();

    @Test
    public void sign_withBasicRequest_shouldSignWithHeaders() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(credentials,
                                                                               httpRequest ->
                                                                                   httpRequest.port(443),
                                                                               signRequest -> {
                                                                               }
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue("demo.us-east-1.amazonaws.com");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();

    }

    @Test
    public void sign_withQuery_shouldSignWithQueryParams() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> httpRequest.port(443),
            signRequest ->
                signRequest.putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Algorithm"))
            .hasValue("AWS4-ECDSA-P256-SHA256");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Credential"))
            .hasValue("AKIDEXAMPLE/20200803/demo/aws4_request");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-SignedHeaders"))
            .hasValue("host;x-amz-archive-description");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature")).isPresent();
    }

    @Test
    public void sign_withQueryAndExpiration_shouldSignWithQueryParamsAndExpire() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> httpRequest.port(443),
            signRequest -> signRequest
                .putProperty(AUTH_LOCATION, AuthLocation.QUERY_STRING)
                .putProperty(EXPIRATION_DURATION, Duration.ofSeconds(1))
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);
        expectedSigningConfig.setExpirationInSeconds(1);

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Algorithm"))
            .hasValue("AWS4-ECDSA-P256-SHA256");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Credential"))
            .hasValue("AKIDEXAMPLE/20200803/demo/aws4_request");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-SignedHeaders"))
            .hasValue("host;x-amz-archive-description");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Signature")).isPresent();
        assertThat(signedRequest.request().firstMatchingRawQueryParameter("X-Amz-Expires")).hasValue("1");
    }

    @Test
    public void sign_withUnsignedPayload_shouldNotSignPayload() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SyncSignRequest<AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> {
            },
            signRequest -> signRequest
                .putProperty(PAYLOAD_SIGNING_ENABLED, false)
        );

        AwsSigningConfig expectedSigningConfig = new AwsSigningConfig();
        expectedSigningConfig.setCredentials(toCredentials(request.identity()));
        expectedSigningConfig.setService("demo");
        expectedSigningConfig.setRegion("aws-global");
        expectedSigningConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        expectedSigningConfig.setTime(1596476903000L);
        expectedSigningConfig.setUseDoubleUriEncode(true);
        expectedSigningConfig.setShouldNormalizeUriPath(true);
        expectedSigningConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
        expectedSigningConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);

        SyncSignedRequest signedRequest = signer.sign(request);

        assertThat(signedRequest.request().firstMatchingHeader("Host")).hasValue("demo.us-east-1.amazonaws.com");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Date")).hasValue("20200803T174823Z");
        assertThat(signedRequest.request().firstMatchingHeader("X-Amz-Region-Set")).hasValue("aws-global");
        assertThat(signedRequest.request().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void sign_withAnonymousCredentials_shouldNotSign() {
        AwsCredentialsIdentity credentials = new TestUtils.AnonymousCredentialsIdentity();
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            credentials,
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        SyncSignedRequest signedRequest = signer.sign(request);

        assertNull(signedRequest.request().headers().get("Authorization"));
    }

    @Test
    public void signAsync_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> signer.signAsync((AsyncSignRequest) null));
    }
}
