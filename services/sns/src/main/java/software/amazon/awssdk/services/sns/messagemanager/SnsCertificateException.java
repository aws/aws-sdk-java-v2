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

package software.amazon.awssdk.services.sns.messagemanager;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Exception thrown when certificate retrieval or validation fails during SNS message verification.
 * <p>
 * This exception is thrown when there are issues with the certificates used to verify SNS message signatures.
 * Certificate validation is a critical security step that ensures messages are genuinely from Amazon SNS.
 * <p>
 * Common scenarios that trigger this exception:
 * <ul>
 *   <li>Certificate URL is not from a trusted SNS-signed domain</li>
 *   <li>Certificate retrieval fails (network issues, invalid URL, etc.)</li>
 *   <li>Certificate chain of trust validation fails</li>
 *   <li>Certificate is not issued by Amazon SNS</li>
 *   <li>Certificate has expired or is not yet valid</li>
 *   <li>Certificate format is invalid or corrupted</li>
 * </ul>
 * <p>
 * When this exception is thrown, the message should be considered untrusted and should not be processed,
 * as the certificate validation is essential for ensuring message authenticity.
 */
@SdkPublicApi
public class SnsCertificateException extends SnsMessageValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SnsCertificateException with the specified detail message.
     *
     * @param message The detail message explaining the certificate validation failure.
     */
    public SnsCertificateException(String message) {
        super(message);
    }

    /**
     * Constructs a new SnsCertificateException with the specified detail message and cause.
     *
     * @param message The detail message explaining the certificate validation failure.
     * @param cause   The underlying cause of the certificate validation failure.
     */
    public SnsCertificateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new builder for constructing SnsCertificateException instances.
     *
     * @return A new builder instance.
     */
    public static SnsMessageValidationException.Builder builder() {
        return new SnsMessageValidationException.Builder() {
            @Override
            public SnsMessageValidationException build() {
                if (cause != null) {
                    return new SnsCertificateException(message, cause);
                }
                return new SnsCertificateException(message);
            }
        };
    }
}