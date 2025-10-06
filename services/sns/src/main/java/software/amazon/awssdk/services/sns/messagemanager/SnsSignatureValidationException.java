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
 * Exception thrown when SNS message signature verification fails.
 * <p>
 * This exception is thrown when the cryptographic signature of an SNS message cannot be verified,
 * indicating that the message may not be authentic or may have been tampered with during transmission.
 * <p>
 * Common scenarios that trigger this exception:
 * <ul>
 *   <li>Invalid or corrupted message signature</li>
 *   <li>Message content has been modified after signing</li>
 *   <li>Signature verification algorithm failure</li>
 *   <li>Mismatch between signature version and verification method</li>
 *   <li>Certificate and signature incompatibility</li>
 * </ul>
 * <p>
 * When this exception is thrown, the message should be considered untrusted and should not be processed.
 */
@SdkPublicApi
public class SnsSignatureValidationException extends SnsMessageValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SnsSignatureValidationException with the specified detail message.
     *
     * @param message The detail message explaining the signature validation failure.
     */
    public SnsSignatureValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SnsSignatureValidationException with the specified detail message and cause.
     *
     * @param message The detail message explaining the signature validation failure.
     * @param cause   The underlying cause of the signature validation failure.
     */
    public SnsSignatureValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new builder for constructing SnsSignatureValidationException instances.
     *
     * @return A new builder instance.
     */
    public static SnsMessageValidationException.Builder builder() {
        return new SnsMessageValidationException.Builder() {
            @Override
            public SnsMessageValidationException build() {
                if (cause != null) {
                    return new SnsSignatureValidationException(message, cause);
                }
                return new SnsSignatureValidationException(message);
            }
        };
    }
}