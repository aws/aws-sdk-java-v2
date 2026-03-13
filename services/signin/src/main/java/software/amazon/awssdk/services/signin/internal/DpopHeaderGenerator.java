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

package software.amazon.awssdk.services.signin.internal;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Arrays;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.utils.Validate;

/**
 * Utilities that implement rfc9449 - OAuth 2.0 Demonstrating Proof of Possession (DPoP)
*/
@SdkInternalApi
public final class DpopHeaderGenerator {

    private static final int ES256_SIGNATURE_BYTE_LENGTH = 64;
    private static final byte DER_SEQUENCE_TAG = 0x30;

    private DpopHeaderGenerator() {

    }

    /**
     * Construct a rfc9449 - OAuth 2.0 Demonstrating Proof of Possession (DPoP) header.
     *
     * The DPoP HTTP header must be a signed JWT (RFC 7519: JSON Web Token), which includes a
     * JWK (RFC 7517: JSON Web Key).
     *
     * For reference, see:
     * <ul>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc9449">RFC 9449 -
     * OAuth 2.0 Demonstrating Proof of Possession (DPoP)</a></li>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 - JSON Web Token (JWT)</a></li>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc7517">RFC 7517 - JSON Web Key (JWK)</a></li>
     * </ul>
     *
     * @param dpopIdentity - DpopIdentity containing ECPrivateKey and ECPublicKey
     * @param endpoint - The HTTP target URI (Section 7.1 of [RFC9110]) of the request to which the JWT is attached,
     *                 without query and fragment parts
     * @param httpMethod - the HTTP method of the request (eg: POST).
     * @param epochSeconds - creation time of the JWT in epoch seconds.
     * @param uuid - Unique identifier for the DPoP proof JWT - should be a UUID4 string.
     * @return DPoP header value
     */
    public static String generateDPoPProofHeader(DpopIdentity dpopIdentity, String endpoint, String httpMethod,
                                                 long epochSeconds, String uuid) {
        Validate.paramNotNull(dpopIdentity, "dpopIdentity");
        Validate.paramNotBlank(endpoint, "endpoint");
        Validate.paramNotBlank(httpMethod, "httpMethod");
        Validate.paramNotBlank(uuid, "uuid");

        try {
            // Load EC public and private key from PEM
            ECPrivateKey privateKey = dpopIdentity.getPrivateKey();
            ECPublicKey publicKey = dpopIdentity.getPublicKey();

            // Build JSON strings (header, payload) with JsonGenerator
            byte[] headerJson = buildHeaderJson(publicKey);
            byte[] payloadJson = buildPayloadJson(uuid, endpoint, httpMethod, epochSeconds);

            // Base64URL encode header + payload
            String encodedHeader = base64UrlEncode(headerJson);
            String encodedPayload = base64UrlEncode(payloadJson);
            String message = encodedHeader + "." + encodedPayload;

            // Sign (ES256)
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = translateDerSignatureToJws(signature.sign(), ES256_SIGNATURE_BYTE_LENGTH);

            // Combine into JWT
            String encodedSignature = base64UrlEncode(signatureBytes);
            return message + "." + encodedSignature;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    // build the JWT header which includes the public key
    // see: https://datatracker.ietf.org/doc/html/rfc9449#name-dpop-proof-jwt-syntax
    private static byte[] buildHeaderJson(ECPublicKey publicKey) {
        ECPoint pubPoint = publicKey.getW();
        String x = base64UrlEncode(stripLeadingZero(pubPoint.getAffineX().toByteArray()));
        String y = base64UrlEncode(stripLeadingZero(pubPoint.getAffineY().toByteArray()));
        JsonWriter jsonWriter = null;
        try {
            jsonWriter = JsonWriter.create();
            jsonWriter.writeStartObject();
            jsonWriter.writeFieldName("typ");
            jsonWriter.writeValue("dpop+jwt");

            jsonWriter.writeFieldName("alg");
            jsonWriter.writeValue("ES256");

            jsonWriter.writeFieldName("jwk");
            jsonWriter.writeStartObject();

            jsonWriter.writeFieldName("crv") ;
            jsonWriter.writeValue("P-256");

            jsonWriter.writeFieldName("kty");
            jsonWriter.writeValue("EC");

            jsonWriter.writeFieldName("x");
            jsonWriter.writeValue(x);

            jsonWriter.writeFieldName("y");
            jsonWriter.writeValue(y);
            jsonWriter.writeEndObject(); // end jwk
            jsonWriter.writeEndObject(); // end root

            return jsonWriter.getBytes();
        } finally {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
        }
    }

    // build claims payload
    // see: https://datatracker.ietf.org/doc/html/rfc9449#name-dpop-proof-jwt-syntax
    private static byte[] buildPayloadJson(String uuid, String endpoint, String httpMethod, long epochSeconds) {
        JsonWriter jsonWriter = null;
        try {
            jsonWriter = JsonWriter.create();
            jsonWriter.writeStartObject();

            jsonWriter.writeFieldName("jti");
            jsonWriter.writeValue(uuid);

            jsonWriter.writeFieldName("htm");
            jsonWriter.writeValue(httpMethod);

            jsonWriter.writeFieldName("htu");
            jsonWriter.writeValue(endpoint);

            jsonWriter.writeFieldName("iat");
            jsonWriter.writeValue(epochSeconds);

            jsonWriter.writeEndObject();

            return jsonWriter.getBytes();
        } finally {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
        }
    }

    /**
     * Java Signature from SHA256withECDSA produces an ASN.1/DER encoded signature.
     * This method translates that signature into the concatenated (R,S) format expected by JWS.
     *
     * An ECDSA signature always produces two big integers: R and S. The DER format encodes these in a variable
     * length sequence because the values are encoded without leading zeroes with a structure following:
     * [ SEQUENCE_TAG, total_length, INTEGER_TAG, length of R, (bytes of R), INTEGER_TAG, length OF S, ( bytes of S) ]
     *
     * The JWT/JOSE spec defines ECDSA signatures as two 32 byte, big-endian integer values for R and S (total of 64 bytes):
     * [ 32 bytes of R, 32 bytes of S]
     *
     * @param derSignature The ASN1/DER-encoded signature.
     * @param outputLength The expected length of the ECDSA JWS signature.  This should be 64 for ES256
     *
     * @return The ECDSA JWS encoded signature (concatenated r,s values)
     **/
    private static byte[] translateDerSignatureToJws(byte[] derSignature, int outputLength) {

        // validate DER signature format
        if (derSignature.length < 8 || derSignature[0] != DER_SEQUENCE_TAG) {
            throw new RuntimeException("Invalid ECDSA signature format");
        }

        // the total length may be more than 1 byte
        // if the first byte is (0x81), its 2 bytes
        int offset; // point to the start of the first INTEGER_TAG
        if (derSignature[1] > 0) {
            offset = 2;
        } else if (derSignature[1] == (byte) 0x81) {
            offset = 3;
        } else {
            throw new RuntimeException("Invalid ECDSA signature format");
        }

        // get the length of R as the byte after the first INTEGER_TAG
        byte rLength = derSignature[offset + 1];

        // determine the number of significant (non-zero) bytes in R
        int i;
        int endOfR = offset + 2 + rLength;
        for (i = rLength; (i > 0) && (derSignature[endOfR - i] == 0); i--) {
            // do nothing
        }

        // get the length of S as the byte after the second INTEGER_TAG which is:
        byte sLength = derSignature[endOfR + 1];

        // determine number of significant bytes in S
        int j;
        int endOfS = endOfR + 2 + sLength;
        for (j = sLength; (j > 0) && (derSignature[endOfS - j] == 0); j--) {
            // do nothing
        }

        int rawLen = Math.max(i, j);
        rawLen = Math.max(rawLen, outputLength / 2);

        // sanity check, ensure the internal structure matches the DER spec.
        if ((derSignature[offset - 1] & 0xff) != derSignature.length - offset
            || (derSignature[offset - 1] & 0xff) != 2 + rLength + 2 + sLength
            || derSignature[offset] != 2
            || derSignature[endOfR] != 2) {
            throw new RuntimeException("Invalid ECDSA signature format");
        }

        byte[] jwsSignature = new byte[2 * rawLen];
        // copy the significant bytes of R (i bytes), removing any leading zeros, into the first half of output array.
        // Right aligned!
        System.arraycopy(derSignature, endOfR - i, jwsSignature, rawLen - i, i);
        // do the same for S to the second half of the output array. Also right aligned.
        System.arraycopy(derSignature, (offset + 2 + rLength + 2 + sLength) - j, jwsSignature, 2 * rawLen - j, j);

        return jwsSignature;
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] stripLeadingZero(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0x00) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }
}
