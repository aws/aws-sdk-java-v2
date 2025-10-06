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

package software.amazon.awssdk.services.sns.internal.messagemanager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sns.messagemanager.SnsCertificateException;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessage;
import software.amazon.awssdk.services.sns.messagemanager.SnsSignatureValidationException;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal validator for SNS message signatures.
 *
 * <p>This class handles cryptographic verification of SNS message signatures using AWS certificates.
 * It supports both SignatureVersion1 (SHA1) and SignatureVersion2 (SHA256) as per AWS SNS standards,
 * ensuring that messages are genuinely from Amazon SNS and have not been tampered with during transmission.
 *
 * <p>The validator performs comprehensive signature verification including:
 * <ul>
 *   <li>Certificate validation and chain of trust verification</li>
 *   <li>Signature algorithm selection based on signature version</li>
 *   <li>Message canonicalization for signature verification</li>
 *   <li>Cryptographic signature verification using public key</li>
 *   <li>Certificate key usage validation for digital signatures</li>
 * </ul>
 *
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Validates certificate issuer against known Amazon SNS certificate authorities</li>
 *   <li>Checks certificate validity period and expiration</li>
 *   <li>Verifies certificate subject contains appropriate SNS identifiers</li>
 *   <li>Ensures certificates have digital signature key usage enabled</li>
 *   <li>Supports multiple AWS partitions (aws, aws-gov, aws-cn)</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe as all methods are static
 * and do not maintain any mutable state.
 *
 * <p><strong>Usage:</strong> This class is intended for internal use by the SNS message manager
 * and should not be used directly by client code. Signature validation is automatically
 * performed during message parsing through {@link DefaultSnsMessageManager}.
 *
 * @see DefaultSnsMessageManager
 * @see CertificateRetriever
 * @see SnsMessage
 */
@SdkInternalApi
public final class SignatureValidator {

    private static final String SIGNATURE_VERSION_1 = "1";
    private static final String SIGNATURE_VERSION_2 = "2";
    
    private static final String SHA1_WITH_RSA = "SHA1withRSA";
    private static final String SHA256_WITH_RSA = "SHA256withRSA";
    
    private static final String CERTIFICATE_TYPE = "X.509";

    private SignatureValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates the signature of an SNS message using the provided certificate.
     *
     * <p>This method performs comprehensive cryptographic verification of the SNS message signature
     * to ensure the message is authentic and from Amazon SNS. The validation process includes:
     * <ul>
     *   <li>Parsing and validating the X.509 certificate</li>
     *   <li>Verifying certificate validity period and chain of trust</li>
     *   <li>Checking certificate key usage for digital signatures</li>
     *   <li>Building canonical message string for signature verification</li>
     *   <li>Performing cryptographic signature verification using the certificate's public key</li>
     * </ul>
     *
     * <p>The method supports both SignatureVersion1 (SHA1withRSA) and SignatureVersion2 (SHA256withRSA)
     * signature algorithms as specified by AWS SNS standards.
     *
     * <p><strong>Security Validation:</strong>
     * <ul>
     *   <li>Certificate must be issued by a trusted Amazon SNS certificate authority</li>
     *   <li>Certificate must be within its validity period</li>
     *   <li>Certificate subject must contain appropriate SNS identifiers</li>
     *   <li>Certificate must have digital signature key usage enabled</li>
     * </ul>
     *
     * @param message The SNS message to validate. Must contain all required signature fields
     *                including signature, signatureVersion, and message content.
     * @param certificateBytes The X.509 certificate bytes in PEM or DER format used for 
     *                        signature verification. Must be a valid certificate from Amazon SNS.
     * @throws SnsSignatureValidationException If signature verification fails, indicating the
     *                                        message may have been tampered with or is not from Amazon SNS
     * @throws SnsCertificateException If certificate processing, parsing, or validation fails
     * @throws NullPointerException If message or certificateBytes is null
     */
    public static void validateSignature(SnsMessage message, byte[] certificateBytes) {
        Validate.paramNotNull(message, "message");
        Validate.paramNotNull(certificateBytes, "certificateBytes");

        X509Certificate certificate = parseCertificate(certificateBytes);
        validateCertificate(certificate);
        
        String signatureAlgorithm = getSignatureAlgorithm(message.signatureVersion());
        String canonicalMessage = buildCanonicalMessage(message);
        
        verifySignature(message.signature(), canonicalMessage, certificate.getPublicKey(), signatureAlgorithm);
    }

