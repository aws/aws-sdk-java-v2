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

package software.amazon.awssdk.services.signin.auth.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.DPOP_IDENTITY;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.getJwtPayloadFromEncodedDpopHeader;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.verifySignature;

import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.services.signin.internal.DpopSigner;

public class DpopSignerTest {
    @Test
    public void sign_addsDpopHeaderWithValidSignature() throws Exception {
        DpopSigner signer = new DpopSigner();
        SignedRequest signed = signer.sign(
            SignRequest.builder(DPOP_IDENTITY)
                       .request(SdkHttpRequest
                                    .builder()
                                    .uri("https://test-endpoint.com:123/path/is/included?query=not&included=false")
                                    .method(SdkHttpMethod.PUT)
                                    .build()
                       )
                       .build()
        );
        assertNotNull(signed.request().headers().get("DPoP"));
        assertEquals(1, signed.request().headers().get("DPOP").size());
        String dpopHeader = signed.request().headers().get("DPOP").get(0);
        verifySignature(dpopHeader);

        Map<String, Object> payload = getJwtPayloadFromEncodedDpopHeader(dpopHeader);
        assertEquals("PUT", payload.get("htm"));
        assertEquals("https://test-endpoint.com:123/path/is/included", payload.get("htu"));
    }

    @Test
    public void signAsync_addsDpopHeaderWithValidSignature() throws Exception {
        DpopSigner signer = new DpopSigner();
        AsyncSignedRequest signed = signer.signAsync(
            AsyncSignRequest.builder(DPOP_IDENTITY)
                            .request(SdkHttpRequest
                                    .builder()
                                    .uri("https://test-endpoint.com:123/path/is/included?query=not&included=false")
                                    .method(SdkHttpMethod.PUT)
                                    .build()
                       )
                            .build()
        ).join();

        assertNotNull(signed.request().headers().get("DPoP"));
        assertEquals(1, signed.request().headers().get("DPOP").size());
        String dpopHeader = signed.request().headers().get("DPOP").get(0);
        verifySignature(dpopHeader);

        Map<String, Object> payload = getJwtPayloadFromEncodedDpopHeader(dpopHeader);
        assertEquals("PUT", payload.get("htm"));
        assertEquals("https://test-endpoint.com:123/path/is/included", payload.get("htu"));
    }
}
