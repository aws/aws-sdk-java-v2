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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Two self-signed RSA-2048 certificate/private-key pairs standing in for SNS signing certificates in tests, plus helpers
 * that perform the "SNS service" side of signing: the canonical string per the AWS documentation and the RSA signature
 * over it. Both certificates have CN and SAN {@code sns.amazonaws.com} (the common name the SDK requires for us-east-1)
 * and are valid 2020-01-01 to 2120-01-01.
 * <p>
 * The keys are test-only and were generated with:
 * <pre>
 * openssl req -x509 -newkey rsa:2048 -nodes -sha256 -not_before 20200101000000Z -not_after 21200101000000Z \
 *   -subj "/CN=sns.amazonaws.com" -addext "subjectAltName=DNS:sns.amazonaws.com" -keyout k.pem -out c.pem
 * openssl pkcs8 -topk8 -nocrypt -in k.pem -outform DER | base64   # KEY_PKCS8_B64
 * openssl x509 -in c.pem -outform DER | base64                   # CERT_DER_B64
 * </pre>
 */
final class TestSigningCertificates {
    static final String CERT_A_DER_B64 =
        "MIIDOTCCAiGgAwIBAgIULeVPJWE01CwqICHEOmmngxOqk8AwDQYJKoZIhvcNAQELBQAwHDEaMBgG"
        + "A1UEAwwRc25zLmFtYXpvbmF3cy5jb20wIBcNMjAwMTAxMDAwMDAwWhgPMjEyMDAxMDEwMDAwMDBa"
        + "MBwxGjAYBgNVBAMMEXNucy5hbWF6b25hd3MuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB"
        + "CgKCAQEAo2rHv7r+sdSf7xtDi2CBBI6TPOBp/Totluxr5RCAgA4Yl/zmlLrJNZKpBGOjP/iKsCpY"
        + "UByOOVRnv0bF+5aeorfLo7dl2mdmgS5thn2YdoaIhU0EnL4b+9AcLz0AegCxbn4joIInW2VK9v+Q"
        + "Z32Rw5vdV7OZEE014+e3ABujxgiRhs9F+itIx6FszAZfhCtqeOllZojhC23al7Dnwj/NWMdT+eR2"
        + "Rnvh+wjJ3vqpy5U/DDt80FjZ1ey+bzoM97KajLm5WfrxmFJ/IdbEMWGTco9FiFrXnmByNHnFDUfD"
        + "r1uxkvTIdeFFzzTOmwimPu8O/vWSHOAsMtA95gc3lGNOuwIDAQABo3EwbzAdBgNVHQ4EFgQUKAbZ"
        + "k3loIjaLfyG1uyg4F9LpS+cwHwYDVR0jBBgwFoAUKAbZk3loIjaLfyG1uyg4F9LpS+cwDwYDVR0T"
        + "AQH/BAUwAwEB/zAcBgNVHREEFTATghFzbnMuYW1hem9uYXdzLmNvbTANBgkqhkiG9w0BAQsFAAOC"
        + "AQEAKq+Rv5i2xCqeayHz1eT1DFurz77AVqBlhSSK/QS8ek9AaSqt3zBcpsB7ZmmzYcBI9UMztHVz"
        + "GhzpMZGTdkgGqbzidvK6WZxPbVtX3ntROCBsYVarpk+GZdY9k1TwzcEPtkoK0QN3IaImE7nVR3OQ"
        + "n93617r2S86LOXw2gdwH8pF5Hz1CLxQyoWllrZuxWR90LrTysuFqqz7uhewW//DrK/o5Kdx39/h9"
        + "ks0QYANENvBbx827tjNhWeeamAGIggWKl9N+DkMk7ZuukRwPDfSxU3K8v1RxfEROQQ9wOFWpYZTl"
        + "+yhJj6j4AiuQHa+vJr6HZvj6U0JhXygwqPdAEWvpXg==";

