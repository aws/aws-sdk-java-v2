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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.auth.aws.TestUtils.TickingClock;
import static software.amazon.awssdk.http.auth.aws.internal.signer.V4RequestSigner.header;
import static software.amazon.awssdk.http.auth.aws.internal.signer.V4RequestSigner.presigned;
import static software.amazon.awssdk.http.auth.aws.internal.signer.V4RequestSigner.query;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;


public class V4RequestSignerTest {

    private static final AwsCredentialsIdentity creds =
        AwsCredentialsIdentity.create("access", "secret");
    private static final AwsSessionCredentialsIdentity sessionCreds =
        AwsSessionCredentialsIdentity.create("access", "secret", "token");

    @Test
    public void sign_computesSigningResult() {
        String expectedSignature = "4c7049543386ba32bc85e7a7d7b892e7da1c412abf3508ca84775ed099790acf";
        String expectedContentHash = "abc123";
        String expectedCanonicalRequestString = "GET\n"
                                                + "/./foo\n\n"
                                                + "x-amz-archive-description:test test\n"
                                                + "x-amz-content-sha256:checksum\n\n"
                                                + "x-amz-archive-description;x-amz-content-sha256\n"
                                                + "abc123";
        V4RequestSigningResult result = V4RequestSigner.create(getProperties(creds), "abc123").sign(getRequest());

        assertEquals(expectedSignature, result.getSignature());
        assertEquals(expectedContentHash, result.getContentHash());
        assertEquals(expectedCanonicalRequestString, result.getCanonicalRequest().getCanonicalRequestString());
    }


