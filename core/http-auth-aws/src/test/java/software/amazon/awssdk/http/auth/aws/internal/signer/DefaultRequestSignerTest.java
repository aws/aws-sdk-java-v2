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

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.utils.BinaryUtils.toHex;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

public class DefaultRequestSignerTest {

    V4Properties v4Properties = V4Properties.builder()
                                            .credentials(AwsCredentialsIdentity.create("foo", "bar"))
                                            .credentialScope(new CredentialScope("baz", "qux", Instant.EPOCH))
                                            .signingClock(Clock.fixed(Instant.EPOCH, UTC))
                                            .doubleUrlEncode(true)
                                            .normalizePath(true)
                                            .build();

    V4RequestSigner requestSigner = new DefaultV4RequestSigner(v4Properties);

    @Test
    public void requestSigner_sign_shouldReturnSignedContext_butNotAddAnyAuthInfoToRequest() {
        SdkHttpRequest.Builder request = SdkHttpRequest
            .builder()
            .uri(URI.create("https://localhost"))
            .method(SdkHttpMethod.GET)
            .putHeader("x-amz-content-sha256", "quux");
        String expectedContentHash = "quux";
        String expectedSigningKeyHex = "3d558b7a87b67996abc908071e0771a31b2a7977ab247144e60a6cba3356be1f";
        String expectedSignature = "7557839280ea0ef5c4acc66e5670d0857ac4f491884b1b8031d4dea2fc33483c";
        String expectedCanonicalRequestString = "GET\n/\n\n"
                                                + "host:localhost\nx-amz-content-sha256:quux\n\n"
                                                + "host;x-amz-content-sha256\nquux";
        String expectedHost = "localhost";

        V4Context v4Context = requestSigner.sign(request);

        assertEquals(expectedContentHash, v4Context.getContentHash());
        assertEquals(expectedSigningKeyHex, toHex(v4Context.getSigningKey()));
        assertEquals(expectedSignature, v4Context.getSignature());
        assertEquals(expectedCanonicalRequestString, v4Context.getCanonicalRequest().getCanonicalRequestString());
        assertEquals(expectedHost, v4Context.getSignedRequest().firstMatchingHeader("Host").orElse(""));
    }
}
