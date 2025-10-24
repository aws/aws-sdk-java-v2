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

import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public final class EcKeyLoader {

    private static final String SECP_256_R1_STD_NAME = "secp256r1";

    private static final byte DER_SEQUENCE_TAG = 0x30;
    private static final byte DER_INTEGER_TAG = 0x02;
    private static final byte DER_OCTET_STRING_TAG = 0x04;
    private static final byte DER_BIT_STRING_TAG = 0x03;
    private static final byte DER_OPTIONAL_SEQ_PARAM_0 = (byte) 0xA0;
    private static final byte DER_OPTIONAL_SEQ_PARAM_1 = (byte) 0xA1;
    private static final byte DER_OBJECT_IDENTIFIER_TAG = 0x06;

    private static final int SEC1_VERSION = 1;

    // bytes for "1.2.840.10045.3.1.7" - the OID for secp256r1 aka prime256v1/NIST P-256
    private static byte[] SECP_256_R1_OID_BYTES = new byte[] {0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07};

    private EcKeyLoader() {
    }

    /**
     * Load ECPrivateKey and ECPublicKey from a SEC1 / RFC 5915 ASN.1 formated PEM.
     * <p>
     * The only supported curve is: secp256r1.
     *
     * @param pem EC1 / RFC 5915 ASN.1 formated PEM contents
     * @return The ECPrivateKey and ECPublicKey
     */
    public static Pair<ECPrivateKey, ECPublicKey> loadSec1Pem(String pem) {
        try {
            byte[] sec1Der = pemToDer(pem);
            ParsedEcKey parsed = parseSec1(sec1Der);
            if (parsed.curveOid == null) {
                throw new IllegalArgumentException("Missing EC Curve OID");
            }
            ECParameterSpec params = curveFromOid(parsed.curveOid);

            // Create an ECPrivateKey from the parsed privateScalar value and the EC Curve (EC Parameters)
            ECPrivateKey privateKey = (ECPrivateKey) KeyFactory
                .getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(parsed.privateScalar, params));

            // create an ECPublicKey from the public bytes
            if (parsed.publicBytes == null) {
                throw new IllegalArgumentException("Invalid certificate - public key is required.");
            }
            ECPublicKey publicKey = derivePublicFromBytes(parsed.publicBytes, privateKey.getParams());

            return Pair.of(privateKey, publicKey);
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    // we only support one algorithm/curve: secp256r1, validate that the oid we have matches that and then build the curve
    private static ECParameterSpec curveFromOid(byte[] oid) throws NoSuchAlgorithmException, InvalidParameterSpecException {
        if (Arrays.equals(SECP_256_R1_OID_BYTES, oid)) {
            AlgorithmParameters parameters = null;
            parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec(SECP_256_R1_STD_NAME));
            return parameters.getParameterSpec(ECParameterSpec.class);
        }
        throw new IllegalArgumentException("Unsupported curve OID: " + Arrays.toString(oid));
    }

    // the public key is an octet string of the public X,Y with fixed lengths
    private static ECPublicKey derivePublicFromBytes(byte[] raw, ECParameterSpec params) throws NoSuchAlgorithmException,
                                                                                                InvalidKeySpecException {
        if (raw[0] != DER_OCTET_STRING_TAG) {
            throw new IllegalArgumentException("Expected uncompressed point");
        }
        int len = (raw.length - 1) / 2;
        BigInteger x = new BigInteger(1, java.util.Arrays.copyOfRange(raw, 1, 1 + len));
        BigInteger y = new BigInteger(1, java.util.Arrays.copyOfRange(raw, 1 + len, 1 + 2 * len));
        ECPoint w = new ECPoint(x, y);
        ECPublicKeySpec spec = new ECPublicKeySpec(w, params);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(spec);
    }

    private static class ParsedEcKey {
        BigInteger privateScalar;
        byte[] curveOid;
        byte[] publicBytes;
    }


    /**
     * Follows the SEC1 / RFC 5915 ASN.1 format: PrivateKeyInfo ::= SEQUENCE { version INTEGER (0), privateKeyAlgorithm
     * AlgorithmIdentifier, -- ecPublicKey + curve OID privateKey OCTET STRING -- contains the SEC1 DER parameters [0]
     * ECParameters {{ NamedCurve }} OPTIONAL, publicKey  [1] BIT STRING OPTIONAL }
     * <p>
     * See: <a href="https://datatracker.ietf.org/doc/html/rfc5915#appendix-A">RFC 5915 - ASIN.1 format</a>
     *
     * @param der - asn.1 DER representing an EC private key with public key.
     * @return the parsed EC key, including the public key bytes.
     */
    private static ParsedEcKey parseSec1(byte[] der) {
        ParsedEcKey result = new ParsedEcKey();
        ByteBuffer buffer = ByteBuffer.wrap(der);
        int len;
        try {
            if (buffer.get() != DER_SEQUENCE_TAG) {
                throw new IllegalArgumentException(
                    "Invalid SEC1 Private Key: Not a SEQUENCE");
            }
            readLength(buffer);

            // validate the version
            if (buffer.get() != DER_INTEGER_TAG) {
                throw new IllegalArgumentException(
                    "Invalid SEC1 Private Key: Expected INTEGER");
            }
            len = readLength(buffer);
            if (len != 1 || buffer.get() != SEC1_VERSION) {
                throw new IllegalArgumentException("Invalid SEC1 Private Key: invalid version");
            }

            // read private key
            if (buffer.get() != DER_OCTET_STRING_TAG) {
                throw new IllegalArgumentException(
                    "Invalid SEC1 Private Key: Expected OCTET STRING");
            }
            len = readLength(buffer);

            byte[] privateKeyBytes = new byte[len];
            buffer.get(privateKeyBytes);
            result.privateScalar = new BigInteger(1, privateKeyBytes);

            while (buffer.hasRemaining()) {
                byte tag = buffer.get();
                len = readLength(buffer);
                if (tag == DER_OPTIONAL_SEQ_PARAM_0) { // [0] parameters (curve OID)
                    if (buffer.get() != DER_OBJECT_IDENTIFIER_TAG) {
                        throw new IllegalArgumentException(
                            "Invalid SEC1 Private Key: Expected OID");
                    }
                    int oidLen = readLength(buffer);
                    byte[] oid = new byte[oidLen];
                    buffer.get(oid);
                    result.curveOid = oid;
                } else if (tag == DER_OPTIONAL_SEQ_PARAM_1) { // [1] parameters public key (BIT STRING)
                    byte bitTag = buffer.get();
                    if (bitTag != DER_BIT_STRING_TAG) {
                        throw new IllegalArgumentException(
                            "Invalid SEC1 Private Key: Expected BIT STRING");
                    }
                    int bitLen = readLength(buffer);
                    byte[] bitString = new byte[bitLen];
                    buffer.get(bitString);
                    // First byte of BIT STRING is the unused bits count, skip it
                    result.publicBytes = java.util.Arrays.copyOfRange(bitString, 1, bitString.length);
                } else {
                    // ignore unknown
                    buffer.position(buffer.position() + len);
                }
            }
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Invalid SEC1 Private Key: failed to parse.", e);
        }
        return result;
    }

    // Strip header/footer and base64 decode to return the DER that was encoded in the PEM
    public static byte[] pemToDer(String pem) {
        StringBuilder sb = new StringBuilder();
        for (String line : pem.split("\\r?\\n")) {
            if (line.startsWith("-----")) {
                continue;
            }
            sb.append(line.trim());
        }
        return Base64.getDecoder().decode(sb.toString());
    }

    /**
     * Read a length from a DER byte input stream. lengths may be either a single byte (short form) or multiple bytes.  If the
     * first bit is 0, then the remaining 7 bits give the length directly (short form). If the first bit is 1, then the next 7
     * bits give the number of bytes to read for the length. Eg: [0x82 0x01 0xF4] means the length is 2 bytes long (0x82) and the
     * length is 500 (0x01F4).
     *
     * Throws BufferUnderflowException if there are insufficient bytes
     *
     * @param buffer - byte buffer to read from
     * @return the length
     */
    private static int readLength(ByteBuffer buffer) {
        int b = buffer.get() & 0xFF; // convert signed byte to unsigned int

        // if the high (first) bit is 0, then the length is a single byte, return it as is.
        if ((b & 0x80) == 0) {
            return b;
        }
        // remove the leading 1 bit, this should give the number of bytes for the length
        int num = b & 0x7F;
        if (num == 0) {
            throw new IllegalArgumentException("Indefinite lengths not supported");
        }
        // limit to 4 bytes, supported keys will never have more than 4 bytes of length
        if (num > 4) {
            throw new IllegalArgumentException("Too many bytes in length");
        }
        int val = 0;

        // construct the length by reading num bytes from the input byte stream.
        for (int i = 0; i < num; i++) {
            int nb = buffer.get() & 0xFF;
            if (nb < 0) {
                throw new IllegalArgumentException("Unexpected EOF in length bytes");
            }
            val = (val << 8) | (nb & 0xFF);
        }
        return val;
    }
}