    @Test
    public void sign_withHeader_addsAuthHeaders() {
        String expectedAuthorization = "AWS4-HMAC-SHA256 Credential=access/19700101/us-east-1/demo/aws4_request, " +
                                       "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date, " +
                                       "Signature=0fafd04465eb6201e868a80f72d15d50731512298f554684ce6627c0619f429a";
        V4RequestSigningResult result = header(getProperties(creds)).sign(getRequest());

        assertThat(result.getSignedRequest().firstMatchingHeader("X-Amz-Date")).hasValue("19700101T000000Z");
        assertThat(result.getSignedRequest().firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
    }

    @Test
    public void sign_withHeaderAndSessionCredentials_addsAuthHeadersAndTokenHeader() {
        String expectedAuthorization = "AWS4-HMAC-SHA256 Credential=access/19700101/us-east-1/demo/aws4_request, " +
                                       "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;"
                                       + "x-amz-security-token, " +
                                       "Signature=cda79272f6d258c2cb2f04ac84a5f9515440e0158bf39e212c3dcf88b3a477a9";
        V4RequestSigningResult result = header(getProperties(sessionCreds)).sign(getRequest());

        assertThat(result.getSignedRequest().firstMatchingHeader("X-Amz-Date")).hasValue("19700101T000000Z");
        assertThat(result.getSignedRequest().firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
        assertThat(result.getSignedRequest().firstMatchingHeader("X-Amz-Security-Token")).hasValue("token");
    }

    @Test
    public void sign_withHeaderAndSessionCredentials_correctSigningUsingProvidedHostHeader() {
        String expectedAuthorization = "AWS4-HMAC-SHA256 Credential=access/19700101/us-east-1/demo/aws4_request, " +
                                       "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;"
                                       + "x-amz-security-token, " +
                                       "Signature=c8228e7bef8a72a450df38e6e935ce61fdb8989670b41d97cfc20d04bb76b10a";
        SdkHttpRequest.Builder request = getRequest().putHeader(SignerConstant.HOST, "virtual-host.localhost");
        V4RequestSigningResult result = header(getProperties(sessionCreds)).sign(request);

        assertThat(result.getSignedRequest().firstMatchingHeader("Host")).hasValue("virtual-host.localhost");
        assertThat(result.getSignedRequest().firstMatchingHeader("X-Amz-Date")).hasValue("19700101T000000Z");
        assertThat(result.getSignedRequest().firstMatchingHeader("Authorization")).hasValue(expectedAuthorization);
        assertThat(result.getSignedRequest().firstMatchingHeader("X-Amz-Security-Token")).hasValue("token");
    }

    @Test
    public void sign_withQuery_addsAuthQueryParams() {
        V4RequestSigningResult result = query(getProperties(creds)).sign(getRequest());

        assertEquals("AWS4-HMAC-SHA256", result.getSignedRequest().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals("19700101T000000Z", result.getSignedRequest().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals("host;x-amz-archive-description;x-amz-content-sha256",
                     result.getSignedRequest().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals("access/19700101/us-east-1/demo/aws4_request", result.getSignedRequest().rawQueryParameters().get(
            "X-Amz-Credential").get(0));
        assertEquals("bb3ddb98bc32b85c8aa484bfaf321171a22ad802baa03ee9d5fcda9842b769c9",
                     result.getSignedRequest().rawQueryParameters().get("X-Amz-Signature").get(0));
    }

    @Test
    public void sign_withQueryAndSessionCredentials_addsAuthQueryParamsAndTokenParam() {
        V4RequestSigningResult result = query(getProperties(sessionCreds)).sign(getRequest());

        assertEquals("AWS4-HMAC-SHA256", result.getSignedRequest().rawQueryParameters().get("X-Amz-Algorithm").get(0));
        assertEquals("19700101T000000Z", result.getSignedRequest().rawQueryParameters().get("X-Amz-Date").get(0));
        assertEquals("host;x-amz-archive-description;x-amz-content-sha256",
                     result.getSignedRequest().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals(
            "access/19700101/us-east-1/demo/aws4_request",
            result.getSignedRequest().rawQueryParameters().get("X-Amz-Credential").get(0));
        assertEquals("2ffe9562fefd57e14f43bf1937b6b85cc0f0180d63789254bddec25498e14a29",
                     result.getSignedRequest().rawQueryParameters().get("X-Amz-Signature").get(0));
        assertEquals("token", result.getSignedRequest().rawQueryParameters().get("X-Amz-Security-Token").get(0));
    }

    @Test
    public void sign_withPresigned_addsExpirationParam() {
        V4RequestSigningResult result = presigned(getProperties(creds), Duration.ZERO).sign(getRequest());

        assertEquals("0", result.getSignedRequest().rawQueryParameters().get("X-Amz-Expires").get(0));
        assertEquals("host;x-amz-archive-description",
                     result.getSignedRequest().rawQueryParameters().get("X-Amz-SignedHeaders").get(0));
        assertEquals("691f39caa2064fe4fb897976dfb4b09df54749c825a5fcd1e2f0b3fcd1bcc600", result.getSignature());
    }

    @Test
    public void sign_withHeader_withNoContentHashHeader_throws() {
        SdkHttpRequest.Builder request = getRequest().removeHeader("x-amz-content-sha256");

        assertThrows(IllegalArgumentException.class,
                     () -> header(getProperties(sessionCreds)).sign(request)
        );
    }

    @Test
    public void sign_withQuery_withNoContentHashHeader_throws() {
        SdkHttpRequest.Builder request = getRequest().removeHeader("x-amz-content-sha256");

        assertThrows(IllegalArgumentException.class,
                     () -> query(getProperties(sessionCreds)).sign(request)
        );
    }

    private V4Properties getProperties(AwsCredentialsIdentity creds) {
        Clock clock = new TickingClock(Instant.EPOCH);
        return V4Properties.builder()
                           .credentials(creds)
                           .credentialScope(new CredentialScope("us-east-1", "demo", clock.instant()))
                           .signingClock(clock)
                           .doubleUrlEncode(false)
                           .normalizePath(false)
                           .build();
    }

    private SdkHttpRequest.Builder getRequest() {
        URI target = URI.create("https://test.com/./foo");
        return SdkHttpRequest.builder()
                             .method(SdkHttpMethod.GET)
                             .uri(target)
                             .encodedPath(target.getPath())
                             .putHeader("x-amz-content-sha256", "checksum")
                             .putHeader("x-amz-archive-description", "test  test");
    }
}
