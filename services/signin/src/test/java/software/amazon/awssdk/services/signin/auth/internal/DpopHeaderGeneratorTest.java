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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.DPOP_IDENTITY;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.getJwtHeaderFromEncodedDpopHeader;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.getJwtPayloadFromEncodedDpopHeader;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.verifySignature;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.services.signin.internal.DpopHeaderGenerator;

public class DpopHeaderGeneratorTest {

    @Test
    public void testGenerateAndVerifyDPoPHeader() throws Exception {
        String endpoint = "https://testing.amazon.com/v1/token";
        String httpMethod = "POST";
        long epochSeconds = Instant.now().getEpochSecond();
        String uuid = UUID.randomUUID().toString();

        // Generate the DPoP proof JWT
        String dpop = DpopHeaderGenerator.generateDPoPProofHeader(DPOP_IDENTITY, endpoint, httpMethod, epochSeconds, uuid);
        assertNotNull(dpop, "DPoP header should not be null");

        // JWT should be in form header.payload.signature
        String[] parts = dpop.split("\\.");
        assertEquals(3, parts.length, "DPoP header must have 3 JWT parts");

        // Decode and parse header
        Map<String, Object> header = getJwtHeaderFromEncodedDpopHeader(dpop);
        assertEquals("ES256", header.get("alg"));
        assertEquals("dpop+jwt", header.get("typ"));
        assertTrue(header.containsKey("jwk"));

        Map<String, String> jwk = (Map<String, String>) header.get("jwk");
        assertEquals("EC", jwk.get("kty"));
        assertEquals("P-256", jwk.get("crv"));
        assertNotNull(jwk.get("x"));
        assertNotNull(jwk.get("y"));

        // Decode and parse payload
        Map<String, Object> payload = getJwtPayloadFromEncodedDpopHeader(dpop);
        assertEquals(uuid, payload.get("jti"));
        assertEquals(httpMethod, payload.get("htm"));
        assertEquals(endpoint, payload.get("htu"));
        assertEquals(((Number) payload.get("iat")).longValue(), epochSeconds);

        // Verify the ES256 signature using the public key from JWK
        boolean verified = verifySignature(jwk, parts[0], parts[1], parts[2]);
        assertTrue(verified, "DPoP ES256 signature should verify correctly");
    }

    @Test
    public void missingArguments_raisesException() {
        assertThrows(NullPointerException.class, () -> {
            DpopHeaderGenerator.generateDPoPProofHeader(
                null, "https://example.com", "POST",
                Instant.now().getEpochSecond(), UUID.randomUUID().toString());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DpopHeaderGenerator.generateDPoPProofHeader(
                DPOP_IDENTITY, "", "POST",
                Instant.now().getEpochSecond(), UUID.randomUUID().toString());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DpopHeaderGenerator.generateDPoPProofHeader(
                DPOP_IDENTITY, "https://example.com", "",
                Instant.now().getEpochSecond(), UUID.randomUUID().toString());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DpopHeaderGenerator.generateDPoPProofHeader(
                DPOP_IDENTITY, "https://example.com", "POST",
                Instant.now().getEpochSecond(), "");
        });
    }


}