    static final String KEY_A_PKCS8_B64 =
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCjase/uv6x1J/vG0OLYIEEjpM8"
        + "4Gn9Oi2W7GvlEICADhiX/OaUusk1kqkEY6M/+IqwKlhQHI45VGe/RsX7lp6it8ujt2XaZ2aBLm2G"
        + "fZh2hoiFTQScvhv70BwvPQB6ALFufiOggidbZUr2/5BnfZHDm91Xs5kQTTXj57cAG6PGCJGGz0X6"
        + "K0jHoWzMBl+EK2p46WVmiOELbdqXsOfCP81Yx1P55HZGe+H7CMne+qnLlT8MO3zQWNnV7L5vOgz3"
        + "spqMublZ+vGYUn8h1sQxYZNyj0WIWteeYHI0ecUNR8OvW7GS9Mh14UXPNM6bCKY+7w7+9ZIc4Cwy"
        + "0D3mBzeUY067AgMBAAECggEALdjO2p0pSJXpGumQvSEhgThI2aryQW+Zi76flJ/ZFVQfFUEIJdDz"
        + "k1qmnYsgqVle+4adyTgBtNbX1oOSFwAOWINtF+zjn+8jhHmUk8dXx2T+Fzvg3+K8Mh0UUzJvmErq"
        + "fJ/Z+fWZ+hvEeOCBUG+bvf/UDP9foYGv7VU8c8TNYUjiJTxqcpdE6U4PeXUASLF/NfDC7NCgSVIw"
        + "cZ4vfF2v8rBy+77pZufTjIf/sUSlEwePn1kywLf2OTNtqE22rPVW48JzKuk5yHcQE5mj6tC0tVFC"
        + "5YwaURkXOPaTRuTj/TykObAZrlc6SFHzLTUjTS9lLaAy0z/Qkk4EqmwUgzRjUQKBgQDZQm/2RvsU"
        + "2B+8Zb2qPREjuVrUodLVTZs+/fbZYuZLVVn55OwhgsXGh1If+zX+2BSHGijY8ur9LfAfOnN25E0N"
        + "7rdAeB16644+Cm7nm7TMPMQB6afWLDGpYhndkVibe0PseHwzZCF5/oyZFBf8J/JS3d2sqeSfV7ET"
        + "9KE1LxJcywKBgQDAjoUPysc6en0sjZGe8S9tWCuNV5uVUy2F4829/+5e+GMvfOpkUb8NtjQROzyF"
        + "eWv6TFNscdAz5aabIWAeSNydvAfKuaKkXkwaWQJTeBKFpSqkdz9o4Q4ROpthjLy8T3UK2O4BhLb+"
        + "tDOfaxNFQpB2akvtnamUW+H+DsQjchQH0QKBgGQ7JWIuTwnHx6RoH79ZWv3rNn9ghN8JZbUC/448"
        + "9VgN8Wu7dEDljw7PQVEl2TxQPfAyXmPwdf8F4YDWu4w/OVlTJ1Hj2F3sLHxs3cuWZyvOYPMs89R4"
        + "IRUvABXbR1C75pNC5rHi4RriF8VLouEfIMmgN/O/kYz9rOEy++iIZeVPAoGADgGP3jEywqcsObRm"
        + "djg26e4y5MoNkBQHyKhkS8C09wV8OXUNm5RyoC2zpdk4MsE9QGyS2N2CdnPq5XfNBQbpvTNzIaTV"
        + "Fr2hLX1x9N5UApKcAnCGFTvhiRFJQhQ5mjzxV4Ol9EhSAz7aTms8cJpB5csfkL4aGqeATbmHPMjM"
        + "ejECgYEAr6yjK6X8FeIKuRucTnaeZ193ldAc2nck3mfT7WXKuuDZDWAls/Zxk/Vkwz+pqOACV77S"
        + "L/CxTTvb4uNKSTTnT5IH95ZX+6cAEvk1xNdx+KszeNIZ7izrT6GxRcoVVgsvMplu4YjxigYXWzhH"
        + "HIlxTJ5zEeS+u8IZGPgeac1UIRs=";

