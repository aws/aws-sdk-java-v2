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

package software.amazon.awssdk.s3.wiretap.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

final class SelfSignedCertificate {

    private static final int INTEGER = 0x02;
    private static final int BIT_STRING = 0x03;
    private static final int OCTET_STRING = 0x04;
    private static final int OBJECT_IDENTIFIER = 0x06;
    private static final int UTF8_STRING = 0x0C;
    private static final int UTC_TIME = 0x17;
    private static final int SEQUENCE = 0x30;
    private static final int SET = 0x31;
    private static final int CONTEXT_CONSTRUCTED = 0xA0;
    private static final int DNS_NAME = 0x82;

    private static final int[] ECDSA_WITH_SHA256 = {1, 2, 840, 10045, 4, 3, 2};
    private static final int[] COMMON_NAME = {2, 5, 4, 3};
    private static final int[] SUBJECT_ALT_NAME = {2, 5, 29, 17};

    private static final BigInteger V3 = BigInteger.TWO;
    private static final long NOT_BEFORE_SKEW_HOURS = 1;
    private static final long VALIDITY_DAYS = 3650;
    private static final int P256_LENGTH_BITS = 256;
    private static final DateTimeFormatter UTC_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);

    private static final String[] SUBJECT_ALT_NAMES = {
        "localhost", "*.amazonaws.com", "*.s3.amazonaws.com",
    };

    private static final char[] KEYSTORE_PASSWORD = "wiretap".toCharArray();

    private SelfSignedCertificate() {
    }

    record Generated(X509Certificate certificate, PrivateKey privateKey) {
    }

    static SSLSocketFactory serverSocketFactory() {
        try {
            Generated generated = generate(SUBJECT_ALT_NAMES);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, KEYSTORE_PASSWORD);
            keyStore.setKeyEntry("wiretap", generated.privateKey(), KEYSTORE_PASSWORD,
                    new Certificate[]{generated.certificate()});

            KeyManagerFactory keyManagers =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagers.init(keyStore, KEYSTORE_PASSWORD);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers.getKeyManagers(), null, new SecureRandom());
            return context.getSocketFactory();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Could not build the wiretap's TLS server socket factory", e);
        }
    }

    static Generated generate(final String... dnsNames) {
        try {
            SecureRandom random = new SecureRandom();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), random);
            KeyPair keyPair = generator.generateKeyPair();

            byte[] algorithm = sequence(oid(ECDSA_WITH_SHA256));
            byte[] name = sequence(set(sequence(oid(COMMON_NAME), utf8("S3Wiretap"))));
            Instant now = Instant.now();
            byte[] validity = sequence(
                    utcTime(now.minus(NOT_BEFORE_SKEW_HOURS, ChronoUnit.HOURS)),
                    utcTime(now.plus(VALIDITY_DAYS, ChronoUnit.DAYS)));
            byte[] subjectPublicKeyInfo = keyPair.getPublic().getEncoded();

            byte[][] generalNames = new byte[dnsNames.length][];
            for (int i = 0; i < dnsNames.length; i++) {
                generalNames[i] = tlv(DNS_NAME, dnsNames[i].getBytes(StandardCharsets.US_ASCII));
            }
            byte[] extensions = tlv(CONTEXT_CONSTRUCTED | 3, sequence(
                    sequence(oid(SUBJECT_ALT_NAME), tlv(OCTET_STRING, sequence(generalNames)))));

            byte[] tbsCertificate = sequence(
                    tlv(CONTEXT_CONSTRUCTED, integer(V3)),
                    integer(new BigInteger(P256_LENGTH_BITS, random)),
                    algorithm,
                    name,
                    validity,
                    name,
                    subjectPublicKeyInfo,
                    extensions);

            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate(), random);
            signature.update(tbsCertificate);
            byte[] der = sequence(tbsCertificate, algorithm, bitString(signature.sign()));

            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
            return new Generated(certificate, keyPair.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not mint the wiretap's self-signed certificate", e);
        }
    }

    private static byte[] tlv(final int tag, final byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (value.length < 0x80) {
            out.write(value.length);
        } else {
            byte[] length = BigInteger.valueOf(value.length).toByteArray();
            int offset = length[0] == 0 ? 1 : 0;
            out.write(0x80 | (length.length - offset));
            out.write(length, offset, length.length - offset);
        }
        out.writeBytes(value);
        return out.toByteArray();
    }

    private static byte[] sequence(final byte[]... parts) {
        return tlv(SEQUENCE, concat(parts));
    }

    private static byte[] set(final byte[]... parts) {
        return tlv(SET, concat(parts));
    }

    private static byte[] integer(final BigInteger value) {
        return tlv(INTEGER, value.toByteArray());
    }

    private static byte[] bitString(final byte[] value) {
        return tlv(BIT_STRING, concat(new byte[]{0}, value));
    }

    private static byte[] utf8(final String value) {
        return tlv(UTF8_STRING, value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] utcTime(final Instant value) {
        return tlv(UTC_TIME, UTC_TIME_FORMAT.format(value).getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] oid(final int[] arcs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(arcs[0] * 40 + arcs[1]);
        for (int i = 2; i < arcs.length; i++) {
            int arc = arcs[i];
            int groups = 1;
            while (arc >>> (7 * groups) != 0) {
                groups++;
            }
            for (int group = groups - 1; group >= 0; group--) {
                int septet = (arc >>> (7 * group)) & 0x7F;
                out.write(group == 0 ? septet : septet | 0x80);
            }
        }
        return tlv(OBJECT_IDENTIFIER, out.toByteArray());
    }

    private static byte[] concat(final byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
