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

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.signin.internal.EcKeyLoader;
import software.amazon.awssdk.utils.Pair;

public class EcKeyLoaderTest {
    // A valid RFC 5915 secp256r1 DER with both private and public keys
    private static final byte[] VALID_TEST_DER = {
         // header: seq, total length, integer, length of version (1), version (1), integer, length private key (32)
         0x30, 0x77, 0x02, 0x01, 0x01, 0x04, 0x20,
         //  32 bytes of private key (starting at index 7)
         0x06, (byte)0xC8, 0x5E, 0x3A, (byte)0xCF, (byte)0xEF, 0x00, (byte)0xAB,
         0x1E, 0x5B, (byte)0xF5, (byte)0xA3, 0x45, 0x63, (byte)0x91, (byte)0x81,
         (byte)0x80, 0x36, (byte)0xF0, (byte)0x93, (byte)0xF2, (byte)0xCF, 0x58, 0x4C,
         (byte)0x9A, (byte)0x93, 0x5D, (byte)0xDB, 0x27, (byte)0x85, 0x07, (byte)0x8D, // index 39
         // end private key, seq param 0 (index 40), length of param (10)
         (byte)0xA0, 0x0A,
         // object id tag, length of oid (8)
         0x06, 0x08,
         // bytes for "1.2.840.10045.3.1.7" - the OID for secp256r1 aka prime256v1/NIST P-256 (index 44)
         0x2A,  (byte)0x86, 0x48, (byte)0xCE, 0x3D, 0x03, 0x01, 0x07,
         // seq param 1, bit string tag, length (66)
         (byte)0xA1, 0x44, 0x03, 0x42,
         // public key bytes
         0x00, 0x04, (byte)0x9D, (byte) 0xB9, 0x4E, 0x18, 0x1B, 0x25, 0x10, 0x46, 0x1C, 0x18, (byte)0xB5,
         (byte)0xD2, (byte)0x9E, 0x10, 0x75, 0x34, (byte)0xFC, (byte)0xE6, (byte)0xA8, 0x5B, 0x12, (byte)0xBE, 0x77, 0x24,
         (byte)0xC6, (byte)0x93, 0x19, (byte)0xAC, 0x36, (byte)0x91, 0x57, (byte)0xBC, (byte)0xB8, 0x5C, (byte)0xE2,
         (byte)0xE6, (byte)0xA2, 0x2E, (byte)0x82, (byte)0x9D, (byte)0x87, 0x01, 0x4E, (byte)0xD2, (byte)0xAE, 0x4D, 0x17, 0x6A,
         (byte)0x9C, 0x05, 0x1A, (byte)0xEE, (byte)0xDE, (byte)0xA7, 0x2F, (byte)0xA9, (byte)0xE1, (byte)0xCE, 0x20, 0x53, 0x63,
         (byte)0xA9, (byte)0xDC, 0x17
    };

    @Test
    void validKey_returnsValidPair() {
        Pair<ECPrivateKey, ECPublicKey> keys = EcKeyLoader.loadSec1Pem(derToPem(VALID_TEST_DER));

        assertNotNull(keys);
        assertNotNull(keys.left());
        assertNotNull(keys.right());

        ECPrivateKey privateKey = keys.left();
        ECPublicKey publicKey = keys.right();

        // Both keys must use the same curve
        assertEquals(privateKey.getParams().getCurve(), publicKey.getParams().getCurve());

        // Curve should be secp256r1
        assertEquals("secp256r1", privateKey.getParams().toString().contains("secp256r1") ? "secp256r1" : "unknown");
        assertTrue(privateKey.getS().signum() > 0);

        // Public key X/Y must be non-zero
        assertTrue(publicKey.getW().getAffineX().signum() > 0);
        assertTrue(publicKey.getW().getAffineY().signum() > 0);
    }

    @Test
    void invalidKey_badVersion_throwsException() {
        byte[] invalidDER = Arrays.copyOf(VALID_TEST_DER, VALID_TEST_DER.length);
        invalidDER[4] = (byte)0xFF;
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            EcKeyLoader.loadSec1Pem(derToPem(invalidDER));
        });
        assertTrue(e.getMessage().contains("invalid version"));

    }

    @Test
    void invalidKey_unsupportedCurve_throwsException() {
        byte[] invalidDER = Arrays.copyOf(VALID_TEST_DER, VALID_TEST_DER.length);
        invalidDER[44] = (byte)0xFF;
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            EcKeyLoader.loadSec1Pem(derToPem(invalidDER));
        });
        assertTrue(e.getMessage().contains("Unsupported curve"));
    }

    @Test
    void invalidKey_missingCurve_throwsException() {
        byte[] invalidDER = Arrays.copyOf(VALID_TEST_DER, 39);

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            EcKeyLoader.loadSec1Pem(derToPem(invalidDER));
        });
        assertTrue(e.getMessage().contains("Missing EC Curve OID"));
    }

    @Test
    void invalidKey_missingPublicKey_throwsException() {
        byte[] invalidDER = Arrays.copyOf(VALID_TEST_DER, 51);

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            EcKeyLoader.loadSec1Pem(derToPem(invalidDER));
        });
        assertTrue(e.getMessage().contains("public key is required."));
    }

    private String derToPem(byte[] der) {
        return Base64.getEncoder().encodeToString(der);
    }

}
