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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import software.amazon.awssdk.services.signin.internal.DpopIdentity;

/**
 * Utilities and constants used in testing DPoP.
 */
public final class DpopTestUtils {
    public static final String VALID_TEST_PEM =
        "-----BEGIN EC PRIVATE KEY-----\n"
        + "MHcCAQEEICeY73qhQO/3o1QnrL5Nu3HMDB9h3kVW6imRdcHks0tboAoGCCqGSM49"
        + "AwEHoUQDQgAEbefyxjd/UlGwAPF6hy0k4yCW7dSghc6yPd4To0sBqX0tPS/aoLrl"
        + "QnPjfDslgD29p4+Pgwxj1s8cFHVeDKdKTQ==\n"
        + "-----END EC PRIVATE KEY-----";

    public static final DpopIdentity DPOP_IDENTITY = DpopIdentity.create(VALID_TEST_PEM);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DpopTestUtils() {

    }

    public static boolean verifySignature(String encodedDpopHeader) throws Exception {
        String[] parts = encodedDpopHeader.split("\\.");
        assertEquals(3, parts.length, "DPoP header must have 3 JWT parts");

        Map<String, Object> header = getJwtHeaderFromEncodedDpopHeader(encodedDpopHeader);
        assertTrue(header.containsKey("jwk"));

        Map<String, String> jwk = (Map<String, String>) header.get("jwk");
        return verifySignature(jwk, parts[0], parts[1], parts[2]);
    }

    public static Map<String, Object> getJwtHeaderFromEncodedDpopHeader(String encodedDpopHeader) throws Exception {
        String[] parts = encodedDpopHeader.split("\\.");
        assertEquals(3, parts.length, "DPoP header must have 3 JWT parts");

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        return (Map<String, Object>) MAPPER.readValue(headerJson, Map.class);
    }

    public static Map<String, Object> getJwtPayloadFromEncodedDpopHeader(String encodedDpopHeader) throws Exception {
        String[] parts = encodedDpopHeader.split("\\.");
        assertEquals(3, parts.length, "DPoP header must have 3 JWT parts");

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return (Map<String, Object>) MAPPER.readValue(headerJson, Map.class);
    }


    /**
     * Verifies an ES256 signature given base64url-encoded JWT parts and the JWK public key.
     * @return true if the signature is valid.
     */
    public static boolean verifySignature(Map<String, String> jwk, String encodedHeader, String encodedPayload,
                                     String encodedSignature) throws Exception {
        byte[] sigBytes = Base64.getUrlDecoder().decode(encodedSignature);
        byte[] message = (encodedHeader + "." + encodedPayload).getBytes(StandardCharsets.UTF_8);

        // Convert x and y to BigIntegers
        BigInteger x = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("x")));
        BigInteger y = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("y")));
        ECPoint w = new ECPoint(x, y);

        // Use the NIST P-256 curve
        KeyFactory kf = KeyFactory.getInstance("EC");
        ECParameterSpec ecSpec = getECParameterSpec("secp256r1");
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(w, ecSpec);
        ECPublicKey pubKey = (ECPublicKey) kf.generatePublic(pubSpec);

        // Convert JWS (R||S) signature to DER for Java verification
        byte[] derSignature = jwsToDer(sigBytes);

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(pubKey);
        verifier.update(message);
        return verifier.verify(derSignature);
    }

    /**
     * Converts a 64-byte JWS (R||S) ECDSA signature to DER format.
     */
    private static byte[] jwsToDer(byte[] jwsSignature) throws Exception {
        if (jwsSignature.length != 64) {
            throw new IllegalArgumentException("Invalid ES256 signature length");
        }
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(jwsSignature, 0, r, 0, 32);
        System.arraycopy(jwsSignature, 32, s, 0, 32);

        BigInteger R = new BigInteger(1, r);
        BigInteger S = new BigInteger(1, s);

        // ASN.1 encode sequence of two INTEGERs
        byte[] derR = encodeDerInteger(R);
        byte[] derS = encodeDerInteger(S);
        int len = derR.length + derS.length;
        byte[] der = new byte[len + 2];
        der[0] = 0x30;
        der[1] = (byte) len;
        System.arraycopy(derR, 0, der, 2, derR.length);
        System.arraycopy(derS, 0, der, 2 + derR.length, derS.length);
        return der;
    }

    private static byte[] encodeDerInteger(BigInteger val) {
        byte[] raw = val.toByteArray();
        int len = raw.length;
        byte[] out = new byte[len + 2];
        out[0] = 0x02;
        out[1] = (byte) len;
        System.arraycopy(raw, 0, out, 2, len);
        return out;
    }

    private static ECParameterSpec getECParameterSpec(String name) throws Exception {
        java.security.AlgorithmParameters parameters = java.security.AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec(name));
        return parameters.getParameterSpec(ECParameterSpec.class);
    }


}