    static final String CERT_B_DER_B64 =
        "MIIDOTCCAiGgAwIBAgIUSdO98RH8QsGM63v1wpvcgc1SGBgwDQYJKoZIhvcNAQELBQAwHDEaMBgG"
        + "A1UEAwwRc25zLmFtYXpvbmF3cy5jb20wIBcNMjAwMTAxMDAwMDAwWhgPMjEyMDAxMDEwMDAwMDBa"
        + "MBwxGjAYBgNVBAMMEXNucy5hbWF6b25hd3MuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB"
        + "CgKCAQEAyOc5e/YAC73YuuLYO+gV/GluTlosXtn0ZT59lwiRIWSpEJXczaLPSSEUNXN5MkxLRdYA"
        + "Myl35CjGS2kCGsX9ccj/507sjcvlHHXuUjGTlP05iVE6RDiIgk9X7Xf0smgbWUtJqfa/hCjG1E5L"
        + "qSazKEVLWpe1prVAqMcVk1XltyzKvZswE7mo/ApRrjszbM38xTXWBOm59/eksDqVAwhvcnmD5Oog"
        + "xTEHcFlAuHC72pupqBSrOYcdbQGNSmhqV41ttQXyjqX+PZ1/Blcu7OVmMwyX2m5avgbggAKd9O7g"
        + "eBu+ZxCYWd5hYuj6MIbii/AkGRdHurz0beK3x//LAptFowIDAQABo3EwbzAdBgNVHQ4EFgQUFmhf"
        + "2yxA0xaA8IEJL25cIg8+YccwHwYDVR0jBBgwFoAUFmhf2yxA0xaA8IEJL25cIg8+YccwDwYDVR0T"
        + "AQH/BAUwAwEB/zAcBgNVHREEFTATghFzbnMuYW1hem9uYXdzLmNvbTANBgkqhkiG9w0BAQsFAAOC"
        + "AQEAwoWcWuggFHYvMpty9u2b8zV8HuEZOkbGk+4Z7O0WzOlv15kWMhWtoiUcMJ1d50MxOdn6WGfq"
        + "CQkmaIKePqoeuGQ2TYU5MmVp1NpKtJi7uuMsrX2+ntuWQLsVoJxb1fRqMiCZGQEcZDfa+E23D7fb"
        + "7VDgrQVDpCzau55kzrFVnEiV1EI+G4PYx6SeTGId93uKAMXabHO1n2FKZWS4KUtbK3JliqnlPUuW"
        + "g1luvJP0wHZ/w8OTHiPClI09ehxgHf/dMBcN4uOdUjj/IQUFT2LouQ/qwYOA2KH9B9da8CNADVz0"
        + "uJSAAUOyEWrW50Nmq2vpAxKbtDybk2gsbvo6RdNsfQ==";

    static final String KEY_B_PKCS8_B64 =
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDI5zl79gALvdi64tg76BX8aW5O"
        + "Wixe2fRlPn2XCJEhZKkQldzNos9JIRQ1c3kyTEtF1gAzKXfkKMZLaQIaxf1xyP/nTuyNy+Ucde5S"
        + "MZOU/TmJUTpEOIiCT1ftd/SyaBtZS0mp9r+EKMbUTkupJrMoRUtal7WmtUCoxxWTVeW3LMq9mzAT"
        + "uaj8ClGuOzNszfzFNdYE6bn396SwOpUDCG9yeYPk6iDFMQdwWUC4cLvam6moFKs5hx1tAY1KaGpX"
        + "jW21BfKOpf49nX8GVy7s5WYzDJfablq+BuCAAp307uB4G75nEJhZ3mFi6PowhuKL8CQZF0e6vPRt"
        + "4rfH/8sCm0WjAgMBAAECggEAUteFzKfTvQO7qtR/dbRt3HddFNH6GeKl9FmgT/coYSywyjYBponM"
        + "D8ukGaZUA3mSUGm+C1q+JUbq/Y1/YKrjp1fKrgbNZGr/Po0iiKIIyRbh8s2bySXGpHUzPKiKLkbf"
        + "gCKbbYphzo1B76mZaVab6a8pM2H7dZKn5vCw5OA2qSEDfclL+300d+UlSEV5MS96PP3UOVczSJey"
        + "Jjk2SWVlVV0djwwuRaelGLqczywmGs07p2gfGtdLf0zQViK1AEAzHVoc6L9A25EsaP5qhGqRnYYJ"
        + "WAcFd281UYXJP0/7OwoLvUoTNbRGpCC394V3ym3ZEv1YrAesm7BIeMZzSvaMAQKBgQD3RKi20Nfw"
        + "LTVMKW7g2++027j+KdnkG1Aj0g5XNQO0gXcyQ43+rkfG8WF88bbgCC1XZZyUNXYa9Hml3ekKrj6o"
        + "dAJlgOGpjwn0DjBUl0/rp4wWoQtmYE06PfOiqPZIhKzEhC1/A5d0VXK/ec5FNjAxWFTY4raNfIE6"
        + "ifRTID77gwKBgQDP/2tVS0hVoi4E79sCxww/6ZAUt/Dygqt+dmRa4ZydSJe8+j0WP+4m4JS83Ov6"
        + "X4KSBrcUtttyHVvz0xS3FrD0kRGMuTpP4Xv5XedP7M0y7X6e8zTmvVIombY2NXORVDxwfsvneREn"
        + "U7hZtRQEZyB26cYcmsmxVNbqcBKhGPvTYQKBgQCdfZ+wAUjocwL1ZZZ3pd4BZcloKSPdBACBMDNu"
        + "HBgmCifvXdO9GSgicG/Vcuc1vj+S4OdgBsUY2gn5OWRLJzxFcs5r9DDHHJIMXlUuK8n9oJO7fguS"
        + "w1es9vSM7+0dt1OLP1zy/0iiEiKqDlzw/FiptUcldREtANYWgPObVniHHQKBgHPZ6Ve1udFwpr87"
        + "NbqDUbBbkFPp7Du3SnwCx3zTQx9b6qBopRO6ghSHbHERgCKRhjF9qgXQn8Qc1DWnsVcVPXTLRIIk"
        + "PXV2u0223vrmg5jFLYytHB8U/ij9DL9+4CLnT069YSdJiKNl/z2AHWkiY7O4HA+cq5CpF5e22rl+"
        + "J+2hAoGBAIi0J8BdAzp2Gk8+aw/HASi4wcl10iTuoc3kj7kw7tJDKXPy/8YPixfXuhipKL7g2glg"
        + "GJQHCgbqS8+4OD5Z0nZg+69LQ7Wzm4IJFrnhGF8awNyH+K+tiN5BmaekXeJJCUpAG0vkdWVN9keg"
        + "RPsQ4PuldRXHMFa0Z5D1c9qF5VrD";