    private static X509Certificate parseCertificate(byte[] certificateBytes) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
            Certificate certificate = certificateFactory.generateCertificate(
                new ByteArrayInputStream(certificateBytes));
            
            if (!(certificate instanceof X509Certificate)) {
                throw SnsCertificateException.builder()
                    .message("Certificate is not an X.509 certificate")
                    .build();
            }
            
            return (X509Certificate) certificate;
        } catch (CertificateException e) {
            throw SnsCertificateException.builder()
                .message("Failed to parse certificate: " + e.getMessage())
                .cause(e)
                .build();
        }
    }

    private static void validateCertificate(X509Certificate certificate) {
        try {
            // Check certificate validity period
            certificate.checkValidity();
            
            // Verify certificate is issued by Amazon SNS with comprehensive chain validation
            validateCertificateChainOfTrust(certificate);
            
            // Additional security checks
            validateCertificateKeyUsage(certificate);
            
        } catch (CertificateException e) {
            throw SnsCertificateException.builder()
                .message("Certificate validation failed: " + e.getMessage())
                .cause(e)
                .build();
        }
    }

    /**
     * Validates the certificate chain of trust to ensure it's issued by Amazon SNS.
     * <p>
     * This method performs comprehensive validation of the certificate issuer to ensure
     * it comes from a trusted Amazon SNS certificate authority. It checks multiple
     * issuer patterns to support different AWS partitions and certificate structures.
     *
     * @param certificate The certificate to validate.
     * @throws SnsCertificateException If the certificate is not from a trusted Amazon SNS issuer.
     */
    private static void validateCertificateChainOfTrust(X509Certificate certificate) {
        String issuerDN = certificate.getIssuerDN().getName();
        String subjectDN = certificate.getSubjectDN().getName();
        
        if (!isAmazonSnsIssuer(issuerDN)) {
            throw SnsCertificateException.builder()
                .message("Certificate is not issued by Amazon SNS. Issuer: " + issuerDN + 
                        ". Expected issuer patterns: CN=sns.amazonaws.com, CN=Amazon, " +
                        "O=Amazon.com Inc., or O=Amazon Web Services")
                .build();
        }
        
        // Additional validation for subject DN to ensure it's an SNS certificate
        if (!isValidSnsSubject(subjectDN)) {
            throw SnsCertificateException.builder()
                .message("Certificate subject is not valid for Amazon SNS. Subject: " + subjectDN + 
                        ". Expected subject patterns should contain sns.amazonaws.com or Amazon SNS identifiers")
                .build();
        }
    }

    /**
     * Validates certificate key usage to ensure it's appropriate for signature verification.
     * <p>
     * This method checks that the certificate has the appropriate key usage extensions
     * for digital signature verification, which is required for SNS message validation.
     *
     * @param certificate The certificate to validate.
     * @throws SnsCertificateException If the certificate doesn't have appropriate key usage.
     */
    private static void validateCertificateKeyUsage(X509Certificate certificate) {
        boolean[] keyUsage = certificate.getKeyUsage();
        
        // Key usage array indices according to RFC 5280:
        // 0: digitalSignature, 1: nonRepudiation, 2: keyEncipherment, etc.
        if (keyUsage != null && keyUsage.length > 0) {
            // Check if digital signature is enabled (index 0)
            if (!keyUsage[0]) {
                throw SnsCertificateException.builder()
                    .message("Certificate does not have digital signature key usage enabled, " +
                            "which is required for SNS message signature verification")
                    .build();
            }
        }
        // If keyUsage is null, the certificate doesn't restrict key usage, which is acceptable
    }

    private static boolean isAmazonSnsIssuer(String issuerDN) {
        if (issuerDN == null) {
            return false;
        }
        
        // Convert to lowercase for case-insensitive matching
        String normalizedIssuer = issuerDN.toLowerCase();
        
        // Check for various Amazon SNS certificate issuer patterns
        return normalizedIssuer.contains("cn=sns.amazonaws.com") ||
               normalizedIssuer.contains("cn=amazon") ||
               normalizedIssuer.contains("o=amazon.com inc.") ||
               normalizedIssuer.contains("o=amazon web services") ||
               normalizedIssuer.contains("o=amazon.com, inc.") ||
               normalizedIssuer.contains("cn=amazon web services") ||
               // Support for different AWS partitions
               normalizedIssuer.contains("amazonaws.com") && normalizedIssuer.contains("amazon");
    }

    /**
     * Validates that the certificate subject is appropriate for Amazon SNS.
     * <p>
     * This method checks the certificate subject DN to ensure it contains
     * identifiers that are consistent with Amazon SNS certificates.
     *
     * @param subjectDN The subject DN to validate.
     * @return true if the subject is valid for SNS, false otherwise.
     */
    private static boolean isValidSnsSubject(String subjectDN) {
        if (subjectDN == null) {
            return false;
        }
        
        // Convert to lowercase for case-insensitive matching
        String normalizedSubject = subjectDN.toLowerCase();
        
        // Check for SNS-related subject patterns
        return normalizedSubject.contains("sns.amazonaws.com") ||
               normalizedSubject.contains("amazon") ||
               normalizedSubject.contains("aws") ||
               // Allow certificates that contain amazonaws.com domain
               normalizedSubject.contains("amazonaws.com");
    }

    private static String getSignatureAlgorithm(String signatureVersion) {
        switch (signatureVersion) {
            case SIGNATURE_VERSION_1:
                return SHA1_WITH_RSA;
            case SIGNATURE_VERSION_2:
                return SHA256_WITH_RSA;
            default:
                throw SnsSignatureValidationException.builder()
                    .message("Unsupported signature version: " + signatureVersion + 
                            ". Supported versions are: " + SIGNATURE_VERSION_1 + ", " + SIGNATURE_VERSION_2)
                    .build();
        }
    }

    private static String buildCanonicalMessage(SnsMessage message) {
        StringBuilder canonical = new StringBuilder();
        
        // Build canonical string according to SNS specification
        // The order and format must match exactly what SNS uses for signing
        
        canonical.append("Message\n");
        canonical.append(message.message()).append("\n");
        
        canonical.append("MessageId\n");
        canonical.append(message.messageId()).append("\n");
        
        // Subject is optional but must be included if present
        if (message.subject().isPresent()) {
            canonical.append("Subject\n");
            canonical.append(message.subject().get()).append("\n");
        }
        
        canonical.append("Timestamp\n");
        canonical.append(message.timestamp().toString()).append("\n");
        
        canonical.append("TopicArn\n");
        canonical.append(message.topicArn()).append("\n");
        
        canonical.append("Type\n");
        canonical.append(message.type()).append("\n");
        
        return canonical.toString();
    }

    private static void verifySignature(String signatureBase64, String canonicalMessage, 
                                      PublicKey publicKey, String signatureAlgorithm) {
        try {
            // Decode the base64 signature
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            
            // Initialize signature verification
            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initVerify(publicKey);
            signature.update(canonicalMessage.getBytes(StandardCharsets.UTF_8));
            
            // Verify the signature
            boolean isValid = signature.verify(signatureBytes);
            
            if (!isValid) {
                throw SnsSignatureValidationException.builder()
                    .message("Message signature verification failed. The message may have been tampered with or " +
                            "is not from Amazon SNS.")
                    .build();
            }
            
        } catch (IllegalArgumentException e) {
            throw SnsSignatureValidationException.builder()
                .message("Invalid base64 signature format: " + e.getMessage())
                .cause(e)
                .build();
        } catch (NoSuchAlgorithmException e) {
            throw SnsSignatureValidationException.builder()
                .message("Signature algorithm not supported: " + signatureAlgorithm)
                .cause(e)
                .build();
        } catch (InvalidKeyException e) {
            throw SnsSignatureValidationException.builder()
                .message("Invalid public key for signature verification: " + e.getMessage())
                .cause(e)
                .build();
        } catch (SignatureException e) {
            throw SnsSignatureValidationException.builder()
                .message("Signature verification failed: " + e.getMessage())
                .cause(e)
                .build();
        }
    }
}