    private TestSigningCertificates() {
    }

    static PrivateKey privateKey(String pkcs8B64) throws Exception {
        byte[] der = Base64.getDecoder().decode(pkcs8B64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    /** The certificate as the PEM body an SNS certificate URL serves. */
    static byte[] pem(String derB64) {
        StringBuilder sb = new StringBuilder("-----BEGIN CERTIFICATE-----\n");
        for (int i = 0; i < derB64.length(); i += 64) {
            sb.append(derB64, i, Math.min(i + 64, derB64.length())).append('\n');
        }
        sb.append("-----END CERTIFICATE-----\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String sign(String canonical, PrivateKey key, String algorithm) throws Exception {
        Signature signer = Signature.getInstance(algorithm);
        signer.initSign(key);
        signer.update(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    /**
     * The string SNS signs for a Notification: {@code Label\nValue\n} pairs in the order Message, MessageId, Subject (if
     * present), Timestamp, TopicArn, Type. Mirrors {@link SignatureValidator}'s canonical format.
     */
    static String notificationCanonical(String message, String messageId, String subject, String timestamp,
                                        String topicArn) {
        StringBuilder sb = new StringBuilder();
        sb.append("Message\n").append(message).append('\n');
        sb.append("MessageId\n").append(messageId).append('\n');
        if (subject != null) {
            sb.append("Subject\n").append(subject).append('\n');
        }
        sb.append("Timestamp\n").append(timestamp).append('\n');
        sb.append("TopicArn\n").append(topicArn).append('\n');
        sb.append("Type\nNotification\n");
        return sb.toString();
    }

    /** A SignatureVersion 2 (SHA256withRSA) Notification envelope with the given signature and {@code SigningCertURL}. */
    static String notificationJson(String message, String messageId, String subject, String timestamp, String topicArn,
                                   String signatureB64, String signingCertUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"Type\" : \"Notification\",\n");
        sb.append("  \"MessageId\" : \"").append(esc(messageId)).append("\",\n");
        sb.append("  \"TopicArn\" : \"").append(esc(topicArn)).append("\",\n");
        if (subject != null) {
            sb.append("  \"Subject\" : \"").append(esc(subject)).append("\",\n");
        }
        sb.append("  \"Message\" : \"").append(esc(message)).append("\",\n");
        sb.append("  \"Timestamp\" : \"").append(esc(timestamp)).append("\",\n");
        sb.append("  \"SignatureVersion\" : \"2\",\n");
        sb.append("  \"Signature\" : \"").append(signatureB64).append("\",\n");
        sb.append("  \"SigningCertURL\" : \"").append(esc(signingCertUrl)).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
